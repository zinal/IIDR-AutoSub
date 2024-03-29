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

/**
 * CDC Engine mode
 * @author zinal
 */
public enum EngineMode {

    /**
     * Able to act as a source.
     */
    Source,
    
    /**
     * Able to act as a target.
     */
    Target,
    
    /**
     * Able to act as both source and target.
     */
    Dual;

    public static EngineMode fromString(String value) {
        if (value==null)
            return null;
        value = value.trim();
        if (value.length()==0)
            return null;
        if ("Source".equalsIgnoreCase(value))
            return Source;
        if ("Target".equalsIgnoreCase(value))
            return Target;
        if ("Dual".equalsIgnoreCase(value))
            return Dual;
        if ("Both".equalsIgnoreCase(value))
            return Dual;
        throw new IllegalArgumentException("Not an EngineMode: " + value);
    }

}
