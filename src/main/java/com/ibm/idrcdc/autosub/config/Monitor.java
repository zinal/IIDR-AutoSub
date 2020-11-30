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
package com.ibm.idrcdc.autosub.config;

import java.util.HashMap;
import java.util.Map;
import org.jdom2.Element;

/**
 * Monitored subscription, including its configuration settings
 * and runtime processing flags.
 * @author zinal
 */
public class Monitor {
    
    private final String subscription;
    private final String source;
    private final String target;
    private final Map<String, TableInfo> tables = new HashMap<>();
    
    private boolean disabled; // incorrect configuration flag
    private boolean known;    // does subscription exist?
    private boolean repair;   // should the subscription be repaired?
    
    private boolean suppressMissing; // for "Missing..." message
    private boolean suppressStopped; // for "Stopped..." message
    private boolean suppressNoRepair; // for "Cannot repair" message

    /**
     * Create object by parsing XML data element
     * @param options XML configuration
     */
    public Monitor(Element options) {
        this.source = Misc.getAttr(options, "source");
        this.target = Misc.getAttr(options, "target");
        this.subscription = Misc.getAttr(options, "name");
        for (Element el : options.getChildren("table")) {
            final TableInfo ti = new TableInfo(el);
            tables.put(ti.getFullName(), ti);
        }
        this.disabled = false;
        this.known = false;
        this.repair = false;
        this.suppressMissing = false;
        this.suppressStopped = false;
        this.suppressNoRepair = false;
    }

    public String getSubscription() {
        return subscription;
    }

    public String getSource() {
        return source;
    }
    
    public String getTarget() {
        return target;
    }

    public Map<String, TableInfo> getTables() {
        return tables;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isKnown() {
        return known;
    }

    public void setKnown(boolean known) {
        this.known = known;
    }

    public boolean isRepair() {
        return repair;
    }

    public void setRepair(boolean repair) {
        this.repair = repair;
    }

    public boolean isSuppressMissing() {
        return suppressMissing;
    }

    public void setSuppressMissing(boolean suppressMissing) {
        this.suppressMissing = suppressMissing;
    }

    public boolean isSuppressStopped() {
        return suppressStopped;
    }

    public void setSuppressStopped(boolean suppressStopped) {
        this.suppressStopped = suppressStopped;
        if (suppressStopped==false)
            setSuppressNoRepair(false);
    }

    public boolean isSuppressNoRepair() {
        return suppressNoRepair;
    }

    public void setSuppressNoRepair(boolean suppressNoRecovery) {
        this.suppressNoRepair = suppressNoRecovery;
    }

}
