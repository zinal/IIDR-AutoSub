/*
** ****************************************************************************
** (c) Copyright IBM Corp. 2019, 2020. All rights reserved.
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
package com.ibm.idrcdc.autosub.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.text.StringTokenizer;

/**
 * Remote tool execution.
 * Used to run dmclearstagingstore, dmgetbookmark, dmsetbookmark.
 * @author zinal
 */
public class RemoteTool {

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(RemoteTool.class);

    private static final String CATEGORY_COMMAND = "shell-command";
    private static final String CATEGORY_OUTPUT = "shell-output";
    private static final String CATEGORY_STATUS = "shell-status";

    private final String logPrefix;
    private final String commandText;
    private final Map<String,String> substitutions;

    public RemoteTool(String logPrefix, String commandText, Map<String, String> substitutions) {
        this.logPrefix = logPrefix;
        this.commandText = commandText;
        this.substitutions = (substitutions == null) ? Collections.emptyMap() : substitutions;
    }

    public RemoteTool(String logPrefix, String commandText) {
        this.logPrefix = logPrefix;
        this.commandText = commandText;
        this.substitutions = Collections.emptyMap();
    }

    public int execute() {
        return execute(null);
    }

    public int execute(StringBuilder output) {
        final String[] command = new StringTokenizer(commandText)
                .setQuoteChar('"').getTokenArray();
        if (substitutions.isEmpty() == false) {
            for (int i=0; i<command.length; ++i) {
                String s = command[i];
                if (!s.contains("${"))
                    continue;
                for (Map.Entry<String,String> me : substitutions.entrySet()) {
                    String e = me.getKey();
                    if (e.startsWith("${") == false)
                        e = "${" + e + "}";
                    s = s.replace(e, me.getValue());
                }
                command[i] = s;
            }
        }
        LOG.debug("Running the command {}", (Object) command);
        RecoveryReport.logIf(CATEGORY_COMMAND, Arrays.toString(command));
        try {
            final Process proc = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader br =
                    new BufferedReader(
                            new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = br.readLine())!=null) {
                    if (output!=null) {
                        output.append(line).append("\n");
                    }
                    LOG.debug("{}: {}", logPrefix, line);
                    RecoveryReport.logIf(CATEGORY_OUTPUT, line);
                }
            }
            int retCode = proc.waitFor();
            LOG.debug("Command completed with code {}", retCode);
            RecoveryReport.logIf(CATEGORY_STATUS, String.valueOf(retCode));
            return retCode;
        } catch(Exception ex) {
            LOG.error("Execution failed for command {}", (Object) command, ex);
            return -1;
        }
    }

    public static int run(String logPrefix, String commandText,
            Map<String, String> substitutions, StringBuilder output) {
        return new RemoteTool(logPrefix, commandText, substitutions) . execute(output);
    }

    public static int run(String logPrefix, String commandText,
            Map<String, String> substitutions) {
        return new RemoteTool(logPrefix, commandText, substitutions) . execute(null);
    }

    public static int run(String logPrefix, String commandText, StringBuilder output) {
        return new RemoteTool(logPrefix, commandText, Collections.emptyMap()) . execute(output);
    }

    public static int run(String logPrefix, String commandText) {
        return new RemoteTool(logPrefix, commandText, Collections.emptyMap()) . execute(null);
    }

}
