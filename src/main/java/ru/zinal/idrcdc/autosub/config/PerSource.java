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
package ru.zinal.idrcdc.autosub.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Monitors for one source datastore, grouped by target datastore
 * @author zinal
 */
public class PerSource {
    
    private final String source;
    private final List<PerSourceTarget> targets = new ArrayList<>();
    
    public PerSource(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    public List<PerSourceTarget> getTargets() {
        return targets;
    }
    
    /**
     * Check is the whole source disabled
     * @return true, if all corresponding monitors are disabled
     */
    public boolean isDisabled() {
        for (PerSourceTarget pst : targets) {
            if (!pst.isDisabled())
                return false;
        }
        return true;
    }
    
    /**
     * Find monitor by its subscription name
     * @param name subscription name
     * @return Monitor object, or null if not found
     */
    public Monitor findMonitor(String name) {
        for (PerSourceTarget pst : targets) {
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
    public PerSourceTarget findTarget(String name) {
        for (PerSourceTarget pst : targets) {
            if (name.equalsIgnoreCase(pst.getTarget()))
                return pst;
        }
        return null;
    }
    
    /**
     * Build the list of replicated tables, without repetitions.
     * @return List of replicated tables
     */
    public List<TableInfo> extractPendingTables() {
        final List<TableInfo> retval = new ArrayList<>();
        for (PerSourceTarget pst : targets) {
            for (Monitor m : pst.getMonitors()) {
                if (!m.isRepair())
                    continue;
                for (TableInfo ti : m.getTables().values()) {
                    boolean found = false;
                    for (TableInfo xti : retval) {
                        if (ti.getFullName()
                                .equalsIgnoreCase(xti.getFullName())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                        retval.add(ti);
                }
            }
        }
        return retval;
    }
    
}
