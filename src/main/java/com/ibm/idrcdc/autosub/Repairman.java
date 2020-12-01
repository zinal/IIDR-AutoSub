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

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import com.ibm.replication.cdc.scripting.ResultStringTable;
import com.ibm.idrcdc.autosub.model.*;

/**
 * Algorithm to repair subscriptions of a single source datastore.
 * @author zinal
 */
public class Repairman implements Runnable {

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(Repairman.class);

    private final long startTime;
    private final AsGlobals globals;
    private final PerSource source;
    private final Script script;

    private final Map<String, String> subsToStart = new HashMap<>();

    public Repairman(AsGlobals globals, PerSource source, Script script) {
        this.startTime = System.currentTimeMillis();
        this.globals = globals;
        this.source = source;
        this.script = script;
    }

    public Map<String, String> getSubsToStart() {
        return subsToStart;
    }

    @Override
    public void run() {
        LOG.info("Repair sequence started for source datastore {}",
                source.getSource());

        // 1. Grab the bookmarks on proper targets.
        grabAllBookmarks();

        // 2. Connect to the source datastore
        script.dataStore(source.getSource(), EngineType.Source);

        // 3. Stop all the running subscriptions
        // (necessary for dmclearstagingstore)
        stopAllSubscriptions();

        // 4. Clear the staging store
        if ( ! clearStagingStore() ) {
            // Re-start the stopped subscriptions on error, and return
            restartSubscriptions();
            LOG.info("Repair sequence HALTED for source datastore {}",
                    source.getSource().getName());
            return;
        }

        // 5. Re-adding the altered tables
        Set<String> tables = source.extractAlteredTables();
        for (String tableFull : tables) {
            if (!readdTable(tableFull)) {
                setRepairFailed(tableFull);
            }
        }

        // 6. Repairing the subscriptions
        for (PerTarget pst : source.getTargets()) {
            for (Monitor m : pst.getMonitors()) {
                if (m.isRepair())
                    repair(m);
            }
        }

        // 7. Setting the bookmarks
        for (PerTarget pst : source.getTargets()) {
            for (Monitor m : pst.getMonitors()) {
                if (m.isRepair())
                    resetBookmark(m);
            }
        }

        // 8. Restarting the subscriptions
        restartSubscriptions();

        LOG.info("Repair sequence COMPLETED for source datastore {}",
                source.getSource().getName());
    }

    private boolean readdTable(String tableFull) {
        String[] table = tableFull2Pair(tableFull);
        LOG.info("Re-adding table {}...", tableFull);
        try {
            script.execute("readd replication table name \"{0}\" "
                    + "schema \"{1}\" table \"{2}\";",
                    source.getSource(), table[0], table[1]);
            return true;
        } catch(Exception ex) {
            LOG.error("Failed to re-add table {} to the replication", ex);
            return false;
        }
    }

    private void repair(Monitor m) {
        if (! m.isRepair())
            return;
        LOG.info("Processing subscription {}", m.getSubscription());
        try {
            script.dataStore(m.getTarget(), EngineType.Target);
            script.execute("select subscription name \"{0}\";",
                    m.getSubscription());
            try { // Ignoring the possible unlock error
                script.execute("unlock subscription;");
            } catch(Exception ex) {}
            // We must lock the subscription before any modifications
            LOG.info("\tLocking...");
            script.execute("lock subscription;");
            LOG.info("\tDescribing...");
            script.execute("describe subscription;");
            for (String tableFull : m.getSourceTables()) {
                LOG.info("\tRe-mapping table {}...", tableFull);
                String[] table = tableFull2Pair(tableFull);
                script.execute("select table mapping "
                        + "sourceSchema \"{0}\" sourceTable \"{1}\";",
                        table[0], table[1]);
                Map<String,Boolean> state = grabColumnStatus(m);
                script.execute("reassign table mapping;");
                updateReplicatedColumns(m, state);
                LOG.info("\tParking...");
                script.execute("park table mapping;");
                LOG.info("\tSwitching to REFRESH...");
                script.execute("modify table mapping method refresh;");
                LOG.info("\tSwitching to MIRROR...");
                script.execute("modify table mapping method mirror;");
                LOG.info("\tMarking capture point...");
                script.execute("mark capture point;");
            }
            LOG.info("\tUnlocking...");
            script.execute("unlock subscription;");
            LOG.info("\tComplete!");

            subsToStart.put(m.getSubscription().getName(), m.getTarget().getName());
            m.setFailureTime(0L); // Reset the failure time in case we have recovered
        } catch(Exception ex) {
            m.setRepairFailed(startTime);
            LOG.info("\tFailed!", ex);
        }
    }

