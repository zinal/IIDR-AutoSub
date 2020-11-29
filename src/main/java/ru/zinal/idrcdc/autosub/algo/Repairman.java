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
package ru.zinal.idrcdc.autosub.algo;

import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import com.ibm.replication.cdc.scripting.ResultStringTable;
import ru.zinal.idrcdc.autosub.config.*;

/**
 * Algorithm to repair subscriptions of a single source datastore.
 * @author zinal
 */
public class Repairman implements Runnable {

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(Repairman.class);

    private final Config config;
    private final PerSource source;
    private final Script script;

    private final Map<String, String> subsToStart = new HashMap<>();

    public Repairman(Config config, PerSource source, Script script) {
        this.config = config;
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
        
        // 0. Подключаемся к источнику
        script.dataStore(source.getSource(), Script.ST_SOURCE);
        
        // 1. Выполняем остановку всех работающих подписок
        // (требуется для выполнения dmclearstagingstore)
        if (config.isStagingClearEnable()) {
            stopAllSubscriptions();
        }
        
        // 2. Повторно добавляем метаданные всех таблиц
        List<TableInfo> tables = source.extractPendingTables();
        for (TableInfo ti : tables) {
            LOG.info("Re-adding table {}...", ti.getFullName());
            script.execute("readd replication table name \"{0}\" "
                    + "schema \"{1}\" table \"{2}\";",
                    source.getSource(), ti.getSchema(), ti.getName());
        }
        
        // 3. Чиним подписки, которые можно починить
        for (PerSourceTarget pst : source.getTargets()) {
            repair(pst);
        }
        
        // 4. Очистка staging store
        if (config.isStagingClearEnable())
            clearStagingStore();
        
        // 5. Запускаем специально остановленные или исправленные подписки
        for (Map.Entry<String, String> sub : subsToStart.entrySet()) {
            LOG.info("Starting subscription {}", sub.getKey());
            try {
                script.dataStore(sub.getValue(), Script.ST_TARGET);
                script.execute("select subscription name \"{0}\";", sub.getKey());
                try { // Заблокированные подписки не удаётся запустить
                    script.execute("unlock subscription;");
                } catch(Exception ex) {}
                script.execute("start mirroring wait {0};",
                        String.valueOf(config.getWaitStartMirroring()));
            } catch(Exception ex) {
                LOG.warn("Failed to start subscription {}", sub.getKey(), ex);
            }
        }

        LOG.info("Repair sequence complete for source datastore {}", 
                source.getSource());
    }

    private void repair(PerSourceTarget pst) {
        // Подключаемся к получателю
        script.dataStore(pst.getTarget(), Script.ST_TARGET);
        for (Monitor m : pst.getMonitors()) {
            if (m.isRepair())
                try {
                    // Починка каждой из подписок
                    repair(m);
                } catch(Exception ex) {
                    LOG.warn("Failed to repair subscription {}", ex);
                }
        }
    }

    private void repair(Monitor m) {
        LOG.info("Processing subscription {}", m.getSubscription());
        script.execute("select subscription name \"{0}\";", 
                m.getSubscription());
        try { // Игнорируем возможную ошибоку разблокировки
            script.execute("unlock subscription;");
        } catch(Exception ex) {}
        script.execute("lock subscription;");
        LOG.info("\tDescribing...");
        script.execute("describe subscription;");
        for (TableInfo ti : m.getTables().values()) {
            LOG.info("\tRe-mapping table {}...", ti.getFullName());
            script.execute("select table mapping "
                    + "sourceSchema \"{0}\" sourceTable \"{1}\";",
                    ti.getSchema(), ti.getName());
            script.execute("reassign table mapping;");
            updateReplicatedColumns(ti);
            LOG.info("\tMarking capture point...");
            script.execute("mark capture point;");
        }
        LOG.info("\tUnlocking...");
        script.execute("unlock subscription;");
        subsToStart.put(m.getSubscription(), m.getTarget());
        LOG.info("\tComplete!");
    }
    
    private void updateReplicatedColumns(TableInfo ti) {
        final List<ColumnInfo> includeColumns = new ArrayList<>();
        final List<String> excludeColumns = new ArrayList<>();
        script.execute("list source columns;");
        final ResultStringTable table = script.getTable();
        for (int irow = 0; irow < table.getRowCount(); ++irow) {
            if ("Yes".equalsIgnoreCase(table.getValueAt(irow, "DERIVED")))
                continue; // Пропускаем сгенерированные колонки
            String colname = table.getValueAt(irow, "SOURCE COLUMN");
            String replicate = table.getValueAt(irow, "REPLICATE");
            ColumnInfo ci = ti.getColumns().get(colname);
            if (ci==null) {
                if ("Yes".equalsIgnoreCase(replicate))
                    excludeColumns.add(colname);
            } else {
                if (!"Yes".equalsIgnoreCase(replicate))
                    includeColumns.add(ci);
            }
        }
        for (ColumnInfo ci : includeColumns) {
            LOG.info("\tIncluding column {}", ci.getName());
            script.execute("filter source column name \"{0}\" "
                    + "replicate yes critical {1};",
                    ci.getName(),
                    ci.isCritical() ? "yes" : "no");
        }
        for (String colname : excludeColumns) {
            LOG.info("\tExcluding column {}", colname);
            script.execute("filter source column name \"{0}\" "
                    + "replicate no critical no;", colname);
        }
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
            script.dataStore(sub.getValue(), Script.ST_TARGET);
            script.execute("select subscription name \"{0}\";", sub.getKey());
            script.execute("end replication method immediate wait {0};",
                    String.valueOf(config.getWaitStartMirroring()));
        }
        do {
            try { // Пауза на 1 секунду
                Thread.sleep(1000L);
            } catch (InterruptedException ix) {}
        } while (listRunningSubscriptions().isEmpty() == false);
    }

    private void clearStagingStore() {
        try {
            LOG.info("Starting the tool to clear staging store...");
            final Process proc = new ProcessBuilder(
                    config.getStagingClearTool(), source.getSource())
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader br =
                    new BufferedReader(
                            new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = br.readLine())!=null) {
                    LOG.info("SSTORE-OUTPUT: {}", line);
                }
            }
            int retCode = proc.waitFor();
            if (retCode != 0)
                throw new Exception("Failed with error code " + retCode);
            LOG.info("Staging store cleared.");
        } catch(Exception ex) {
            LOG.error("Failed to clear the staging store, "
                    + "subscription may not run properly", ex);
        }
    }
    
}
