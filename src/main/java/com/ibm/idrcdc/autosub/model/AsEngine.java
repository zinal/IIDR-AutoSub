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

import java.util.Objects;

/**
 * Source or target engine configuration.
 * @author zinal
 */
public class AsEngine {

    private final String name;
    private final EngineType type;
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

}
