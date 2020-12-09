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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A signature of changes detected in the source.
 * Consists of the set of altered table names and of the set of pending subscriptions.
 * @author zinal
 */
public class ChangeSignature {

    private final Map<String, Record> records = new HashMap<>();

    public ChangeSignature(Collection<PerSource> sources) {
        if (sources!=null) {
            for (PerSource ps : sources)
                records.put(ps.getSource().getName(), new Record(ps));
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + Objects.hashCode(this.records);
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
        final ChangeSignature other = (ChangeSignature) obj;
        if (!Objects.equals(this.records, other.records)) {
            return false;
        }
        return true;
    }

    public static final class Record {

        private final Set<String> alteredTables = new HashSet<>();
        private final Set<String> pendingSubscriptions = new HashSet<>();

        /**
         * Extract the validation results to the change signature.
         * @param ps Validation results for the source datastore.
         */
        public Record(PerSource ps) {
            alteredTables.addAll(ps.alteredTables());
            for (Monitor m : ps.pendingMonitors()) {
                pendingSubscriptions.add(m.getSubscription().getName());
            }
        }

        public Set<String> getAlteredTables() {
            return alteredTables;
        }

        public Set<String> getPendingSubscriptions() {
            return pendingSubscriptions;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 89 * hash + Objects.hashCode(this.alteredTables);
            hash = 89 * hash + Objects.hashCode(this.pendingSubscriptions);
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
            final Record other = (Record) obj;
            if (!Objects.equals(this.alteredTables, other.alteredTables)) {
                return false;
            }
            if (!Objects.equals(this.pendingSubscriptions, other.pendingSubscriptions)) {
                return false;
            }
            return true;
        }
    }

}
