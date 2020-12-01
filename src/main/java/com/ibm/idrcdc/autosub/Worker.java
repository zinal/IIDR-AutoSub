/*
** ****************************************************************************
** (c) Copyright IBM Corp. 2020 All rights reserved.
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
package com.ibm.idrcdc.autosub;

import com.ibm.idrcdc.autosub.model.*;
import com.ibm.replication.cdc.scripting.EmbeddedScriptException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Application entry point and main working cycle.
 * @author zinal
 */
public class Worker implements Runnable {

    private static final org.slf4j.Logger LOG
            = org.slf4j.LoggerFactory.getLogger(Worker.class);

    public static final String VERSION = "2.0-dev";

    private final AsGlobals globals;
    private final AsConfig config;
    private final AsGroups groups;
    private final FileFlag flagShutdown;
    private final FileFlag flagReload;

    private boolean configValidated = false;
    private boolean suppressConfigCheckMessage = false;
    private boolean suppressConnectErrorMessage = false;

    public Worker(AsGlobals globals, FileFlag flagShutdown) {
        this.globals = globals;
        this.config = AsConfig.load(globals);
        this.groups = new AsGroups(config);
        this.flagShutdown = flagShutdown;
        this.flagReload = FileFlag.newReload(globals.getDataFile());
    }

    /**
     * Entry point
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        LOG.info("autosub Worker version {}", VERSION);
        try {
            final AsGlobals globals = makeGlobals(
                    args.length == 0 ? "cdc-autosub.properties" : args[0] );
            final FileFlag flagShutdown = FileFlag.newShutdown(globals.getDataFile());
            flagShutdown.disable();
            // Main working cycle
            while (true) {
                // Check for shutdown request
                if (flagShutdown.isEnabled())
                    break;
                new Worker(globals, flagShutdown) . run();
            }
            flagShutdown.disable();
            LOG.info("Service shutting down...");
        } catch(Exception ex) {
            LOG.error("Service execution failed", ex);
            System.exit(1);
        }
    }

    private static AsGlobals makeGlobals(String configFile) throws Exception {
        final Properties props = new Properties();
        try (InputStream is
                = new FileInputStream(configFile)) {
            props.load(is);
        }
        return new AsGlobals(props);
    }

    /**
     * Working cycle with the fixed configuration
     * (e.g. set of monitored subscriptions).
     */
    @Override
    public void run() {
        // Enter the monitoring cycle.
        while (true) {
            // If configuration reload is requested, just exit to the main loop,
            // which will re-create the Worker object and re-run this method.
            if (flagReload.isEnabled()) {
                flagReload.disable();
                break;
            }
            // If shutdown is requested, exit to the main loop, which will exit too.
            if (flagShutdown.isEnabled())
                break;
            // Validate the configuration, if not yet done.
            if (validate()) {
                // Find the subscriptions to repair
                List<PerSource> pending = checkPending();
                if (pending!=null) {
                    // Repair everything
                    for (PerSource ps : pending)
                        repair(ps);
                }
            }
        } // while (true)
    }

    /**
     * Validate the configured subscriptions for name validity.
     * After the first successful validation always returns true
     * without any re-validation attempts.
     * @return  true if validation succeeds, false otherwise
     */
    private boolean validate() {
        if ( configValidated )
            return true;
        try (Script script = openScript()) {
            if (script == null) {
                if (!suppressConfigCheckMessage) {
                    LOG.warn("Configuration validation skipped due to missing "
                            + "Access Server connection.");
                    suppressConfigCheckMessage = true;
                }
                return false;
            }
            suppressConfigCheckMessage = false;
            new ConfigValidator(groups, script) . run();
            configValidated = true;
            return true;
        }
    }

    /**
     * Identify any fixable failed subscriptions
     * @return List of source datastores containing fixable subscriptions
     */
    private List<PerSource> checkPending() {
        List<PerSource> pending = null;
        for (PerSource ps : groups.getData()) {
            if (ps.isDisabled())
                continue; // skip groups without valid monitors
            // Connect to access server for each source datastore
            try (Script script = openScript()) {
                if (script!=null) {
                    // Validate monitors in each group
                    if ( new PendingChecker(ps, script).check() ) {
                        if (pending==null)
                            pending = new ArrayList<>();
                        pending.add(ps);
                    }
                }
            } catch(Exception ex) {
                LOG.error("Error checking state for subscriptions in "
                        + "source datastore {}", ps.getSource(), ex);
            }
        }
        return pending;
    }

    private void repair(PerSource ps) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Create the embedded scriptiong object with the Access Server connection.
     * @return Script object, or null if connection has failed.
     */
    public Script openScript() {
        try {
            final Script retval = new Script(globals);
            if (suppressConnectErrorMessage) {
                suppressConnectErrorMessage = false;
                LOG.info("Re-established connection to the Access Server");
            }
            return retval;
        } catch(Exception ex) {
            if (!suppressConnectErrorMessage) {
                suppressConnectErrorMessage = true;
                final String TEXT = "Failed to connect to the Access Server";
                if (ex instanceof EmbeddedScriptException) {
                    LOG.warn("{}.\n{}", TEXT,
                            ((EmbeddedScriptException)ex).getResultCodeAndMessage(),
                            ex);
                } else {
                    LOG.warn("{}.", TEXT, ex);
                }
            }
            return null;
        }
    }

}
