/*
** ****************************************************************************
** (c) Copyright IBM Corp. 2019 All rights reserved.
** 
** The following sample of source code ("Sample") is owned by International 
** Business Machines Corporation or one of its subsidiaries ("IBM") and is 
** copyrighted and licensed, not sold. You may use, copy, modify, and 
** distribute the Sample in any form without payment to IBM.
** 
** The Sample code is provided to you on an "AS IS" basis, without warranty of 
** any kind. IBM HEREBY EXPRESSLY DISCLAIMS ALL WARRANTIES, EITHER EXPRESS OR 
** IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
** MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. Some jurisdictions do 
** not allow for the exclusion or limitation of implied warranties, so the above 
** limitations or exclusions may not apply to you. IBM shall not be liable for 
** any damages you suffer as a result of using, copying, modifying or 
** distributing the Sample, even if IBM has been advised of the possibility of 
** such damages.
**
** Author:   Maksim Zinal <mzinal@ru.ibm.com>
 */
package ru.zinal.idrcdc.autosub;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import org.jdom2.input.SAXBuilder;
import ru.zinal.idrcdc.autosub.algo.*;
import ru.zinal.idrcdc.autosub.config.*;

/**
 * Application entry point and main working cycle.
 * @author zinal
 */
public class Worker {
    
    private static final org.slf4j.Logger LOG
            = org.slf4j.LoggerFactory.getLogger(Worker.class);
    
    private final Config config;
    private final Globals globals;
    
    private List<PerSource> groups;
    
    private boolean monitorsValidated = false;
    private boolean suppressConfigCheckMessage = false;
    
    public Worker(Properties props) {
        this.config = new Config(props);
        this.globals = new Globals(config);
        LOG.info("Configuration loaded.");
    }
    
    /**
     * Initialization
     * @throws Exception 
     */
    public void init() throws Exception {
        globals.setMonitors( createMonitors(globals.getConfig()) );
        groups = createMonitorGroups(globals.getMonitors());
        LOG.info("Monitors prepared, total {} monitor(s).", 
                globals.getMonitors().size());
    }
    
    /**
     * Main working cycle
     */
    public void service() {
        // Pre-check configuration names
        validate();
        // Main working cycle
        while (true) {
            try { // Configured sleep delay
                Thread.sleep(config.getMainSleep());
            } catch(InterruptedException ix) {}
            // In case the pre-check did not complete, repeat it
            if (!validate())
                // We move forward only after validation succeeds
                continue;
            // Find the subscriptions to repair
            List<PerSource> pending = checkPending();
            if (pending==null || pending.isEmpty())
                continue;
            LOG.info("Waiting for {} seconds before re-check and repair",
                    config.getPauseAfterError() / 1000L);
            try { // Sleep to settle all DDLs
                Thread.sleep(config.getPauseAfterError());
            } catch(InterruptedException ix) {}
            // Repair everything
            for (PerSource g : pending)
                repair(g);
        } // while (true)
    }
    
    /**
     * Identify any fixable failed subscriptions
     * @return List of source datastores containing fixable subscriptions
     */
    private List<PerSource> checkPending() {
        List<PerSource> pending = null;
        for (PerSource g : groups) {
            if (g.isDisabled())
                continue; // skip gtoups without valid monitors
            // Connect to access server for each source datastore
            try (Script script = globals.openScript()) {
                if (script!=null) {
                    // Validate monitors in each group
                    if ( new PendingChecker(g, script).check() ) {
                        if (pending==null)
                            pending = new ArrayList<>();
                        pending.add(g);
                    }
                }
            } catch(Exception ex) {
                LOG.error("Error checking state for subscriptions in "
                        + "source datastore {}", g.getSource(), ex);
            }
        }
        return pending;
    }

