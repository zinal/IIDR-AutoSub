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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import com.ibm.idrcdc.autosub.Worker;
import com.ibm.idrcdc.autosub.monitor.*;
import com.ibm.idrcdc.autosub.config.*;

/**
 * Algorithm to repair subscriptions of a single source datastore.
 * @author zinal
 */
public class Repairman implements Runnable {

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(Repairman.class);

    private final long startTime;
    private final AsGlobals globals;
    private final PerSource origin;
    private final Script script;

    private final Map<String, String> subsToStart = new HashMap<>();
    private final Set<String> selectedTables;

    public Repairman(AsGlobals globals, PerSource origin, Script script) {
        this.startTime = System.currentTimeMillis();
        this.globals = globals;
        this.origin = origin;
        this.script = script;
        this.selectedTables = origin.alteredTables();
    }

    @Override
    public void run() {
        if (selectedTables.isEmpty())
            return;
        if (getPendingSubs().isEmpty())
            return;
        RecoveryReport.enable(globals, origin);
        try {
            repair();
        } finally {
            RecoveryReport.disable();
        }
    }

    public void repair() {
        List<Monitor> pending = getPendingSubs();
        
        LOG.info("Repair sequence STARTED for source datastore {}, "
                + "subscriptions {}, tables {}",
                origin.getSource(), pending, selectedTables);

        if (RecoveryReport.isEnabled()) {
            RecoveryReport.logIf("version", "autosub " + Worker.VERSION);
            RecoveryReport.logIf("metadata", "Datastore " + origin.getName()
                    + ", subs " + pending.toString() 
                    + ", tables " + selectedTables.toString());
        }

        // Connect to the source datastore
        script.dataStore(origin.getSource(), EngineMode.Source);

        boolean repairSucceeded = false;

        try {
            // 1. Grab and print the bookmarks on target datastores.
            if (globals.isGrabBookmarks())
                printAllBookmarks();

            // 2. Reading the column states
            for (Monitor m : getPendingSubs()) {
                grabTableColumns(m);
            }
            if (getPendingSubs().isEmpty())
                return; // Exit if we failed to read the column states

            // 3. Stop all the running subscriptions on the source datastore
            // (necessary for dmclearstagingstore)
            stopAllSubscriptions();

            // 4. Clear the staging store
            if ( ! clearStagingStore() )
                return;

            // 5. Re-adding the altered tables
            for (String tableFull : selectedTables) {
                if (!readdTable(tableFull)) {
                    // Mark all dependant subscriptions as non-recoverable
                    markRepairFailed(tableFull);
                }
            }
            if (getPendingSubs().isEmpty())
                return;

            // 6. Repairing the subscriptions
            for (Monitor m : getPendingSubs()) {
                // Update the subscription and reset the bookmark
                if ( repair(m) ) {
                    // Success - will start the subscription
                    subsToStart.put(m.getSubscription().getName(), m.getTarget().getName());
                    // Reset the failure time in case we have recovered
                    m.setFailureTime(0L);
                }
            }
            if (getPendingSubs().isEmpty())
                return;

            // 7. Restarting the subscriptions
            restartSubscriptions();
            repairSucceeded = true;

            pending = getPendingSubs();
            LOG.info("Repair sequence COMPLETED for source datastore {}, subscriptions {}",
                    origin.getSource(), pending);

            if (RecoveryReport.isEnabled()) {
                RecoveryReport.logIf("success", "Datastore " + origin.getName()
                        + ", subs " + pending.toString());
            }

        } finally {
            if (! repairSucceeded) {
                // Re-start the stopped subscriptions on error, and return
                restartSubscriptions();
                LOG.info("Repair sequence HALTED for source datastore {}",
                        origin.getSource().getName());
            }
        }
    }

    private boolean readdTable(String tableFull) {
        LOG.info("Re-adding table {}...", tableFull);
        final Map<String,String> subst = new HashMap<>();
        subst.put("TABLE", tableFull);
        final StringBuilder output = new StringBuilder();
        int code = RemoteTool.run("readd-table",
                origin.getSource().cmdReAddTable(), subst, output);
        if (code!=0) {
            LOG.error("Failed to re-add table {}, status code {}.\n"
                    + "---- BEGIN OUTPUT ----\n"
                    + "{}\n"
                    + "----- END OUTPUT -----", tableFull, code, output);
            return false;
        }
        return true;
    }

