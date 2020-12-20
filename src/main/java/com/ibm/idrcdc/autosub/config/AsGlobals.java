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

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;

/**
 * Global configuration properties for the AutoSub application.
 * @author zinal
 */
public class AsGlobals {

    private String accessServerAddress;
    private int accessServerPort;
    private String accessServerLogin;
    private String accessServerPassword;
    private String configDirectory;
    private String recoveryDirectory;
    private String dataFile;
    private long mainPeriod;
    private int waitStartMirroring;
    private long pauseAfterError;
    private long pauseBeforeRepair;

    /**
     * Constructor for the manual setup.
     */
    public AsGlobals() {
        this.accessServerAddress = "localhost";
        this.accessServerPort = 10101;
        this.accessServerLogin = null;
        this.accessServerPassword = null;
        this.configDirectory = "subs-config";
        this.recoveryDirectory = "subs-recovery";
        this.dataFile = "subs-datafile.xml";
        this.mainPeriod = 10000L;
        this.waitStartMirroring = 30;
        this.pauseAfterError = 30000L;
        this.pauseBeforeRepair = 5000L;
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
        this.recoveryDirectory = props.getProperty("subs.config", "subs-recovery");
        this.dataFile = props.getProperty("subs.work", "subs-datafile.xml");
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

    public String getRecoveryDirectory() {
        return recoveryDirectory;
    }

    public void setRecoveryDirectory(String recoveryDirectory) {
        this.recoveryDirectory = recoveryDirectory;
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

    public static AsGlobals fromArgs(String[] args) throws Exception {
        String configFile = args.length == 0 ? null : args[0];
        if (StringUtils.isBlank(configFile))
            configFile = "autosub.properties";
        final Properties props = new Properties();
        try (InputStream is
                = new FileInputStream(configFile)) {
            props.load(is);
        }
        return new AsGlobals(props);
    }

}
