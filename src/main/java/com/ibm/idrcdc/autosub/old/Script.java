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
package com.ibm.idrcdc.autosub.old;

import com.ibm.idrcdc.autosub.config.Config;
import com.ibm.replication.cdc.scripting.EmbeddedScript;
import com.ibm.replication.cdc.scripting.EmbeddedScriptException;
import com.ibm.replication.cdc.scripting.Result;
import com.ibm.replication.cdc.scripting.ResultStringTable;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

/**
 * Supporting helper class to execute commands through embedded
 * CDC scripting interface.
 * @author zinal
 */
public class Script implements AutoCloseable {
    
    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(Script.class);
    
    public static final int ST_SOURCE = 1;
    public static final int ST_TARGET = 2;
    public static final int ST_BOTH = 3;
    
    private final EmbeddedScript es;
    
    private final Set<String> connections = new HashSet<>();
    private String currentSource = null;
    private String currentTarget = null;
    
    /**
     * Create new Access Server connection through the embedded
     * scripting interface.
     * @param config Configuration options
     * @throws Exception 
     */
    public Script(Config config) throws Exception {
        EmbeddedScript temp = new EmbeddedScript();
        try {
            temp.open();
            String cmd = MessageFormat.format("connect server "
                    + "hostname {0} port {1} "
                    + "username \"{2}\" password \"{3}\";",
                    config.getAccessServerAddress(),
                    String.valueOf(config.getAccessServerPort()),
                    config.getAccessServerLogin(),
                    config.getAccessServerPassword());
            temp.execute(cmd);
            int code = temp.getResultCode();
            if ( code != 0  && code != 1101 ) {
                throw new Exception("Cannot connect: " 
                        + temp.getResultCodeAndMessage());
            }
            this.es = temp;
            temp = null;
        } finally {
            if (temp!=null)
                temp.close();
        }
    }

    /**
     * @return Name of current source datastore
     */
    public String getCurrentSource() {
        return currentSource;
    }

    /**
     * @return Name of current target datastore
     */
    public String getCurrentTarget() {
        return currentTarget;
    }

    /**
     * Connect or select a data store.
     * @param name Data store name
     * @param mode ST_SOURCE, ST_TARGET or ST_BOTH
     */
    public void dataStore(String name, int mode) {
        name = name.trim().toUpperCase();
        switch (mode) {
            case ST_SOURCE:
                if (connections.contains(name)) {
                    if (name.equalsIgnoreCase(currentTarget)) {
                        execute("select datastore name \"{0}\";", name);
                    } else {
                        execute("select datastore name \"{0}\" "
                                + "context source;", name);
                    }
                } else {
                     execute("connect datastore name \"{0}\" "
                             + "context source;", name);
                     connections.add(name);
                }
                currentSource = name;
                break;
            case ST_TARGET:
                if (connections.contains(name)) {
                    if (name.equalsIgnoreCase(currentSource)) {
                        execute("select datastore name \"{0}\";", name);
                    } else {
                        execute("select datastore name \"{0}\" "
                                + "context target;", name);
                    }
                } else {
                     execute("connect datastore name \"{0}\" "
                             + "context target;", name);
                     connections.add(name);
                }
                currentTarget = name;
                break;
            case ST_BOTH:
                if (connections.contains(name)) {
                    execute("select datastore name \"{0}\";", name);
                } else {
                     execute("connect datastore name \"{0}\";", name);
                     connections.add(name);
                }
                currentSource = name;
                currentTarget = name;
                break;
            default:
                throw new IllegalArgumentException("dataStore(mode=" 
                        + mode + ")");
        }
        if (LOG.isDebugEnabled()) {
            execute("show context;");
            getTable();
        }
    }

    /**
     * Execute a CHCCLP command
     * @param command Command text
     * @param args Formatting arguments, substituted through MessageFormat
     */
    public void execute(String command, Object ... args) {
        final String cmd = (args.length > 0) ? 
                MessageFormat.format(command, args) : command;
        try {
            LOG.debug("CHCCLP> {}", cmd);
            es.execute(cmd);
        } catch(EmbeddedScriptException ese) {
            String messageAndCode = ese.getResultCodeAndMessage();
            LOG.debug("CHCCLP ERROR: {}", messageAndCode);
            throw new RuntimeException(messageAndCode, ese);
        }
    }
    
    public int getCode() {
        return es.getResultCode();
    }
    
    public String getMessage() {
        return es.getResultCodeAndMessage();
    }
    
    public Result getResult() {
        return es.getResult();
    }
    
    /**
     * @return Command execution result as a table
     */
    public ResultStringTable getTable() {
        Result res = es.getResult();
        if (res instanceof ResultStringTable) {
            ResultStringTable table = (ResultStringTable) res;
            if (LOG.isDebugEnabled())
                printTable(table);
            return table;
        }
        if (res==null)
            throw new IllegalStateException("Missing result object");
        throw new IllegalStateException("Incorrect type of result object: "
                + res.getClass().getName());
    }
    
    @Override
    public void close() {
        es.close();
    }

    private void printTable(ResultStringTable table) {
        StringBuilder sb = new StringBuilder();
        for (int icolumn = 0; icolumn < table.getColumnCount(); ++icolumn) {
            sb.append("\t").
                    append('[').
                    append(table.getColumnAt(icolumn)).
                    append(']');
        }
        LOG.debug("CHCOUT>{}", sb);
        for (int irow = 0; irow < table.getRowCount(); ++irow) {
            sb = new StringBuilder();
            for (int icolumn = 0; icolumn < table.getColumnCount(); ++icolumn) {
                sb.append("\t").append(table.getValueAt(irow, icolumn));
            }
            LOG.debug("CHCOUT>{}", sb);
        }
    }
    
}