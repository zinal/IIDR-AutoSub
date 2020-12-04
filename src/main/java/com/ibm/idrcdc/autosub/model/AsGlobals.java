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

import java.util.Properties;

/**
 * Global configuration properties for the application.
 * @author zinal
 */
public class AsGlobals {

    private String accessServerAddress = null;
    private int accessServerPort = -1;
    private String accessServerLogin = null;
    private String accessServerPassword = null;
    private String configDirectory = null;
    private String dataFile = null;
    private long mainPeriod = -1L;
    private int waitStartMirroring = -1;
    private long pauseAfterError = -1L;
    private long pauseBeforeRepair = -1L;

    /**
     * Constructor for the manual setup.
     */
    public AsGlobals() {
    }

    /**
     * Read properties from the input Properties object.
     * @param props
     */
    public AsGlobals(Properties props) {
        this.accessServerAddress = props.getProperty("as.host", "localhost");
        this.accessServerPort = Integer.parseInt(props.getProperty("as.port", "10101"));
        this.accessServerLogin = props.getProperty("as.user", "admin");
        this.accessServerPassword = props.getProperty("as.password", "password");
        this.configDirectory = props.getProperty("subs.config", "subs-config");
        this.dataFile = props.getProperty("subs.work", "subs-work.xml");
        this.mainPeriod = Long.parseLong(props.getProperty("tool.period", "10000"));
        this.waitStartMirroring =
                Integer.parseInt(props.getProperty("tool.wait_start_mirroring", "30"));
        this.pauseAfterError =
                Long.parseLong(props.getProperty("tool.pause_after_error", "30000"));
        this.pauseBeforeRepair =
                Long.parseLong(props.getProperty("tool.pause_before_repair", "5000"));
    }

    public String getAccessServerAddress() {
        return accessServerAddress;
    }

    public void setAccessServerAddress(String accessServerAddress) {
        this.accessServerAddress = accessServerAddress;
    }

    public int getAccessServerPort() {
        return accessServerPort;
    }

    public void setAccessServerPort(int accessServerPort) {
        this.accessServerPort = accessServerPort;
    }

    public String getAccessServerLogin() {
        return accessServerLogin;
    }

    public void setAccessServerLogin(String accessServerLogin) {
        this.accessServerLogin = accessServerLogin;
    }

    public String getAccessServerPassword() {
        return accessServerPassword;
    }

    public void setAccessServerPassword(String accessServerPassword) {
        this.accessServerPassword = accessServerPassword;
    }

    public String getConfigDirectory() {
        return configDirectory;
    }

    public void setConfigDirectory(String configDirectory) {
        this.configDirectory = configDirectory;
    }

    public String getDataFile() {
        return dataFile;
    }

    public void setDataFile(String dataFile) {
        this.dataFile = dataFile;
    }

    public long getMainPeriod() {
        return mainPeriod;
    }

    public void setMainPeriod(long mainSleep) {
        this.mainPeriod = mainSleep;
    }

    public long getPauseAfterError() {
        return pauseAfterError;
    }

    public void setPauseAfterError(long pauseAfterError) {
        this.pauseAfterError = pauseAfterError;
    }

    public int getWaitStartMirroring() {
        return waitStartMirroring;
    }

    public void setWaitStartMirroring(int waitStartMirroring) {
        this.waitStartMirroring = waitStartMirroring;
    }

    public long getPauseBeforeRepair() {
        return pauseBeforeRepair;
    }

    public void setPauseBeforeRepair(long pauseBeforeRepair) {
        this.pauseBeforeRepair = pauseBeforeRepair;
    }

}
