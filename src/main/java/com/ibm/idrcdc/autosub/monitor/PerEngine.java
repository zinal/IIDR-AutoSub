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

import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import com.ibm.idrcdc.autosub.config.*;

/**
 * Per-engine monitoring data.
 * @author zinal
 */
public class PerEngine {

    private final AsEngine engine;
    private boolean enabled;

    private String engineVersion = null;
    private String engineType = null;
    private boolean ddlAware;

    private String pathSeparator = null;
    private String cmdVersionCache = null;
    private String cmdEventsCache = null;
    private String cmdClearCache = null;
    private String cmdBookmarkGetCache = null;
    private String cmdBookmarkPutCache = null;
    private String cmdReAddTableCache = null;

    public PerEngine(AsEngine engine) {
        this.engine = engine;
        this.enabled = false;
        this.ddlAware = false;
    }

    public AsEngine getEngine() {
        return engine;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return engine.getName();
    }

    public EngineType getType() {
        return engine.getType();
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }

    public String getEngineType() {
        return engineType;
    }

    public void setEngineType(String engineType) {
        this.engineType = engineType;
        this.ddlAware = ( "Oracle".equalsIgnoreCase(engineType)
                || "IBM DB2".equalsIgnoreCase(engineType) );
    }

    public boolean isDdlAware() {
        return ddlAware;
    }

    private StringBuilder startCommand(String command) {
        return new StringBuilder()
                .append(engine.getRemoteExec()).append(' ')
                .append(engine.getEngineInstallDir())
                .append(getPathSeparator())
                .append("bin")
                .append(getPathSeparator())
                .append(command);
    }

    private StringBuilder startCommandInstance(String command) {
        return startCommand(command)
                .append(" -I \"")
                .append(engine.getInstanceName())
                .append("\"");
    }

    private String getPathSeparator() {
        if (pathSeparator == null) {
            String sample = engine.getEngineInstallDir();
            if (StringUtils.isBlank(sample)) {
                sample = engine.getCommandVersion();
            }
            if (sample.startsWith("/")
                    || (StringUtils.countMatches(sample, '/') >
                            StringUtils.countMatches(sample, '\\')) )
                pathSeparator = "/";
            else
                pathSeparator = "\\";
        }
        return pathSeparator;
    }

    public String cmdVersion() {
        String command = cmdVersionCache;
        if (!StringUtils.isBlank(command)) {
            return command;
        }
        command = engine.getCommandVersion();
        if (StringUtils.isBlank(command)) {
            command = startCommand("dmshowversion").toString();
        }
        cmdVersionCache = command;
        return command;
    }

    public String cmdEvents() {
        String command = cmdEventsCache;
        if (!StringUtils.isBlank(command)) {
            return command;
        }
        command = engine.getCommandEvents();
        if (StringUtils.isBlank(command)) {
            command = startCommandInstance("dmshowevents")
                    .append(" -a -c 2")
                    .toString();
        }
        cmdEventsCache = command;
        return command;
    }

    public String cmdClear() {
        String command = cmdClearCache;
        if (!StringUtils.isBlank(command)) {
            return command;
        }
        command = engine.getCommandClear();
        if (StringUtils.isBlank(command)) {
            command = startCommandInstance("dmclearstagingstore")
                    .toString();
        }
        cmdClearCache = command;
        return command;
    }

    public String cmdBookmarkGet() {
        String command = cmdBookmarkGetCache;
        if (!StringUtils.isBlank(command)) {
            return command;
        }
        command = engine.getCommandBookmarkGet();
        if (StringUtils.isBlank(command)) {
            command = startCommandInstance("dmshowbookmark")
                    .append(" -s \"${SUB}\"")
                    .toString();
        }
        cmdBookmarkGetCache = command;
        return command;
    }

    public String cmdBookmarkPut() {
        String command = cmdBookmarkPutCache;
        if (!StringUtils.isBlank(command)) {
            return command;
        }
        command = engine.getCommandBookmarkPut();
        if (StringUtils.isBlank(command)) {
            command = startCommandInstance("dmsetbookmark")
                    .append(" -s \"${SUB}\" -a -b ${BOOKMARK}")
                    .toString();
        }
        cmdBookmarkPutCache = command;
        return command;
    }

    public String cmdReAddTable() {
        String command = cmdReAddTableCache;
        if (!StringUtils.isBlank(command)) {
            return command;
        }
        command = engine.getCommandReAddTable();
        if (StringUtils.isBlank(command)) {
            command = startCommandInstance("dmreaddtable")
                    .append(" -t \"${TABLE}\" -a")
                    .toString();
        }
        cmdReAddTableCache = command;
        return command;
    }

    @Override
    public String toString() {
        return engine.getName();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.engine);
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
        final PerEngine other = (PerEngine) obj;
        if (!Objects.equals(this.engine, other.engine)) {
            return false;
        }
        return true;
    }

}
