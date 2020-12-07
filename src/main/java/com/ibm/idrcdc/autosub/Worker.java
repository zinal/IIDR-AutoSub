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
import java.util.ArrayList;
import java.util.List;

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
        LOG.info("autosub version {} Worker", VERSION);
        try {
            final AsGlobals globals = AsGlobals.fromArgs(args);
            LOG.info("Working data file is {}", globals.getDataFile());
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
            System.exit(0);
        } catch(Exception ex) {
            LOG.error("Service execution failed", ex);
            System.exit(1);
        }
    }

    /**
     * Working cycle with the fixed configuration
     * (e.g. set of monitored subscriptions).
     */
    @Override
    public void run() {
        // Enter the monitoring cycle.
        while (true) {
            long tvStart = System.currentTimeMillis();
            // If shutdown is requested, exit to the main loop, which will exit too.
            if (flagShutdown.isEnabled())
                break;
            // If configuration reload is requested, just exit to the main loop,
            // which will re-create the Worker object and re-run this method.
            if (flagReload.isEnabled()) {
                flagReload.disable();
                LOG.info("Re-loading configuration...");
                break;
            }
            // Validate the configuration, if not yet done.
            if (validate()) {
                // Find the subscriptions to repair
                List<PerSource> pending = checkPending();
                if (pending!=null && !pending.isEmpty()) {
                    LOG.debug("Pending recovery for datastores {}...", pending);
                    if ( pauseBeforeRepair() )
                        continue; // May get a shutdown flag
                    // Re-check after a small delay
                    ChangeSignature sig1 = new ChangeSignature(pending);
                    pending = checkPending();
                    ChangeSignature sig2 = new ChangeSignature(pending);
                    if (! sig1.equals(sig2)) {
                        LOG.debug("... changed by other means, going back to monitoring.");
                        continue;
                    }
                    LOG.debug("... second check shows {}, starting repairs.", pending);
                    // Repair everything
                    for (PerSource ps : pending) {
                        LOG.info("Repairs for source datastore {}...", ps);
                        repair(ps);
                    }
                    LOG.info("Repairs complete.");
                    // Update start time for longer delay
                    tvStart = System.currentTimeMillis();
                }
            }
            pauseBetweenChecks(tvStart);
        } // while (true)
    }

    private boolean waitUntil(final long tvFinish) {
        while (true) {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException ix) {}
            if (flagShutdown.isEnabled())
                return true; // Shutdown fast
            if (tvFinish <= System.currentTimeMillis())
                return false; // End of delay
        }
    }

    private boolean pauseBeforeRepair() {
        return waitUntil(System.currentTimeMillis() + globals.getPauseBeforeRepair());
    }

    private boolean pauseBetweenChecks(long tvStart) {
        return waitUntil(tvStart + globals.getMainPeriod());
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
                    if ( new PendingChecker(globals, ps, script).check(true) ) {
                        if (pending==null)
                            pending = new ArrayList<>();
                        pending.add(ps);
                    }
                }
            } catch(Exception ex) {
                LOG.error("Error checking state for subscriptions in "
                        + "source datastore {}", ps.getSource().getName(), ex);
            }
        }
        return pending;
    }

    /**
     * Run the repairman on the specified source datastore.
     * @param ps Per-source collection of monitors
     */
    private void repair(PerSource ps) {
        try (Script script = openScript()) {
            if (script != null)
                new Repairman(globals, ps, script) . run();
        } catch(Exception ex) {
            LOG.error("Repair sequence failed for datastore {}",
                    ps.getSource().getName(), ex);
        }
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
