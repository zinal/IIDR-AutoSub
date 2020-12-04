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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final PerSource origin;
    private final Script script;

    public PendingChecker(AsGlobals globals, PerSource origin, Script script) {
        this.startTime = System.currentTimeMillis();
        this.globals = globals;
        this.origin = origin;
        this.script = script;
    }

    /**
     * Check if there are some subscriptions to be recovered.
     * Update the per-subscription flags (known/recover),
     * save the names of the altered tables in the Monitor objects.
     *
     * @param thorough true, if depencency checks are to be done.
     * @return true, if there are subscriptions for recovery, and false otherwise
     */
    public boolean check(boolean thorough) {
        // Clean up all flags to their initial states
        clearSubFlags();
        // Grab the subscription states
        script.dataStore(origin.getSource(), EngineType.Source);
        script.execute("monitor replication;");
        final ScriptOutput table = script.getTable();
        for (int irow = 0; irow < table.getRowCount(); ++irow) {
            checkRow(table, irow);
        }
        // Signal lost & found subscriptions
        checkMissingSubs();
        // Select altered tables based on dependencies
        if (thorough)
           selectTables();
        // Return true, if we have subscriptions to be recovered.
        return ! origin.pendingMonitors().isEmpty();
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
        PerTarget pst = origin.findTarget(target);
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
            m.reportSubscriptionRecovered();
            return false;
        }
        // Report the subscription as failed.
        m.reportSubscriptionFailed(substate);
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
        // Supported event sequence: {9505 or 9602}, 1463.
        // Table name is extracted from message.
        script.execute("list subscription events name \"{0}\" type source;",
                m.getSubscription().getName());
        final ScriptOutput table = script.getTable();
        FailureMessageType messageType = null;
        String messageText = null;
        for (int irow=0; irow < table.getRowCount(); ++irow) {
            String eventId = table.getValueAt(irow, "EVENT ID");
            if ("1463".equals(eventId)) {
                // Subscription start, no need to scan further.
                break;
            }
            for (FailureMessageType mt : FailureMessageType.values()) {
                if (mt.id.equals(eventId)) {
                    messageType = mt;
                    script.execute("show subscription event details row {0} ;",
                            String.valueOf(irow+1));
                    messageText = script.getTable().getValueAt(1, 1);
                    break;
                }
            }
            if (messageType != null)
                break; // found the message with table name
        }
        // Retrieve the altered table name from the messages.
        String tableName = null;
        if (messageType != null) {
            switch (messageType) {
                case M9505: {
// Oracle:
// IBM XXX has encountered a critical data definition (DDL) change for source table
// METADEMO.TAB0 and will shutdown. Please re-add the table definition ...
                    String msg9505 = messageToLine(messageText);
                    final String textBegin = "(DDL) change for source table ";
                    int tabBegin = msg9505.indexOf(textBegin);
                    int tabEnd = msg9505.indexOf(" and will shutdown. Please re-add ");
                    if (tabBegin < 0 || tabEnd < 0 || tabBegin >= tabEnd) {
                        LOG.warn("Failed to parse the 9505 message text:\n\t{}", msg9505);
                    } else {
                        tabBegin += textBegin.length();
                        tableName = msg9505.substring(tabBegin, tabEnd);
                    }
                    break;
                }
                case M9602: {
// PostgreSQL:
// An exception has occurred during mirroring.
// Stopping replication because table definition has changed for table "myuser.pgtab1".
                    String msg9602 = messageToLine(messageText).trim();
                    final String textBegin = "definition has changed for table ";
                    int tabBegin = msg9602.indexOf(textBegin);
                    int tabEnd = msg9602.length() - 1;
                    if (!msg9602.endsWith("\"."))
                        tabEnd = -1;
                    if (tabBegin < 0 || tabEnd < 0 || tabBegin >= tabEnd) {
                        LOG.warn("Failed to parse the 9602 message text:\n\t{}", msg9602);
                    } else {
                        tabBegin += textBegin.length();
                        tableName = msg9602.substring(tabBegin, tabEnd);
                        tableName = tableName.replace("\"", "");
                    }
                    break;
                }
            }
        }
        if (StringUtils.isBlank(tableName)) {
            // Not a case we support.
            m.reportCannotRepair();
            return false;
        }
        // Seems to be a supported case.
        m.resetCannotRepair();
        m.getAlteredTables().add(tableName);
        LOG.debug("Probably able to repair the failed subscription {}, tables {}",
                m.getSubscription(), m.getAlteredTables());
        return true;
    }

    private static String messageToLine(String msg) {
        while (msg.contains("\n\r"))
            msg = msg.replace("\n\r", " ");
        return msg.replace('\n', ' ').replace('\r', ' ');
    }

    /**
     * Identify which tables can be re-added safely.
     * @return List of tables to be re-added.
     */
    private void selectTables() {
        List<Monitor> pendingMonitors = origin.pendingMonitors();
        if (pendingMonitors.isEmpty())
            return; // nothing to do
        // Grab the names of all the replicated tables in all subscriptions
        final List<Monitor> monitors = origin.allMonitors();
        for (Monitor m : monitors) {
            m.getSourceTables().clear();
            script.dataStore(m.getTarget().getName(), EngineType.Target);
            script.execute("select subscription name \"{0}\";",
                    m.getSubscription().getName());
            script.execute("list table mappings;");
            ScriptOutput mappings = script.getTable();
            for ( int irow = 0; irow < mappings.getRowCount(); ++irow ) {
                String tableName = mappings.getValueAt(irow, "SOURCE TABLE");
                m.getSourceTables().add(tableName);
            }
        }
        // Collect the names of all altered tables we've detected.
        final Map<String, List<Monitor>> candidates = new HashMap<>();
        for (Monitor m : pendingMonitors) {
            for (String tabName : m.getAlteredTables()) {
                List<Monitor> x = candidates.get(tabName);
                if (x==null) {
                    x = new ArrayList<>();
                    candidates.put(tabName, x);
                }
                x.add(m);
            }
        }

        LOG.debug("selectTables: candidates {}", candidates);

        final Set<String> selectedTables = new HashSet<>();

        // We have a list of altered tables,
        // plus the list of all replicated tables per subscription.
        // To re-add the table, all the subscriptions with it should stop on it.
        for (Map.Entry<String, List<Monitor>> me : candidates.entrySet()) {
            boolean allow = true;
            for (Monitor m : monitors) {
                if (m.getAlteredTables().contains(me.getKey()))
                    continue; // Sub stopped at this table
                if (m.getSourceTables().contains(me.getKey())) {
                    // Sub replicates this table, and is not stopped on it.
                    // Safe recovery is not possible.
                    allow = false;
                    // Disable the repairs for the affected subscriptions, and
                    // print the warning messages.
                    for (Monitor lockedMon : me.getValue()) {
                        lockedMon.setRepair(false);
                        lockedMon.reportRecoveryLocked(me.getKey(), m);
                    }
                }
            }
            if (allow) {
                selectedTables.add(me.getKey());
                for (Monitor m : me.getValue())
                    m.resetRecoveryLocked();
            }
        }

        if (! selectedTables.isEmpty()) {
            LOG.info("Selected table(s) {} in subscription(s) {} for recovery",
                    selectedTables, origin.pendingMonitors());
        }

        // Leave only the selected tables as altered in all pending monitors.
        // If the list of altered tables becomes empty, exclude the monitor from repairs.
        for (Monitor m : origin.pendingMonitors()) {
            m.filterAlteredTables(selectedTables);
            if (m.getAlteredTables().isEmpty()) {
                LOG.debug("Excluded all altered tables for sub {}", m);
                m.setRepair(false);
            }
        }
    }

    /**
     * Remove all known/repair flags and list of tables for all monitors.
     */
    private void clearSubFlags() {
        for (PerTarget pst : origin.getTargets()) {
            for (Monitor m : pst.getMonitors()) {
                m.clearSubFlags();
            }
        }
    }

    /**
     * Detect lost & found subscriptions - for logging only.
     */
    private void checkMissingSubs() {
        for (PerTarget pst : origin.getTargets()) {
            for (Monitor m : pst.getMonitors()) {
                m.checkMissingSub();
            }
        }
    }

}
