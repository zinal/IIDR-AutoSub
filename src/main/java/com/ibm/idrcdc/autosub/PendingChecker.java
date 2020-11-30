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
package com.ibm.idrcdc.autosub;

import com.ibm.idrcdc.autosub.config.Monitor;
import com.ibm.idrcdc.autosub.config.PerSource;
import com.ibm.idrcdc.autosub.config.PerSourceTarget;
import com.ibm.replication.cdc.scripting.ResultStringTable;

/**
 * Algorithm to check subscriptions of a single source datastore
 * for a repairable failure.
 * @author zinal
 */
public class PendingChecker {
    
    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(PendingChecker.class);
    
    private final PerSource source;
    private final Script script;

    public PendingChecker(PerSource source, Script script) {
        this.source = source;
        this.script = script;
    }

    public boolean check() {
        // Сбрасываем флаги наличия подписок и будущей починки
        clearSubFlags();
        // Количество подписок, подлежащих починке
        int countPending = 0;
        // Получаем состояние подписок
        script.dataStore(source.getSource(), Script.ST_SOURCE);
        script.execute("monitor replication;");
        final ResultStringTable table = script.getTable();
        for (int irow = 0; irow < table.getRowCount(); ++irow) {
            String subname = table.getValueAt(irow, "SUBSCRIPTION");
            String substate = table.getValueAt(irow, "STATE");
            String target = table.getValueAt(irow, "TARGET DATASTORE");
            PerSourceTarget pst = source.findTarget(target);
            if (pst==null)
                continue; // Пропускаем неизвестных получателей
            Monitor m = pst.findMonitor(subname);
            if (m==null) // Неизвестные подписки просто пропускаем
                continue;
            // Найденные подписки помечаем как найденные
            m.setKnown(true);
            if ("Mirror Continuous".equalsIgnoreCase(substate)) {
                // Порядок, подписка работает
                if (m.isSuppressStopped()) {
                    m.setSuppressStopped(false);
                    LOG.info("Recovered subscription {}", m.getSubscription());
                }
                continue;
            }
            if (!m.isSuppressStopped()) {
                m.setSuppressStopped(true);
                LOG.info("Subscription {} not working, actual state: {}",
                        m.getSubscription(), substate);
            }
            // Проверяем фактическую причину остановки подписки
            if ( check(m) ) {
                // Причина соответствует нашим методам починки
                ++countPending;
                m.setRepair(true);
            }
        }
        
        // Сигнализация потерянных и вновь найденных подписок
        checkMissingSubs();
        
        return (countPending > 0);
    }

    private boolean check(Monitor m) {
        script.execute("list subscription events name \"{0}\" type source;", 
                m.getSubscription());
        ResultStringTable table = script.getTable();
        boolean have9505 = false;
        for (int irow=0; irow < table.getRowCount(); ++irow) {
            String eventId = table.getValueAt(irow, "EVENT ID");
            if ("9505".equals(eventId)) {
                // Обнаружен значимый оператор DDL
                have9505 = true;
            } else if ("1463".equals(eventId)) {
                // Старт подписки, дальнейшее сканирование не нужно.
                break;
            }
        }
        if (!have9505) {
            // Мы не нашли признаков обрабатываемой нами ситуации.
            if (!m.isSuppressNoRepair()) {
                m.setSuppressNoRepair(true);
                LOG.warn("Cannot repair subscription {}", 
                        m.getSubscription());
            }
            return false;
        }
        // Есть признаки автоматически обрабатываемой ситуации
        if (m.isSuppressNoRepair()) {
            m.setSuppressNoRepair(false);
        }
        LOG.info("Probably able to repair failed subscription {}", 
                m.getSubscription());
        return true;
    }
    
    private void clearSubFlags() {
        for (PerSourceTarget pst : source.getTargets()) {
            for (Monitor m : pst.getMonitors()) {
                m.setKnown(false);
                m.setRepair(false);
            }
        }
    }
    
    private void checkMissingSubs() {
        for (PerSourceTarget pst : source.getTargets()) {
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
