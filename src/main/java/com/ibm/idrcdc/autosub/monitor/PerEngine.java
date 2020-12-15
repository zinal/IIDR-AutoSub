/*
 * AutoSub sample code
 */
package com.ibm.idrcdc.autosub.monitor;

import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import com.ibm.idrcdc.autosub.config.*;

/**
 *
 * @author zinal
 */
public class PerEngine {

    private final AsEngine engine;
    private boolean enabled;

    private String cmdVersionCache = null;
    private String cmdEventsCache = null;
    private String cmdClearCache = null;
    private String cmdBookmarkGetCache = null;
    private String cmdBookmarkPutCache = null;
    private String cmdReAddTableCache = null;
    private String cmdDescribeCache = null;
    private String cmdReAssignTableCache = null;

    public PerEngine(AsEngine engine) {
        this.engine = engine;
        this.enabled = false;
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

    private StringBuilder startCommand() {
        return new StringBuilder()
                    .append(engine.getRemoteExec()).append(' ')
                    .append(engine.getEngineInstallDir());
    }

    private StringBuilder startCommandInstance(String command) {
        return startCommand()
                .append(command)
                .append(" -I ")
                .append(engine.getInstanceName());
    }

    public String cmdVersion() {
        String command = cmdVersionCache;
        if (!StringUtils.isBlank(command)) {
            return command;
        }
        command = engine.getCommandVersion();
        if (StringUtils.isBlank(command)) {
            command = startCommand()
                    .append("/bin/dmshowversion")
                    .toString();
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
            command = startCommandInstance("/bin/dmshowevents")
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
            command = startCommandInstance("/bin/dmclearstagingstore")
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
            command = startCommandInstance("/bin/dmshowbookmark")
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
            command = startCommandInstance("/bin/dmsetbookmark")
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
            command = startCommandInstance("/bin/dmreaddtable")
                    .append(" -t \"${TABLE}\"")
                    .toString();
        }
        cmdReAddTableCache = command;
        return command;
    }

    public String cmdDescribe() {
        String command = cmdDescribeCache;
        if (!StringUtils.isBlank(command)) {
            return command;
        }
        command = engine.getCommandDescribe();
        if (StringUtils.isBlank(command)) {
            command = startCommandInstance("/bin/dmdescribe")
                    .append(" -s \"${SUB}\"")
                    .toString();
        }
        cmdDescribeCache = command;
        return command;
    }

    public String cmdReAssignTable() {
        String command = cmdReAssignTableCache;
        if (!StringUtils.isBlank(command)) {
            return command;
        }
        command = engine.getCommandReAssignTable();
        if (StringUtils.isBlank(command)) {
            command = startCommandInstance("/bin/dmreassigntable")
                    .append(" -s \"${SUB}\" -t \"${TABLE}\"")
                    .toString();
        }
        cmdReAssignTableCache = command;
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
