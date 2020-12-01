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

/**
 * Algorithm to validate configuration settings of subscription monitors.
 * @author zinal
 */
public class ConfigValidator implements Runnable {

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(ConfigValidator.class);

    private final AsGroups groups;
    private final Script script;

    public ConfigValidator(AsGroups groups, Script script) {
        this.groups = groups;
        this.script = script;
    }

    @Override
    public void run() {
        LOG.info("Validating configuration settings...");
        int countValid = 0;
        for ( PerSource ps : groups.getData() ) {
            for ( PerSourceTarget pst : ps.getTargets() ) {
                for ( Monitor m : pst.getMonitors() ) {
                    LOG.info("Subscription {}: {} -> {}",
                            m.getSubscription().getName(),
                            m.getSource().getName(),
                            m.getTarget().getName());
                    try {
                        validate(m);
                    } catch(Exception ex) {
                        m.setDisabled(true);
                        LOG.warn("Disabled handling for subscription {}\n\t{}",
                                m.getSubscription().getName(), ex.getMessage());
                    }
                    // Count the enabled subscriptions
                    if (!m.isDisabled())
                        ++countValid;
                }
            }
        }
        if (countValid > 0) {
            LOG.info("Configuration validated, monitoring {} subscriptions.", countValid);
        } else {
            LOG.info("Empty configuration, nothing to do");
        }
    }

    private void validate(Monitor m) {
        m.setDisabled(false);
        // Switch to proper datastores
        if ( m.getTarget() == m.getSource() ) {
            script.dataStore(m.getSource().getName(), EngineType.Both);
        } else {
            script.dataStore(m.getSource().getName(), EngineType.Source);
            script.dataStore(m.getTarget().getName(), EngineType.Target);
        }
        // Check that the subscription exists
        script.execute("select subscription name \"{0}\";", m.getSubscription().getName());
        // Count the table mappings
        script.execute("list table mappings;");
        int mappingsCount = script.getTable().getRowCount();
        if (mappingsCount==0) {
            throw new RuntimeException("Missing table mappings in the subscription");
        }
        LOG.info("\tFound {} table mapping(s)", mappingsCount);
    }

}
