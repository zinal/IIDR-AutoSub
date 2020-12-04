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
import com.ibm.idrcdc.autosub.model.AsSubscription;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Monitored subscription, including its configuration settings
 * and runtime processing flags.
 * @author zinal
 */
public class Monitor {

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(Monitor.class);

    private final AsSubscription subscription;

    private boolean disabled; // incorrect configuration (not found on startup)
    private boolean known;    // does subscription exist?
    private boolean repair;   // should the subscription be repaired?

    // all replicated source tables
    private final List<String> sourceTables = new ArrayList<>();
    // altered source tables
    private final List<String> alteredTables = new ArrayList<>();
    // bookmark retrieved from target
    private String bookmark;
    // state of columns before re-adding the tables
    private Map<String,Boolean> columnState;
    // time of the last failure
    private long failureTime;

    // Message suppression flags, to avoid endless duplicates in the log.
    private boolean suppressMissing;  // for "Missing..." message
    private boolean suppressStopped;  // for "Stopped..." message
    private boolean suppressNoRepair; // for "Cannot repair" message
    private boolean suppressLocked;   // for "Locked recovery" message

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
        this.suppressLocked = false;
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

    public List<String> getAlteredTables() {
        return alteredTables;
    }

    public void filterAlteredTables(Set<String> allowed) {
        if (alteredTables.isEmpty() || allowed.isEmpty()
                || allowed.containsAll(alteredTables))
            return;
        List<String> temp = new ArrayList<>(alteredTables);
        alteredTables.clear();
        for (String v : temp)
            if (allowed.contains(v))
                alteredTables.add(v);
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

    public Map<String, Boolean> getColumnState() {
        return columnState;
    }

    public void setColumnState(Map<String, Boolean> columnState) {
        this.columnState = columnState;
    }

    public long getFailureTime() {
        return failureTime;
    }

    public void setFailureTime(long failureTime) {
        this.failureTime = failureTime;
    }

    @Override
    public String toString() {
        return subscription.getName();
    }

    /**
     * Mark the table as not repairable, and save the time of failure
     * to compute the retry delay.
     * @param failureTime Time of failure of the subscription recovery attempt.
     */
    public void markRepairFailed(long failureTime) {
        this.failureTime = failureTime;
        this.repair = false;
    }

    /**
     * Re-set the flags for this monitor.
     * Used by the PendingChecker.
     */
    public void clearSubFlags() {
        known = false;
        repair = false;
        alteredTables.clear();
        sourceTables.clear();
        bookmark = null;
    }

    public Map<String,String> substGetBookmark() {
        Map<String,String> m = new HashMap<>();
        m.put("SUB", subscription.getName());
        m.put("SOURCE", subscription.getSource().getName());
        m.put("TARGET", subscription.getTarget().getName());
        return m;
    }

    public Map<String,String> substPutBookmark() {
        Map<String,String> m = new HashMap<>();
        m.put("SUB", subscription.getName());
        m.put("SOURCE", subscription.getSource().getName());
        m.put("TARGET", subscription.getTarget().getName());
        m.put("BOOKMARK", bookmark);
        return m;
    }

    public void checkMissingSub() {
        if (known) {
            if (suppressMissing) {
                suppressMissing = false;
                LOG.info("Found subscription {}", subscription.getName());
            }
        } else {
            if (! suppressMissing) {
                suppressMissing = true;
                LOG.warn("Lost subscription {}", subscription.getName());
            }
        }
    }

    public void reportRecoveryLocked(String tabName, Monitor other) {
        if (! suppressLocked) {
            suppressLocked = true;
            LOG.info("Locked recovery of subscription {} with altered table {} "
                    + "by another subscription {}",
                    subscription.getName(), tabName, other.getSubscription().getName());
        }
    }

    public void resetRecoveryLocked() {
        suppressLocked = false;
    }

    public void reportSubscriptionRecovered() {
        if (suppressStopped) {
            suppressStopped = false;
            LOG.info("Recovered subscription {}", subscription.getName());
        }
        // In case the subscription recovered, we will report this too again.
        suppressNoRepair = false;
        suppressLocked = false;
    }

    public void reportSubscriptionFailed(String substate) {
        if (! suppressStopped ) {
            suppressStopped = true;
            LOG.info("Subscription {} not working, actual state: {}",
                    subscription.getName(), substate);
        }
    }

    public void reportCannotRepair() {
        if (! suppressNoRepair ) {
            suppressNoRepair = true;
            LOG.warn("Cannot repair the failed subscription {}", subscription.getName());
        }
    }

    public void resetCannotRepair() {
        suppressNoRepair = false;
    }

}
