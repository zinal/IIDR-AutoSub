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
package com.ibm.idrcdc.autosub.impl;

import com.ibm.idrcdc.autosub.config.*;
import com.ibm.idrcdc.autosub.monitor.*;
import java.io.File;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Recovery actions logging logic.
 * @author zinal
 */
public class RecoveryReport {
    
    private static final org.slf4j.Logger LOG
            = org.slf4j.LoggerFactory.getLogger(RecoveryReport.class);
    
    private static final ThreadLocal<RecoveryReport> RR = new ThreadLocal<>();
    
    private final PerSource origin;
    private final PrintStream out;
    private final SimpleDateFormat sdf;
    
    public static boolean isEnabled() {
        return RR.get() != null;
    }
    
    public static void enable(AsGlobals globals, PerSource origin) {
        if (RR.get() == null)
            RR.set(new RecoveryReport(globals, origin));
    }

    public static void disable() {
        RecoveryReport rr = RR.get();
        if (rr != null) {
            rr.close();
            RR.remove();
        }
    }

    public static void logIf(String category, String action) {
        RecoveryReport rr = RR.get();
        if (rr!=null)
            rr.log(category, action);
    }

    private RecoveryReport(AsGlobals globals, PerSource origin) {
        final StringBuilder fileName = new StringBuilder();
        fileName.append("recovery_");
        fileName.append(new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
                .format(new Date()));
        fileName.append("_");
        fileName.append(origin.getSource().getName());
        fileName.append(".txt");
        File f = new File(new File(globals.getRecoveryDirectory()), 
                fileName.toString());
        PrintStream out = null;
        try {
            out = new PrintStream(f, "UTF-8");
            LOG.info("Opened recovery report file {}", f);
        } catch(Exception ex) {
            LOG.error("Failed to open recovery report file {}", f, ex);
        }
        this.origin = origin;
        this.out = out;
        this.sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
    }

    private void close() {
        if (out!=null) {
            try {
                out.close();
            } catch(Exception ex) {
                LOG.error("Failed to close the recovery report file", ex);
            }
        }
    }

    private void log(String category, String action) {
        final StringBuilder sb = new StringBuilder(
                50 + category.length() + action.length());
        sb.append(sdf.format(new Date()))
                .append("\t").append(category)
                .append("\t").append(action);
        if (out!=null)
            out.println(sb.toString());
    }

}
