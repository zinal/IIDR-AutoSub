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

/**
 * Subscription to be monitored.
 * @author zinal
 */
public class AsSubscription {

    private final String name;
    private final AsEngine source;
    private final AsEngine target;
    private boolean skipNewBlobs;

    public AsSubscription(String name, AsEngine source, AsEngine target) {
        this.name = name;
        this.source = source;
        this.target = target;
        this.skipNewBlobs = false;
    }

    public String getName() {
        return name;
    }

    public AsEngine getSource() {
        return source;
    }

    public AsEngine getTarget() {
        return target;
    }

    public boolean isSkipNewBlobs() {
        return skipNewBlobs;
    }

    public void setSkipNewBlobs(boolean skipNewBlobs) {
        this.skipNewBlobs = skipNewBlobs;
    }

}
