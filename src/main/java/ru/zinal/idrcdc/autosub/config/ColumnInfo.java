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
package ru.zinal.idrcdc.autosub.config;

import org.jdom2.Element;

/**
 * Configuration settings for columns in replicated tables,
 * part of per-subscription configuration settings.
 * @author zinal
 */
public class ColumnInfo {
    
    private final String name;
    private final boolean critical;

    public ColumnInfo(Element options) {
        this.name = Misc.getAttr(options, "name");
        this.critical = Misc.getAttr(options, "critical", false);
    }

    public String getName() {
        return name;
    }

    public boolean isCritical() {
        return critical;
    }
    
}
