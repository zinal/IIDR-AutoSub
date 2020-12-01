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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * File-based flags for autosub application.
 * @author zinal
 */
public class FileFlag {

    private final File file;

    public FileFlag(File file) {
        this.file = file;
    }

    public FileFlag(String file) {
        this.file = new File(file);
    }

    public boolean isEnabled() {
        return file.isFile();
    }

    public void enable() {
        try {
            if (! file.exists())
                file.createNewFile();
        } catch(IOException ix) {
            throw new RuntimeException("Cannot create file " + file, ix);
        }
    }

    public void disable() {
        if (file.exists()) {
            try {
                Files.delete(file.toPath());
            } catch(IOException ix) {
                throw new RuntimeException("Cannot remove file " + file, ix);
            }
        }
    }

    public static FileFlag newShutdown(String base) {
        return new FileFlag(base + ".SHUTDOWN");
    }

    public static FileFlag newReload(String base) {
        return new FileFlag(base + ".RELOAD");
    }

}