    private Map<String,Boolean> grabColumnStatus(Monitor m) {
        final Map<String,Boolean> state = new HashMap<>();
        script.execute("list source columns;");
        final ResultStringTable table = script.getTable();
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
        final ResultStringTable table = script.getTable();
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
        return "CLOB".equalsIgnoreCase(dtype)
                || "BLOB".equalsIgnoreCase(dtype);
    }

    private Map<String, String> listRunningSubscriptions() {
        final Map<String, String> retval = new HashMap<>();
        script.execute("monitor replication;");
        final ResultStringTable table = script.getTable();
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
        subsToStart.putAll(runningSubs);
        LOG.info("Stopping all subscriptions...");
        for (Map.Entry<String, String> sub : runningSubs.entrySet()) {
            script.dataStore(sub.getValue(), EngineType.Target);
            script.execute("select subscription name \"{0}\";", sub.getKey());
            script.execute("end replication method immediate wait {0};",
                    String.valueOf(globals.getWaitStartMirroring()));
        }
        do {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException ix) {}
        } while (listRunningSubscriptions().isEmpty() == false);
    }

    private boolean clearStagingStore() {
        final String command = source.getSource().getCommandClear();
        int retval = new RemoteTool("cmd-clear-staging", command) . execute();
        if (retval != 0) {
            setRepairFailed(null); // mark repair failure for all subscriptions
            LOG.error("Cannot clear the staging store on source, status code {}. "
                    + "Skipping the recovery of all affected subscriptions...", retval);
            return false;
        }
        return true;
    }

    private void grabAllBookmarks() {
        for (PerTarget pst : source.getTargets()) {
            for (Monitor m : pst.getMonitors()) {
                if (!m.isRepair())
                    continue;
                try {
                    m.setBookmark(grabBookmark(m));
                    LOG.info("Bookmark value for subscription {} is {}",
                            m.getSubscription().getName(), m.getBookmark());
                } catch(Exception ex) {
                    m.setRepairFailed(startTime); // mark repair failure
                    LOG.error("Cannot retrieve bookmark for subscription {}. "
                            + "Skipping the recovery...", m.getSubscription().getName(), ex);
                }
            }
        }
    }

    private String grabBookmark(Monitor m) {
        final String command = m.getTarget().getCommandBookmarkGet();
        if (StringUtils.isBlank(command))
            throw new RuntimeException("Get bookmark command not configured");
        final StringBuilder data = new StringBuilder();
        int retval = new RemoteTool("cmd-get-bookmark",
                command, m.substGetBookmark()) . execute(data);
        if (retval!=0)
            throw new RuntimeException("Get bookmark command failed with code " + retval);
        return data.toString().trim();
    }

    private boolean resetBookmark(Monitor m) {
        final String command = m.getSource().getCommandBookmarkGet();
        if (StringUtils.isBlank(command)) {
            LOG.warn("Put bookmark command not configured for source {}", m.getSource().getName());
            return false;
        }
        int retval = new RemoteTool("cmd-put-bookmark",
                command, m.substPutBookmark()) . execute();
        if (retval != 0) {
            LOG.warn("Failed to set bookmark for subscription {} with status code {}",
                    m.getSubscription().getName(), retval);
            return false;
        }
        return true;
    }

    private void setRepairFailed(String tableName) {
        for (PerTarget pst : source.getTargets()) {
            for (Monitor m : pst.getMonitors()) {
                if (tableName!=null) {
                    if (!m.getSourceTables().contains(tableName))
                        continue; // Filter out unrelated subscription monitors
                }
                m.setRepairFailed(startTime);
            }
        }
    }

    private void restartSubscriptions() {
        for (Map.Entry<String, String> sub : subsToStart.entrySet()) {
            LOG.info("Starting subscription {}", sub.getKey());
            try {
                script.dataStore(sub.getValue(), EngineType.Target);
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
    }

    private static String[] tableFull2Pair(String tableFull) {
        String[] parsedName = tableFull.split("[.]");
        if (parsedName.length == 2)
            return parsedName;
        String schemaName = parsedName.length > 1 ? parsedName[0] : "";
        String tableName = parsedName.length > 0 ? parsedName[parsedName.length - 1] : "";
        return new String[] { schemaName, tableName };
    }

}
