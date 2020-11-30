/*
** ****************************************************************************
** (c) Copyright IBM Corp. 2019 All rights reserved.
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

import java.util.Properties;

/**
 * Global configuration properties for the application
 * @author zinal
 */
public class Config {
    
    private String accessServerAddress = null;
    private int accessServerPort = -1;
    private String accessServerLogin = null;
    private String accessServerPassword = null;
    private String subscriptionDirectory = null;
    private long mainSleep = -1L;
    private long pauseAfterError = -1L;
    private int waitStartMirroring = -1;
    private boolean stagingClearEnable = false;
    private String stagingClearTool = null;

    /**
     * Read properties from the input Properties object
     * @param props Input
     */
    public Config(Properties props) {
        this.accessServerAddress = props.getProperty("as.host", "localhost");
        this.accessServerPort = Integer.parseInt(props.getProperty("as.port", "10101"));
        this.accessServerLogin = props.getProperty("as.user", "admin");
        this.accessServerPassword = props.getProperty("as.password", "password");
        this.subscriptionDirectory = props.getProperty("subs.dir", "subs");
        this.mainSleep = Long.parseLong(props.getProperty("tool.sleep", "1000"));
        this.pauseAfterError = 
                Long.parseLong(props.getProperty("tool.pause_after_error", "30000"));
        this.waitStartMirroring = 
                Integer.parseInt(props.getProperty("tool.wait_start_mirroring", "30"));
        this.stagingClearEnable =
                Misc.parseBoolean(props.getProperty("tool.staging_clear.enable"), false);
        this.stagingClearTool = props.getProperty("tool.staging_clear.command");
        if (this.stagingClearTool==null)
            this.stagingClearEnable = false;
    }

    public String getAccessServerAddress() {
        return accessServerAddress;
    }

    public int getAccessServerPort() {
        return accessServerPort;
    }

    public String getAccessServerLogin() {
        return accessServerLogin;
    }

    public String getAccessServerPassword() {
        return accessServerPassword;
    }

    public String getSubscriptionDirectory() {
        return subscriptionDirectory;
    }

    public long getMainSleep() {
        return mainSleep;
    }

    public long getPauseAfterError() {
        return pauseAfterError;
    }

    public int getWaitStartMirroring() {
        return waitStartMirroring;
    }

    public boolean isStagingClearEnable() {
        return stagingClearEnable;
    }

    public String getStagingClearTool() {
        return stagingClearTool;
    }
    
}
