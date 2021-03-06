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
package com.ibm.idrcdc.autosub.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import com.ibm.idrcdc.autosub.monitor.*;
import com.ibm.idrcdc.autosub.config.*;

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
     * @return true, if there are subscriptions for recovery, and false otherwise
     */
    public boolean check() {
        // Clean up all flags to their initial states
        clearSubFlags();
        // Grab the subscription states
        script.dataStore(origin.getSource(), EngineMode.Source);
        script.execute("monitor replication;");
        final ScriptOutput table = script.getTable();
        for (int irow = 0; irow < table.getRowCount(); ++irow) {
            checkRow(table, irow);
        }
        // Signal lost & found subscriptions
        checkMissingSubs();
        // Select altered tables based on dependencies
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
        if (m==null) // Skip the unmonitored subscription
            return false;
        if (! m.getTarget().isEnabled())
            return false; // The target datastore is not enabled
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
            m.setRepair( checkMonitor(m) );
            return m.isRepairNeeded();
        }
        return false;
    }

    /**
     * Check the event log of a failed subscription for DDL.
     * @param m Monitor object for a subscription
     * @return true if the subscription should be recovered, false otherwise.
     */
    private RepairMode checkMonitor(Monitor m) {
        if (m.getFailureTime() != 0L) {
            // Pause recovery analysis attempts if there was a failed recovery.
            final long diff = startTime - m.getFailureTime();
            if (diff < globals.getPauseAfterError())
                return RepairMode.Disabled;
        }
        // Find the event with the altered table name in its text.
        // We need the oldest message, but not older then subscription start.
        AlterTableEventType messageType = null;
        int messageIndex = -1;
        script.execute("list subscription events name \"{0}\" type source;",
                m.getSubscription().getName());
        final ScriptOutput table = script.getTable();
        for (int irow=0; irow < table.getRowCount(); ++irow) {
            String eventId = table.getValueAt(irow, "EVENT ID");
            if ("1463".equals(eventId)) {
                // Subscription start, no need to scan further.
                break;
            }
            AlterTableEventType fmt = AlterTableEventType.find(eventId);
            if (fmt != null) {
                messageType = fmt;
                messageIndex = irow;
            }
        }
        if (messageType == null) {
            // Unsupported case - no message we could identify.
            m.reportCannotRepair();
            return RepairMode.Disabled;
        }

        // We have a message to parse, now retrieving its text
        script.execute("show subscription event details row {0} ;",
                String.valueOf(messageIndex+1));
        final String text = messageToLine(
                script.getTable().getValueAt(1, 1) );
        
        // Retrieve the altered table name from the messages.
        String tableName = null;
        boolean requireRefresh = false;
        switch (messageType) {
            case M9505:
                tableName = parse9505(text);
                break;
            case M9506:
                tableName = parse9506(text);
                // This message means DDL->DML-DDL
                requireRefresh = true;
                break;
            case M9602:
                tableName = parse9602(text);
                break;
            case M9519:
                tableName = parse9519(text);
                break;
        }

        LOG.debug("Table name {} from {}", tableName, text);

        if (StringUtils.isBlank(tableName)) {
            // Not a case we support.
            m.reportCannotRepair();
            return RepairMode.Disabled;
        }

        if (! m.getSource().isDdlAware()) {
            // We need a refresh for non-DDL-aware sources.
            requireRefresh = true;
            LOG.debug("Non-DDL-aware source {}", m.getSource());
        }

        switch ( m.getRefreshMode() ) {
            case Never:
                if (requireRefresh) {
                    LOG.debug("We need a Refresh, but settings prohibit it");
                    m.reportCannotRepair();
                    return RepairMode.Disabled;
                }
                break;
            case Allow:
                break;
            case Force:
                requireRefresh = true;
                break;
        }

        // Seems to be a supported case.
        m.resetCannotRepair();
        m.getAlteredTables().add(tableName);

        LOG.debug("Probably able to repair the failed subscription {}, tables {}",
                m.getSubscription(), m.getAlteredTables());

        return requireRefresh ? RepairMode.Refresh : RepairMode.Normal;
    }

    private static String messageToLine(String msg) {
        if (msg==null)
            return null;
        while (msg.contains("\n\r"))
            msg = msg.replace("\n\r", " ");
        return msg.replace('\n', ' ').replace('\r', ' ');
    }
    
    private String parse9505(String text) {
// Oracle, Db2:
// IBM XXX has encountered a critical data definition (DDL) change for source table
// METADEMO.TAB0 and will shutdown. Please re-add the table definition ...
        final String textBegin = "(DDL) change for source table ";
        int tabBegin = text.indexOf(textBegin);
        int tabEnd = text.indexOf(" and will shutdown. Please re-add ");
        if (tabBegin < 0 || tabEnd < 0 || tabBegin >= tabEnd) {
            LOG.warn("Failed to parse the 9505 message text:\n\t{}", text);
            return null;
        } else {
            tabBegin += textBegin.length();
            return text.substring(tabBegin, tabEnd);
        }
    }
    
    private String parse9506(String text) {
// Oracle, PostgreSQL:
// IBM XXX will be shutdown due to an error while parsing logs 
// for table myuser.pgtab1 at position 00000000/017235d8. 
// The table definition may have changed.
        final String textBegin = "error while parsing logs for table ";
        int tabBegin = text.indexOf(textBegin);
        int tabEnd = text.indexOf(" at position ");
        tabBegin += textBegin.length();
        if (tabBegin < 0 || tabEnd < 0 || tabBegin >= tabEnd) {
            LOG.warn("Failed to parse the 9506 message text:\n\t{}", text);
            return null;
        } else {
            return text.substring(tabBegin, tabEnd);
        }
    }
    
    private String parse9602(String text) {
// PostgreSQL, Db2:
// An exception has occurred during mirroring.
// Stopping replication because table definition has changed for table "myuser.pgtab1".
        final String textBegin = "definition has changed for table ";
        int tabBegin = text.indexOf(textBegin);
        int tabEnd = text.length() - 1;
        if (!text.endsWith("\"."))
            tabEnd = -1;
        tabBegin += textBegin.length();
        if (tabBegin < 0 || tabEnd < 0 || tabBegin >= tabEnd) {
            LOG.warn("Failed to parse the 9602 message text:\n\t{}", text);
            return null;
        } else {
            String tableName = text.substring(tabBegin, tabEnd);
            tableName = tableName.replace("\"", "");
            return tableName;
        }
    }

    private String parse9519(String text) {
// MSSQL:
// IBM XXX will be shutdown because the latest table definition
// for table metademo.tab0 is newer than the table definition 
// for the current operation. Refresh the table, ...
        final String textBegin = "table definition for table ";
        int tabBegin = text.indexOf(textBegin);
        int tabEnd = text.indexOf(" is newer than the table definition ");
        tabBegin += textBegin.length();
        if (tabBegin < 0 || tabEnd < 0 || tabBegin >= tabEnd) {
            LOG.warn("Failed to parse the 9519 message text:\n\t{}", text);
            return null;
        } else {
            String tableName = text.substring(tabBegin, tabEnd);
            tableName = tableName.replace("\"", "");
            return tableName;
        }
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
            script.dataStore(m.getTarget().getName(), EngineMode.Target);
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
                        lockedMon.setRepair(RepairMode.Disabled);
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

        // Leave only the selected tables as altered in all pending monitors.
        // If the list of altered tables becomes empty, exclude the monitor from repairs.
        pendingMonitors = origin.pendingMonitors();
        for (Monitor m : pendingMonitors) {
            m.filterAlteredTables(selectedTables);
            if (m.getAlteredTables().isEmpty()) {
                LOG.debug("Excluded all altered tables for sub {}", m);
                m.setRepair(RepairMode.Disabled);
            }
        }

        if (! selectedTables.isEmpty()) {
            pendingMonitors = origin.pendingMonitors();
            LOG.info("Selected table(s) {} in subscription(s) {} "
                    + "for recovery{}", selectedTables, pendingMonitors,
                    plannedRefresh(pendingMonitors) ? " with REFRESH" : "");
        }
    }
    
    private static boolean plannedRefresh(List<Monitor> pendingMonitors) {
        for (Monitor m : pendingMonitors) {
            if (RepairMode.Refresh == m.getRepair())
                return true;
        }
        return false;
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
