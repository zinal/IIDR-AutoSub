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
 * Configuration information for replicated tables,
 * part of a per-subscription configuration.
 * @author zinal
 */
public class TableInfo {
    
    private final String fullName;
    private final Map<String, ColumnInfo> columns = new HashMap<>();
    
    private boolean disabled;
    
    private transient String[] parsedName = null;
    
    public TableInfo(Element options) {
        this.fullName = Misc.getAttr(options, "name");
        for (Element el : options.getChildren("column")) {
            final ColumnInfo ci = new ColumnInfo(el);
            columns.put(ci.getName(), ci);
        }
        this.disabled = false;
    }

    public String getFullName() {
        return fullName;
    }
    
    public String getSchema() {
        if (fullName == null)
            return null;
        parseName();
        if (parsedName.length > 0)
            return parsedName[0];
        return "";
    }
    
    public String getName() {
        if (fullName==null)
            return null;
        parseName();
        if (parsedName.length > 0) {
            return parsedName[parsedName.length - 1];
        }
        return "";
    }
    
    private void parseName() {
        if (parsedName==null) {
            if (fullName==null)
                parsedName = null;
            else
                parsedName = fullName.split("[.]");
        }
    }

    public Map<String, ColumnInfo> getColumns() {
        return columns;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
    
}
