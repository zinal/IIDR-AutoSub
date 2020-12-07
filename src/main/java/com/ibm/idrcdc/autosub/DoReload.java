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

import com.ibm.idrcdc.autosub.model.*;
import java.io.File;

/**
 * Initiate the configuration reload of the Autosub service.
 * @author zinal
 */
public class DoReload {

    private static final org.slf4j.Logger LOG
            = org.slf4j.LoggerFactory.getLogger(DoReload.class);

    public static void main(String[] args) {
        LOG.info("autosub version {} DoReload", Worker.VERSION);
        try {
            final AsGlobals globals = AsGlobals.fromArgs(args);
            LOG.info("Working data file is {}", globals.getDataFile());
            final AsConfig config = AsConfig.loadDir(globals);
            // TODO: check the configuration for validity
            final File sourceFile = new File(globals.getDataFile() + ".tmp");
            final File targetFile = new File(globals.getDataFile());
            AsConfig.save(config, sourceFile);
            targetFile.delete();
            sourceFile.renameTo(targetFile);
            LOG.info("Configuration merged.");
            FileFlag.newReload(globals.getDataFile()) . enable();
            LOG.info("Reload signaled.");
        } catch(Exception ex) {
            LOG.error("Command execution failed", ex);
            System.exit(1);
        }
    }

}