    private boolean repair(Monitor m) {
        final RepairMode mode = m.getRepair();
        if (mode == null || mode == RepairMode.Disabled)
            return false;
        LOG.info("Repairing the subscription {}", m.getSubscription());
        try {
            script.dataStore(m.getTarget(), EngineMode.Target);
            script.execute("select subscription name \"{0}\";",
                    m.getSubscription().getName());
            try { // Ignoring the possible unlock error
                script.execute("unlock subscription;");
            } catch(Exception ex) {}
            // We must lock the subscription before any modifications
            LOG.info("\tLocking subscription...");
            script.execute("lock subscription;");
            LOG.info("\tDescribing subscription...");
            script.execute("describe subscription;");
            for (String tableFull : m.getAlteredTables()) {
                if (!selectedTables.contains(tableFull))
                    continue;
                String[] table = tableFull2Pair(tableFull);
                LOG.info("\tRe-mapping table {}...", tableFull);
                script.execute("select table mapping "
                        + "sourceSchema \"{0}\" sourceTable \"{1}\";",
                        table[0], table[1]);
                Map<String,Boolean> state = m.getColumnState();
                if (state==null)
                    state = Collections.emptyMap();
                script.execute("reassign table mapping;");
                updateReplicatedColumns(m, state);
                if (RepairMode.Refresh == mode) {
                    LOG.info("\tFlagging {} for Refresh...", tableFull);
                    script.execute("flag refresh;");
                }
            }
            LOG.info("\tUnlocking subscription...");
            script.execute("unlock subscription;");
            LOG.info("\tComplete!");
            return true;
        } catch(Exception ex) {
            m.markRepairFailed(startTime);
            LOG.info("\tFailed!", ex);
            return false;
        }
    }

    private boolean grabTableColumns(Monitor m) {
        if (! m.isRepairNeeded())
            return false;
        LOG.info("Reading column data for subscription {}", m.getSubscription());
        try {
            script.dataStore(m.getTarget(), EngineMode.Target);
            script.execute("select subscription name \"{0}\";",
                    m.getSubscription().getName());
            for (String tableFull : m.getAlteredTables()) {
                if (! selectedTables.contains(tableFull) )
                    continue;
                LOG.info("\tAnalysing table {}...", tableFull);
                String[] table = tableFull2Pair(tableFull);
                script.execute("select table mapping "
                        + "sourceSchema \"{0}\" sourceTable \"{1}\";",
                        table[0], table[1]);
                m.setColumnState( grabColumnStates(m) );
            }
            LOG.info("\tComplete!");
            return true;
        } catch(Exception ex) {
            m.markRepairFailed(startTime);
            LOG.info("\tFailed!", ex);
            return false;
        }
    }

    private Map<String,Boolean> grabColumnStates(Monitor m) {
        final Map<String,Boolean> state = new HashMap<>();
        script.execute("list source columns;");
        final ScriptOutput table = script.getTable();
        for (int irow = 0; irow < table.getRowCount(); ++irow) {
            String colname = table.getValueAt(irow, "SOURCE COLUMN");
            String replicate = table.getValueAt(irow, "REPLICATE");
            state.put(colname, "Yes".equalsIgnoreCase(replicate));
        }
        return state;
    }

    private void updateReplicatedColumns(Monitor m, Map<String,Boolean> state) {
        final List<String> includeColumns = new ArrayList<>();
        final List<String> excludeColumns = new ArrayList<>();
        script.execute("list source columns;");
        final ScriptOutput table = script.getTable();
        for (int irow = 0; irow < table.getRowCount(); ++irow) {
            String colname = table.getValueAt(irow, "SOURCE COLUMN");
            String dtype = table.getValueAt(irow, "DATA TYPE");
            String replicateStr = table.getValueAt(irow, "REPLICATE");
            Boolean target = state.get(colname);
            if (target==null) {
                target = Boolean.TRUE;
                if ( m.getSubscription().isSkipNewBlobs() ) {
                    if (isBlobColumn(dtype))
                        target = Boolean.FALSE;
                }
            }
            boolean replicate = "Yes".equalsIgnoreCase(replicateStr);
            if (replicate != target) {
                if (target)
                    includeColumns.add(colname);
                else
                    excludeColumns.add(colname);
            }
        }
        for (String colname : includeColumns) {
            LOG.info("\tIncluding column {}", colname);
            script.execute("filter source column name \"{0}\" "
                    + "replicate yes;", colname);
        }
        for (String colname : excludeColumns) {
            LOG.info("\tExcluding column {}", colname);
            script.execute("filter source column name \"{0}\" "
                    + "replicate no critical no;", colname);
        }
    }

    private static boolean isBlobColumn(String dtype) {
        // TODO: VARCHAR(MAX) for MSSQL???
        return "CLOB".equalsIgnoreCase(dtype)
                || "BLOB".equalsIgnoreCase(dtype);
    }

    private Map<String, String> listRunningSubscriptions() {
        final Map<String, String> retval = new HashMap<>();
        script.execute("monitor replication;");
        final ScriptOutput table = script.getTable();
        for (int irow = 0; irow < table.getRowCount(); ++irow) {
            String subname = table.getValueAt(irow, "SUBSCRIPTION");
            String substate = table.getValueAt(irow, "STATE");
            String target = table.getValueAt(irow, "TARGET DATASTORE");
            if ("Mirror Continuous".equalsIgnoreCase(substate)) {
                retval.put(subname, target);
            }
        }
        return retval;
    }

