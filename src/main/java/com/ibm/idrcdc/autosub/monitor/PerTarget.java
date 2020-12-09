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
package com.ibm.idrcdc.autosub.monitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Monitors for one target datastore
 * (previously filtered by source datastore)
 * @author zinal
 */
public class PerTarget {

    private final PerEngine target;
    private final List<Monitor> monitors = new ArrayList<>();
    private boolean enabled;

    public PerTarget(PerEngine target) {
        this.target = target;
        this.enabled = true;
    }

    public PerEngine getTarget() {
        return target;
    }

    public List<Monitor> getMonitors() {
        return monitors;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Check whether the per-source-target is enabled for monitoring
     * @return true, if at least one corresponding monitor is enabled
     */
    public boolean isFullyEnabled() {
        if (enabled) {
            for (Monitor m : monitors) {
                if (m.isEnabled())
                    return true;
            }
        }
        return false;
    }

    /**
     * Find monitor by its subscription name
     * @param name subscription name
     * @return Monitor object, or null if not found
     */
    public Monitor findMonitor(String name) {
        for (Monitor m : monitors) {
            if (name.equalsIgnoreCase(m.getSubscription().getName()))
                return m;
        }
        return null;
    }

}
