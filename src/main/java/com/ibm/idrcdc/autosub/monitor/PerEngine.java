/*
 * AutoSub sample code
 */
package com.ibm.idrcdc.autosub.monitor;

import java.util.Objects;
import com.ibm.idrcdc.autosub.config.*;

/**
 *
 * @author zinal
 */
public class PerEngine {

    private final AsEngine engine;

    public PerEngine(AsEngine engine) {
        this.engine = engine;
    }

    public AsEngine getEngine() {
        return engine;
    }

    public String getName() {
        return engine.getName();
    }

    public EngineType getType() {
        return engine.getType();
    }

    public String cmdVersion() {
        return engine.cmdVersion();
    }

    public String cmdEvents() {
        return engine.cmdEvents();
    }

    public String cmdClear() {
        return engine.cmdClear();
    }

    public String cmdBookmarkGet() {
        return engine.cmdBookmarkGet();
    }

    public String cmdBookmarkPut() {
        return engine.cmdBookmarkPut();
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
