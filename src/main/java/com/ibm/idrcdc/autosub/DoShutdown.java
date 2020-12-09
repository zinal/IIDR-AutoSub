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

import com.ibm.idrcdc.autosub.config.*;
import com.ibm.idrcdc.autosub.impl.*;

/**
 * Initiate the shutdown of the Autosub service.
 * @author zinal
 */
public class DoShutdown {

    private static final org.slf4j.Logger LOG
            = org.slf4j.LoggerFactory.getLogger(DoShutdown.class);

    public static void main(String[] args) {
        LOG.info("autosub version {} DoShutdown", Worker.VERSION);
        try {
            final AsGlobals globals = AsGlobals.fromArgs(args);
            LOG.info("Working data file is {}", globals.getDataFile());
            FileFlag.newShutdown(globals.getDataFile()) . enable();
            LOG.info("Shutdown signaled.");
        } catch(Exception ex) {
            LOG.error("Command execution failed", ex);
            System.exit(1);
        }
    }

}