    /**
     * Call the repairman to fix the subscriptions
     * @param g Group of subscriptions for a single source datastore
     */
    private void repair(PerSource g) {
        try (Script script = globals.openScript()) {
            if (script==null) {
                LOG.error("No connection to Access Server, cannot repair "
                        + "subscription(s) in source datastore {}",
                        g.getSource());
                return;
            }
            // Repeat the check - the situation may have changed
            if ( new PendingChecker(g, script).check() ) {
                // Run repairs
                new Repairman(globals.getConfig(), g, script).run();
            } else {
                LOG.info("Desided NOT to repair subscriptions in "
                        + "source datastore {}", g.getSource());
            }
        } catch(Exception ex) {
            LOG.error("Error while reparing subscriptions in "
                    + "source datastore {}", g.getSource(), ex);
        }
    }

    /**
     * Validate the configured subscriptions for name validity.
     * After the first successful validation always returns true
     * without any re-validation attempts.
     * @return  true if validation succeeds, false otherwise
     */
    private boolean validate() {
        if ( monitorsValidated )
            return true;
        try (Script script = globals.openScript()) {
            if (script == null) {
                if (!suppressConfigCheckMessage) {
                    LOG.warn("Configuration validation skipped due to missing "
                            + "Access Server connection.");
                    suppressConfigCheckMessage = true;
                }
                return false;
            }
            suppressConfigCheckMessage = false;
            new Validator(globals, script).run();
            monitorsValidated = true;
            return true;
        }
    }
    
    /**
     * Read monitor settings
     * @param config Configuration 
     * @return List of configured per-subscription monitors
     * @throws Exception 
     */
    private static List<Monitor> createMonitors(Config config)
            throws Exception {
        final File dir = new File(config.getSubscriptionDirectory());
        final List<Monitor> monitors = new ArrayList<>();
        final SAXBuilder docBuilder = new SAXBuilder();
        final File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile()
                        && pathname.canRead()
                        && pathname.getName().endsWith(".sub.xml");
            }
        });
        if (files==null) {
            throw new Exception("Subscription sub-directory "
                    + "[" + dir.getAbsolutePath() + "] does not exist");
        }
        for (File f : files) {
            final org.jdom2.Element options 
                    = docBuilder.build(f).detachRootElement();
            monitors.add(new Monitor(options));
        }
        if (monitors.isEmpty()) {
            throw new Exception("Missing subscription configuration files");
        }
        return monitors;
    }

    /**
     * Group monitors by source and target datastore
     * @param monitors List of monitors
     * @return Grouped monitors
     */
    private static List<PerSource> createMonitorGroups(List<Monitor> monitors) {
        final Map<String, Map<String, List<Monitor>>> map1 = new TreeMap<>();
        for (Monitor m : monitors) {
            Map<String, List<Monitor>> map2 = map1.get(m.getSource());
            if (map2==null) {
                map2 = new TreeMap<>();
                map1.put(m.getSource(), map2);
            }
            List<Monitor> vals = map2.get(m.getTarget());
            if (vals==null) {
                vals = new ArrayList<>();
                map2.put(m.getTarget(), vals);
            }
            vals.add(m);
        }
        final List<PerSource> retval = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<Monitor>>> me1 
                : map1.entrySet()) {
            PerSource ps = new PerSource(me1.getKey());
            for (Map.Entry<String, List<Monitor>> me2 
                    : me1.getValue().entrySet()) {
                PerSourceTarget pst = new PerSourceTarget(me2.getKey());
                pst.getMonitors().addAll(me2.getValue());
                ps.getTargets().add(pst);
            }
            retval.add(ps);
        }
        return retval;
    }

    /**
     * Build and configure the application object
     * @return Application object
     * @throws Exception 
     */
    private static Worker makeWorker() throws Exception {
        final Properties props = new Properties();
        try (InputStream is 
                = new FileInputStream("cdc-autosub.properties")) {
            props.load(is);
        }
        return new Worker(props);
    }

    /**
     * Entry point
     * @param args Command line arguments 
     */
    public static void main(String[] args) {
        LOG.info("autosub Worker v1.0");
        try {
            final Worker worker = makeWorker();
            worker.init();
            worker.service();
        } catch(Exception ex) {
            LOG.error("Service execution failed", ex);
            System.exit(1);
        }
    }

}
