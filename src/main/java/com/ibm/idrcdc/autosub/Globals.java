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
package com.ibm.idrcdc.autosub;

import com.ibm.idrcdc.autosub.config.Monitor;
import com.ibm.idrcdc.autosub.config.Config;
import com.ibm.replication.cdc.scripting.EmbeddedScriptException;
import java.util.List;

/**
 * Global application context, including all configuration settings
 * and processing status flags.
 * @author zinal
 */
public class Globals {
    
    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(Globals.class);
    
    private final Config config;
    private List<Monitor> monitors = null;

    private boolean suppressConnectErrorMessage = false;

    public Globals(Config config) {
        this.config = config;
    }

    public Config getConfig() {
        return config;
    }

    public List<Monitor> getMonitors() {
        return monitors;
    }

    public void setMonitors(List<Monitor> monitors) {
        this.monitors = monitors;
    }

    /**
     * Create the embedded scriptiong object with the Access Server connection.
     * @return Script object, or null if connection has failed.
     */
    public Script openScript() {
        try {
            final Script retval = new Script(config);
            if (suppressConnectErrorMessage) {
                suppressConnectErrorMessage = false;
                LOG.info("Re-established connection to the Access Server");
            }
            return retval;
        } catch(Exception ex) {
            if (!suppressConnectErrorMessage) {
                suppressConnectErrorMessage = true;
                final String TEXT = "Failed to connect to the Access Server";
                if (ex instanceof EmbeddedScriptException) {
                    LOG.warn("{}.\n{}", TEXT,
                            ((EmbeddedScriptException)ex).getResultCodeAndMessage(),
                            ex);
                } else {
                    LOG.warn("{}.", TEXT, ex);
                }
            }
            return null;
        }
    }

}
