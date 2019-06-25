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

import ru.zinal.idrcdc.autosub.Globals;
import ru.zinal.idrcdc.autosub.config.Monitor;
import ru.zinal.idrcdc.autosub.config.TableInfo;

/**
 * Algorithm to validate configuration settings of subscription monitors.
 * @author zinal
 */
public class Validator implements Runnable {
    
    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(Validator.class);
    
    private final Globals globals;
    private final Script script;

    public Validator(Globals globals, Script script) {
        this.globals = globals;
        this.script = script;
    }
    
    @Override
    public void run() {
        LOG.info("Validating configuration settings...");
        int countValid = 0;
        for ( Monitor m : globals.getMonitors() ) {
            LOG.info("Subscription {}: {} -> {}", 
                    m.getSubscription(), m.getSource(),
                    m.getTarget());
            final boolean singleStore = m.getTarget()
                    .equalsIgnoreCase(m.getSource());
            try {
                if (singleStore) {
                    script.dataStore(m.getSource(), Script.ST_BOTH);
                } else {
                    script.dataStore(m.getSource(), Script.ST_SOURCE);
                    script.dataStore(m.getTarget(), Script.ST_TARGET);
                }
                validate(m);
            } catch(Exception ex) {
                m.setDisabled(true);
                LOG.warn("Disabled handling for subscription {}\n\t{}",
                        m.getSubscription(), ex.getMessage());
            }
            // Подсчёт проверенных подписок
            if (!m.isDisabled())
                ++countValid;
        }
        if (countValid > 0) {
            LOG.info("Configuration validated, monitoring {} subscriptions.",
                    countValid);
        } else {
            LOG.info("Empty configuration, nothing to do");
        }
    }
    
    private void validate(Monitor m) {
        m.setDisabled(false);
        script.execute("select subscription name \"{0}\";", m.getSubscription());
        int countEnabled = 0;
        for (TableInfo ti : m.getTables().values()) {
            ti.setDisabled(false);
            try {
                script.execute("select table mapping sourceSchema \"{0}\" "
                        + "sourceTable \"{1}\";", ti.getSchema(), ti.getName());
                ++countEnabled;
            } catch(Exception ex) {
                ti.setDisabled(true);
                LOG.warn("Missing table {} in subscription {}, table skipped.",
                        ti.getFullName(), m.getSubscription(), ex);
            }
        }
        if (countEnabled==0) {
            throw new RuntimeException("No monitored tables");
        }
    }
    
}
