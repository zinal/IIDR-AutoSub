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
package com.ibm.idrcdc.autosub.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Monitoring configuration.
 * @author zinal
 */
public class AsConfig {

    private final Map<String, AsEngine> engines = new HashMap<>();
    private final List<AsSubscription> subscriptions = new ArrayList<>();

    public Map<String, AsEngine> getEngines() {
        return engines;
    }

    public List<AsSubscription> getSubscriptions() {
        return subscriptions;
    }

    public AsEngine getEngine(String name) {
        return engines.get(name);
    }

    public void addEngine(AsEngine ae) {
        engines.put(ae.getName(), ae);
    }

    public void addSubscription(AsSubscription as) {
        subscriptions.add(as);
    }

    public boolean isEngineUsed(AsEngine engine) {
        if (engine==null)
            return false;
        for (AsSubscription sub : subscriptions) {
            if (engine == sub.getSource())
                return true;
            if (engine == sub.getTarget())
                return true;
        }
        return false;
    }

    public boolean isEngineUsed(String name) {
        if (name==null)
            name = "";
        for (AsSubscription sub : subscriptions) {
            if (name.equalsIgnoreCase(sub.getSource().getName()))
                return true;
            if (name.equalsIgnoreCase(sub.getTarget().getName()))
                return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + Objects.hashCode(this.engines);
        hash = 37 * hash + Objects.hashCode(this.subscriptions);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AsConfig other = (AsConfig) obj;
        if (!Objects.equals(this.engines, other.engines)) {
            return false;
        }
        if (!Objects.equals(this.subscriptions, other.subscriptions)) {
            return false;
        }
        return true;
    }

}
