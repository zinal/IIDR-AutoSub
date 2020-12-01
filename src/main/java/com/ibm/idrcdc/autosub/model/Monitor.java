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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Monitored subscription, including its configuration settings
 * and runtime processing flags.
 * @author zinal
 */
public class Monitor {

    private final AsSubscription subscription;

    private boolean disabled; // incorrect configuration (not found on startup)
    private boolean known;    // does subscription exist?
    private boolean repair;   // should the subscription be repaired?

    // altered source tables
    private final List<String> sourceTables = new ArrayList<>();
    // bookmark retrieved from target
    private String bookmark;
    // time of the last failure
    long failureTime;

    private boolean suppressMissing; // for "Missing..." message
    private boolean suppressStopped; // for "Stopped..." message
    private boolean suppressNoRepair; // for "Cannot repair" message

    /**
     * Create object by parsing XML data element
     * @param subscription Subscription configuration
     */
    public Monitor(AsSubscription subscription) {
        this.subscription = subscription;
        this.disabled = false;
        this.known = false;
        this.repair = false;
        this.bookmark = null;
        this.failureTime = 0L;
        this.suppressMissing = false;
        this.suppressStopped = false;
        this.suppressNoRepair = false;
    }

    public AsSubscription getSubscription() {
        return subscription;
    }

    public AsEngine getSource() {
        return subscription.getSource();
    }

    public AsEngine getTarget() {
        return subscription.getTarget();
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

    public List<String> getSourceTables() {
        return sourceTables;
    }

    public String getBookmark() {
        return bookmark;
    }

    public void setBookmark(String bookmark) {
        this.bookmark = bookmark;
    }

    public long getFailureTime() {
        return failureTime;
    }

    public void setFailureTime(long failureTime) {
        this.failureTime = failureTime;
    }

    public void setRepairFailed(long failureTime) {
        this.failureTime = failureTime;
        this.repair = false;
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

    /**
     * Re-set the flags for this monitor.
     * Used by the PendingChecker.
     */
    public void clearSubFlags() {
        known = false;
        repair = false;
        sourceTables.clear();
        bookmark = null;
    }

    public Map<String,String> substGetBookmark() {
        Map<String,String> m = new HashMap<>();
        m.put("SUBSCRIPTION", subscription.getName());
        m.put("SOURCE", subscription.getSource().getName());
        m.put("TARGET", subscription.getTarget().getName());
        return m;
    }

    public Map<String,String> substPutBookmark() {
        Map<String,String> m = substGetBookmark();
        m.put("BOOKMARK", bookmark);
        return m;
    }

}
