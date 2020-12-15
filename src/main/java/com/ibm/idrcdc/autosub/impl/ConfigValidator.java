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
package com.ibm.idrcdc.autosub.impl;

import java.util.HashMap;
import java.util.Map;
import com.ibm.idrcdc.autosub.monitor.*;
import com.ibm.idrcdc.autosub.config.*;

/**
 * Algorithm to validate configuration settings of subscription monitors.
 * @author zinal
 */
public class ConfigValidator implements Runnable {

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(ConfigValidator.class);

    private final MonitorGroups groups;
    private final Script script;

    public ConfigValidator(MonitorGroups groups, Script script) {
        this.groups = groups;
        this.script = script;
    }

    @Override
    public void run() {
        LOG.info("*** Validating datastores...");
        for (PerEngine engine : groups.getEngines().values()) {
            LOG.info("Datastore {}...", engine.getName());
            try {
                validate(engine);
            } catch(Exception ex) {
                engine.setEnabled(false);
                LOG.warn("Disabled handling for datastore {}\n\t{}",
                        engine.getName(), Misc.liteMessage(ex));
            }
        }
        LOG.info("*** Validating subscriptions...");
        int countValid = 0;
        for ( PerSource ps : groups.getData() ) {
            for ( PerTarget pst : ps.getTargets() ) {
                for ( Monitor m : pst.getMonitors() ) {
                    if (m.getSource().isEnabled()==false
                            || m.getTarget().isEnabled()==false) {
                        // Skipping subscriptions for disabled engines
                        continue;
                    }
                    LOG.info("Subscription {}: {} -> {}",
                            m.getSubscription().getName(),
                            m.getSource().getName(),
                            m.getTarget().getName());
                    try {
                        validate(m);
                    } catch(Exception ex) {
                        m.setEnabled(false);
                        LOG.warn("Disabled handling for subscription {}\n\t{}",
                                m.getSubscription().getName(), Misc.liteMessage(ex));
                    }
                    // Count the enabled subscriptions
                    if (m.isEnabled())
                        ++countValid;
                }
            }
        }
        if (countValid > 0) {
            LOG.info("*** Configuration validated, monitoring {} subscription(s).", countValid);
        } else {
            LOG.info("*** Empty configuration, nothing to do");
        }
    }

    private void validate(PerEngine e) {
        e.setEnabled(false);
        if (! groups.isEngineUsed(e) ) {
            LOG.info("\tSkipping unused datastore {}.", e.getName());
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Datastore {} ({}) access commands below:", e.getName(), e.getType());
            LOG.debug("\tdmshowversion\t{}",           e.cmdVersion());
            LOG.debug("\tdmshowevents\t{}",            e.cmdEvents());
            if (EngineType.Both==e.getType() || EngineType.Source==e.getType()) {
                LOG.debug("\tdmclearstagingstore\t{}", e.cmdClear());
                LOG.debug("\tdmsetbookmark\t{}",       e.cmdBookmarkPut());
                LOG.debug("\tdmreaddtable\t{}",        e.cmdReAddTable());
                LOG.debug("\tdmdescribe\t{}",          e.cmdDescribe());
                LOG.debug("\tdmreassigntable\t{}",     e.cmdReAssignTable());
            }
            if (EngineType.Both==e.getType() || EngineType.Target==e.getType()) {
                LOG.debug("\tdmshowbookmark\t{}",      e.cmdBookmarkGet());
            }
        }

        int code;
        StringBuilder output;
        Map<String,String> subst;

        code = RemoteTool.run("show-version", e.cmdVersion(), (output = new StringBuilder()));
        if (code != 0) {
            LOG.warn("Failed to run dmshowversion, status code {}. Command output below..."
                    + "\n---- BEGIN OUTPUT ----\n"
                    + "{}"
                    + "\n----- END OUTPUT -----", code, output);
            return;
        }

        subst = new HashMap<>();
        subst.put("INSTANCE", e.getEngine().getInstanceName());
        code = RemoteTool.run("get-events", e.cmdEvents(), (output = new StringBuilder()));
        if (code != 0) {
            LOG.warn("Failed to run dmshowevents, status code {}. Command output below..."
                    + "\n---- BEGIN OUTPUT ----\n"
                    + "{}"
                    + "\n----- END OUTPUT -----", code, output);
            return;
        }

        e.setEnabled(true);
    }

    private void validate(Monitor m) {
        m.setEnabled(false);
        // Switch to proper datastores
        if ( m.getTarget() == m.getSource() ) {
            script.dataStore(m.getSource(), EngineType.Both);
        } else {
            script.dataStore(m.getSource(), EngineType.Source);
            script.dataStore(m.getTarget(), EngineType.Target);
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
        m.setEnabled(true);
    }

}
