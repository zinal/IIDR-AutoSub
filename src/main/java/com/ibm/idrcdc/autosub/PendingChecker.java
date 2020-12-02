/*
** ****************************************************************************
** (c) Copyright IBM Corp. 2019, 2020. All rights reserved.
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
import org.apache.commons.lang3.StringUtils;

/**
 * Algorithm to check subscriptions of a single source datastore
 * for a repairable failure due to DDL on the source table.
 * @author zinal
 */
public class PendingChecker {

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(PendingChecker.class);

    private final long startTime;
    private final AsGlobals globals;
    private final PerSource source;
    private final Script script;

    public PendingChecker(AsGlobals globals, PerSource source, Script script) {
        this.startTime = System.currentTimeMillis();
        this.globals = globals;
        this.source = source;
        this.script = script;
    }

    /**
     * Check if there are some subscriptions to be recovered.
     * Update the per-subscription flags (known/recover),
     * save the names of the altered tables in the Monitor objects.
     * @return true, if there are subscriptions for recovery, and false otherwise
     */
    public boolean check() {
        // Clean up all flags to their initial states
        clearSubFlags();
        // Grab the subscription states
        int countPending = 0;
        script.dataStore(source.getSource(), EngineType.Source);
        script.execute("monitor replication;");
        final ScriptOutput table = script.getTable();
        for (int irow = 0; irow < table.getRowCount(); ++irow) {
            if ( checkRow(table, irow) )
                ++ countPending;
        }
        // Signal lost & found subscriptions
        checkMissingSubs();
        // Return true, if we have subscriptions to be recovered.
        return (countPending > 0);
    }

    /**
     * Subscription status validator.
     * Parses the row from MONITOR REPLICATION output,
     * and calls additional checks for the failed subscriptions.
     * @param table MONITOR REPLICATION output
     * @param irow Row number
     * @return true, if the corresponding subscription should be recovered.
     */
    private boolean checkRow(ScriptOutput table, int irow) {
        String subname = table.getValueAt(irow, "SUBSCRIPTION");
        String substate = table.getValueAt(irow, "STATE");
        String target = table.getValueAt(irow, "TARGET DATASTORE");
        PerTarget pst = source.findTarget(target);
        if (pst==null)
            return false; // Skip the unknown targets
        Monitor m = pst.findMonitor(subname);
        if (m==null) // Skip the unknown subscriptions
            return false;
        // Mark the subscription as known
        m.setKnown(true);
        if ("Mirror Continuous".equalsIgnoreCase(substate)) {
            // Okay, so it works now.
            // Report the recovery, if it failed previously.
            if (m.isSuppressStopped()) {
                m.setSuppressStopped(false);
                LOG.info("Recovered subscription {}", m.getSubscription());
            }
            return false;
        }
        // Report the subscription as failed.
        if (!m.isSuppressStopped()) {
            m.setSuppressStopped(true);
            LOG.info("Subscription {} not working, actual state: {}",
                    m.getSubscription().getName(), substate);
        }
        // Check the actual failure reason
        if ("Failed".equalsIgnoreCase(substate)) {
            if ( checkMonitor(m) ) {
                // Mark the subscription for recovery.
                m.setRepair(true);
                return true;
            }
        }
        return false;
    }

    /**
     * Check the event log of a failed subscription for DDL.
     * @param m Monitor object for a subscription
     * @return true if the subscription should be recovered, false otherwise.
     */
    private boolean checkMonitor(Monitor m) {
        if (m.getFailureTime() != 0L) {
            final long diff = startTime - m.getFailureTime();
            if (diff < globals.getPauseAfterError())
                return false;
        }
        // Supported event sequence: 9505, 1463.
        // Table name is extracted from message 9505.
        script.execute("list subscription events name \"{0}\" type source;",
                m.getSubscription().getName());
        final ScriptOutput table = script.getTable();
        String msg9505 = null;
        for (int irow=0; irow < table.getRowCount(); ++irow) {
            String eventId = table.getValueAt(irow, "EVENT ID");
            if ("9505".equals(eventId)) {
                // DDL detected, target cannot handle it
                script.execute("show subscription event details row {0} ;",
                        String.valueOf(irow+1));
                msg9505 = script.getTable().getValueAt(1, 1);
            } else if ("1463".equals(eventId)) {
                // Subscription start, no need to scan further.
                break;
            }
        }
        // Retrieve the altered table name from the messages.
        String tableName = null;
        if (msg9505 != null) {
            // Parse the 9505 message
            // IBM XXX has encountered a critical data definition (DDL) change for source table
            // METADEMO.TAB0 and will shutdown. Please re-add the table definition ...
            msg9505 = msg9505.replace('\n', ' ').replace('\r', ' ');
            final String textBegin = "(DDL) change for source table ";
            int tabBegin = msg9505.indexOf(textBegin);
            int tabEnd = msg9505.indexOf(" and will shutdown. Please re-add ");
            if (tabBegin < 0 || tabEnd < 0 || tabBegin >= tabEnd) {
                LOG.warn("Failed to parse the 9505 message text:\n\t{}", msg9505);
            } else {
                tabBegin += textBegin.length();
                tableName = msg9505.substring(tabBegin, tabEnd);
            }
        }
        if (StringUtils.isBlank(tableName)) {
            // Not a case we support.
            if (!m.isSuppressNoRepair()) {
                m.setSuppressNoRepair(true);
                LOG.warn("Cannot repair the failed subscription {}", m.getSubscription());
            }
            return false;
        }
        // Seems to be a supported case.
        if (m.isSuppressNoRepair()) {
            m.setSuppressNoRepair(false);
        }
        m.getSourceTables().add(tableName);
        LOG.info("Probably able to repair the failed subscription {}\n\t{}",
                m.getSubscription(), m.getSourceTables());
        return true;
    }

    /**
     * Remove all known/repair flags and list of tables for all monitors.
     */
    private void clearSubFlags() {
        for (PerTarget pst : source.getTargets()) {
            for (Monitor m : pst.getMonitors()) {
                m.clearSubFlags();
            }
        }
    }

    /**
     * Detect lost & found subscriptions - for logging only.
     */
    private void checkMissingSubs() {
        for (PerTarget pst : source.getTargets()) {
            for (Monitor m : pst.getMonitors()) {
                checkMissingSub(m);
            }
        }
    }

    private void checkMissingSub(Monitor m) {
        if (m.isKnown()) {
            if (m.isSuppressMissing()) {
                m.setSuppressMissing(false);
                LOG.info("Found subscription {}", m.getSubscription());
            }
        } else {
            if (! m.isSuppressMissing()) {
                m.setSuppressMissing(true);
                LOG.warn("Lost subscription {}", m.getSubscription());
            }
        }
    }

}
