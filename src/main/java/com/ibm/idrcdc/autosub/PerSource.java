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

import com.ibm.idrcdc.autosub.model.AsEngine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Monitors for one source datastore, grouped by target datastore
 * @author zinal
 */
public class PerSource {

    private final AsEngine source;
    private final List<PerTarget> targets = new ArrayList<>();

    public PerSource(AsEngine source) {
        this.source = source;
    }

    public AsEngine getSource() {
        return source;
    }

    public List<PerTarget> getTargets() {
        return targets;
    }

    /**
     * Check is the whole source enabled
     * @return true, if at least one corresponding monitor is enabled
     */
    public boolean isEnabled() {
        for (PerTarget pst : targets) {
            if (pst.isEnabled())
                return true;
        }
        return false;
    }

    /**
     * Find monitor by its subscription name
     * @param name subscription name
     * @return Monitor object, or null if not found
     */
    public Monitor findMonitor(String name) {
        for (PerTarget pst : targets) {
            Monitor m = pst.findMonitor(name);
            if (m!=null)
                return m;
        }
        return null;
    }

    /**
     * Find per-target group of monitors, by target datastore name
     * @param name Target datastore name
     * @return Per-target group object, or null if not found
     */
    public PerTarget findTarget(String name) {
        for (PerTarget pst : targets) {
            if (name.equalsIgnoreCase(pst.getTarget().getName()))
                return pst;
        }
        return null;
    }

    /**
     * Return the list of all monitored subscriptions for this source datastore.
     * @return List of subscription monitors.
     */
    public List<Monitor> allMonitors() {
        if (targets.isEmpty())
            return Collections.emptyList();
        if (targets.size() == 1)
            return targets.get(0).getMonitors();
        final List<Monitor> v = new ArrayList<>();
        for (PerTarget pst : targets) {
            v.addAll(pst.getMonitors());
        }
        return v;
    }

   /**
     * Return the list of pending monitored subscriptions for this source datastore.
     * @return List of pending subscription monitors.
     */
    public List<Monitor> pendingMonitors() {
        final List<Monitor> v = new ArrayList<>();
        for (PerTarget pst : targets) {
            for (Monitor m : pst.getMonitors())
                if (m.isRepair())
                    v.add(m);
        }
        return v;
    }

    /**
     * Collect all altered tables from the subscriptions waiting for recovery.
     * @return Set of altered table names.
     */
    public Set<String> alteredTables() {
        final Set<String> v = new HashSet<>();
        for (PerTarget pst : targets) {
            for (Monitor m : pst.getMonitors())
                if (m.isRepair())
                    v.addAll(m.getAlteredTables());
        }
        return v;
    }

    @Override
    public String toString() {
        return source.getName();
    }

}
