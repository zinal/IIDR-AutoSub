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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.ibm.idrcdc.autosub.config.*;

/**
 * Collection of monitors grouped by source, then by target.
 * @author zinal
 */
public class MonitorGroups {

    private final AsConfig config;
    private final Map<String, PerEngine> engines = new HashMap<>();
    private final List<PerSource> data = new ArrayList<>();

    /**
     * Build the monitor groups based on the configuration.
     * @param config Configuration
     */
    public MonitorGroups(AsConfig config) {
        this.config = config;
        for (AsEngine engine : config.getEngines().values()) {
            engines.put(engine.getName(), new PerEngine(engine));
        }
        for (AsSubscription as : config.getSubscriptions()) {
            // vars to hold the per-source and per-target engine refs
            PerEngine sourceEngine = engines.get(as.getSource().getName());
            PerEngine targetEngine = engines.get(as.getTarget().getName());;
            if (sourceEngine==null || targetEngine==null)
                throw new IllegalStateException("Illegal config for sub " + as);
            // find the existing source group
            PerSource ps = null;
            for (PerSource cur : data) {
                if (cur.getSource().getEngine() == as.getSource()) {
                    ps = cur;
                    break;
                }
            }
            // make new source group if one not found
            if (ps==null) {
                ps = new PerSource(sourceEngine);
                data.add(ps);
            }
            // find the existing target group
            PerTarget pst = null;
            for (PerTarget cur : ps.getTargets()) {
                if (cur.getTarget().getEngine() == as.getTarget()) {
                    pst = cur;
                    break;
                }
            }
            // make new target group if one not found
            if (pst==null) {
                pst = new PerTarget(targetEngine);
                ps.getTargets().add(pst);
            }
            // add the new monitor to the target group
            Monitor m = new Monitor(as, sourceEngine, targetEngine);
            pst.getMonitors().add(m);
        }
    }

    public AsConfig getConfig() {
        return config;
    }

    public Map<String, PerEngine> getEngines() {
        return engines;
    }

    public List<PerSource> getData() {
        return data;
    }

}
