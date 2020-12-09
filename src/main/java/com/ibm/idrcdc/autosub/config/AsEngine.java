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

import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * Source or target engine configuration.
 * @author zinal
 */
public class AsEngine {

    private final String name;
    private final EngineType type;

    private String remoteExec;
    private String engineInstallDir;
    private String instanceName;

    private String commandVersion;
    private String commandEvents;
    private String commandClear;
    private String commandBookmarkGet;
    private String commandBookmarkPut;

    public AsEngine(String name, EngineType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public EngineType getType() {
        return type;
    }

    public String getRemoteExec() {
        return remoteExec;
    }

    public void setRemoteExec(String remoteExec) {
        this.remoteExec = remoteExec;
    }

    public String getEngineInstallDir() {
        return engineInstallDir;
    }

    public void setEngineInstallDir(String engineInstallDir) {
        this.engineInstallDir = engineInstallDir;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getCommandVersion() {
        return commandVersion;
    }

    public void setCommandVersion(String commandVersion) {
        this.commandVersion = commandVersion;
    }

    public String getCommandEvents() {
        return commandEvents;
    }

    public void setCommandEvents(String commandEvents) {
        this.commandEvents = commandEvents;
    }

    public String getCommandClear() {
        return commandClear;
    }

    public void setCommandClear(String commandClear) {
        this.commandClear = commandClear;
    }

    public String getCommandBookmarkGet() {
        return commandBookmarkGet;
    }

    public void setCommandBookmarkGet(String commandBookmarkGet) {
        this.commandBookmarkGet = commandBookmarkGet;
    }

    public String getCommandBookmarkPut() {
        return commandBookmarkPut;
    }

    public void setCommandBookmarkPut(String commandBookmarkPut) {
        this.commandBookmarkPut = commandBookmarkPut;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + Objects.hashCode(this.name);
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
        final AsEngine other = (AsEngine) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.remoteExec, other.remoteExec)) {
            return false;
        }
        if (!Objects.equals(this.engineInstallDir, other.engineInstallDir)) {
            return false;
        }
        if (!Objects.equals(this.instanceName, other.instanceName)) {
            return false;
        }
        if (!Objects.equals(this.commandVersion, other.commandVersion)) {
            return false;
        }
        if (!Objects.equals(this.commandEvents, other.commandEvents)) {
            return false;
        }
        if (!Objects.equals(this.commandClear, other.commandClear)) {
            return false;
        }
        if (!Objects.equals(this.commandBookmarkGet, other.commandBookmarkGet)) {
            return false;
        }
        if (!Objects.equals(this.commandBookmarkPut, other.commandBookmarkPut)) {
            return false;
        }
        if (this.type != other.type) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return name;
    }

    public String cmdVersion() {
        if (! StringUtils.isBlank(commandVersion))
            return commandVersion;
        final StringBuilder sb = new StringBuilder();
        sb.append(remoteExec).append(' ')
                .append(engineInstallDir)
                .append("/bin/dmshowversion");
        return sb.toString();
    }

    public String cmdEvents() {
        if (! StringUtils.isBlank(commandEvents))
            return commandEvents;
        final StringBuilder sb = new StringBuilder();
        sb.append(remoteExec).append(' ')
                .append(engineInstallDir)
                .append("/bin/dmshowevents")
                .append(" -I ")
                .append(instanceName)
                .append(" -a -c 2");
        return sb.toString();
    }

    public String cmdClear() {
        if (! StringUtils.isBlank(commandClear))
            return commandClear;
        final StringBuilder sb = new StringBuilder();
        sb.append(remoteExec).append(' ')
                .append(engineInstallDir)
                .append("/bin/dmclearstagingstore")
                .append(" -I ")
                .append(instanceName);
        return sb.toString();
    }

    public String cmdBookmarkGet() {
        if (! StringUtils.isBlank(commandBookmarkGet))
            return commandBookmarkGet;
        final StringBuilder sb = new StringBuilder();
        sb.append(remoteExec).append(' ')
                .append(engineInstallDir)
                .append("/bin/dmshowbookmark")
                .append(" -I ")
                .append(instanceName)
                .append(" -s ${SUB}");
        return sb.toString();
    }

    public String cmdBookmarkPut() {
        if (! StringUtils.isBlank(commandBookmarkPut))
            return commandBookmarkPut;
        final StringBuilder sb = new StringBuilder();
        sb.append(remoteExec).append(' ')
                .append(engineInstallDir)
                .append("/bin/dmsetbookmark")
                .append(" -I ")
                .append(instanceName)
                .append(" -s ${SUB} -a -b ${BOOKMARK}");
        return sb.toString();
    }

}
