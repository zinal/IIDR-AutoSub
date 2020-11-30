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

import org.jdom2.Element;

/**
 * Supporting mini-algorithms
 * @author zinal
 */
public class Misc {

    public static String getAttr(Element el, String name) {
        String val = el.getAttributeValue(name);
        if (val==null) {
            throw new IllegalArgumentException("Missing attribute [" + name
                    + "] in tag [" + el.getName() + "]");
        }
        return val.trim();
    }

    public static String getAttr(Element el, String name, String defval) {
        return el.getAttributeValue(name, defval);
    }

    public static boolean parseBoolean(String val, boolean defval) {
        if (val==null)
            return defval;
        val = val.trim();
        if (val.length()==0)
            return defval;
        switch (val.charAt(0)) {
            case '1':
            case 't': case 'T':
            case 'y': case 'Y':
            case 'д': case 'Д':
                return true;
            default:
                return false;
        }
    }

    public static boolean getAttr(Element el, String name, boolean defval) {
        return parseBoolean(el.getAttributeValue(name), defval);
    }
}
