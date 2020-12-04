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
package com.ibm.idrcdc.autosub.model;

import com.ibm.idrcdc.autosub.Monitor;
import com.ibm.idrcdc.autosub.PerTarget;
import com.ibm.idrcdc.autosub.PerSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Collection of monitors grouped by source, then by target.
 * @author zinal
 */
public class AsGroups {

    private final List<PerSource> data = new ArrayList<>();

    /**
     * Build the monitor groups based on the configuration.
     * @param config Configuration
     */
    public AsGroups(AsConfig config) {
        for (AsSubscription as : config.getSubscriptions()) {
            // find the existing source group
            PerSource ps = null;
            for (PerSource cur : data) {
                if (cur.getSource() == as.getSource()) {
                    ps = cur;
                    break;
                }
            }
            // make new source group if one not found
            if (ps==null) {
                ps = new PerSource(as.getSource());
                data.add(ps);
            }
            // find the existing target group
            PerTarget pst = null;
            for (PerTarget cur : ps.getTargets()) {
                if (cur.getTarget() == as.getTarget()) {
                    pst = cur;
                    break;
                }
            }
            // make new target group if one not found
            if (pst==null) {
                pst = new PerTarget(as.getTarget());
                ps.getTargets().add(pst);
            }
            // add the new monitor to the target group
            Monitor m = new Monitor(as);
            pst.getMonitors().add(m);
        }
    }

    public List<PerSource> getData() {
        return data;
    }

}