    private void stopAllSubscriptions() {
        final Map<String, String> runningSubs = listRunningSubscriptions();
        if (runningSubs.isEmpty())
            return;
        LOG.info("Stopping all running subscriptions for datastore {}...", origin.getSource());
        for (Map.Entry<String, String> sub : runningSubs.entrySet()) {
            LOG.info("\tStopping subscription {}...", sub.getKey());
            script.dataStore(sub.getValue(), EngineMode.Target);
            script.execute("select subscription name \"{0}\";", sub.getKey());
            script.execute("end replication method immediate wait {0};",
                    String.valueOf(globals.getWaitStartMirroring()));
        }
        LOG.info("\tWaiting for completion...");
        while (listRunningSubscriptions().isEmpty() == false) {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException ix) {}
        }
        LOG.info("\tComplete!...");
        // All stopped subscriptions must be restarted.
        subsToStart.putAll(runningSubs);
    }

    private boolean clearStagingStore() {
        LOG.info("Clearing the staging store for {}", origin);
        final String command = origin.getSource().cmdClear();
        Map<String,String> subst = new HashMap<>();
        subst.put("SOURCE", origin.getSource().getName());
        int retval = RemoteTool.run("clear-staging", command, subst);
        if (retval != 0) {
            markRepairFailed(null); // mark repair failure for all subscriptions
            LOG.error("Cannot clear the staging store on source, status code {}. "
                    + "Skipping the recovery of all affected subscriptions...", retval);
            return false;
        }
        LOG.info("Staging store cleared!");
        return true;
    }

    private void printAllBookmarks() {
        for (Monitor m : getPendingSubs()) {
            try {
                m.setBookmark(grabBookmark(m));
                LOG.info("Bookmark value for subscription {} is {}",
                        m.getSubscription().getName(), m.getBookmark());
            } catch(Exception ex) {}
        }
    }

    private String grabBookmark(Monitor m) {
        final String command = m.getTarget().cmdBookmarkGet();
        if (StringUtils.isBlank(command))
            throw new RuntimeException("Get bookmark command not configured");
        final StringBuilder data = new StringBuilder();
        int retval = RemoteTool.run("get-bookmark", command, m.substGetBookmark(), data);
        if (retval!=0) {
            LOG.warn("Get bookmark command failed with code {}.\n"
                    + "---- BEGIN OUTPUT ----\n"
                    + "{}\n"
                    + "----- END OUTPUT -----", retval, data);
            throw new RuntimeException("Get bookmark command failed with code " + retval);
        }
        Pattern p = Pattern.compile("^[0-9A-F]{8,512}$");
        for (String v : data.toString().split("[\n]")) {
            if (p.matcher(v).matches())
                return v;
        }
        throw new RuntimeException("Get bookmark command returned illegal output");
    }

    private void markRepairFailed(String tableName) {
        for (PerTarget pst : origin.getTargets()) {
            for (Monitor m : pst.getMonitors()) {
                if (tableName!=null) {
                    if (! m.getAlteredTables().contains(tableName) )
                        continue; // Filter out unrelated subscription monitors
                }
                m.markRepairFailed(startTime);
            }
        }
    }

    private void restartSubscriptions() {
        for (Map.Entry<String, String> sub : subsToStart.entrySet()) {
            LOG.info("Starting subscription {}", sub.getKey());
            try {
                script.dataStore(sub.getValue(), EngineMode.Target);
                script.execute("select subscription name \"{0}\";", sub.getKey());
                try { // Locked subscriptions cannot be started
                    script.execute("unlock subscription;");
                } catch(Exception ex) {}
                script.execute("start mirroring wait {0};",
                        String.valueOf(globals.getWaitStartMirroring()));
            } catch(Exception ex) {
                LOG.error("Failed to start subscription {}", sub.getKey(), ex);
            }
        }
        subsToStart.clear();
    }

    private List<Monitor> getPendingSubs() {
        return origin.pendingMonitors();
    }

    /**
     * Convert the table name from schema.tabname notation 
     * to a pair of schema and tabname.
     * @param tableFull Full table name, dot-separated
     * @return An array of schema and table name
     */
    public static String[] tableFull2Pair(String tableFull) {
        String[] parsedName = tableFull.split("[.]");
        if (parsedName.length == 2)
            return parsedName;
        String schemaName = parsedName.length > 1 ? parsedName[0] : "";
        String tableName = parsedName.length > 0 ? parsedName[parsedName.length - 1] : "";
        return new String[] { schemaName, tableName };
    }

}
