/*
 * AutoSub sample code
 */
package com.ibm.idrcdc.autosub;

import com.ibm.replication.cdc.scripting.ResultStringValue;

/**
 * Wrapper implementation around ResultStringValue to allow mock tests.
 * @author zinal
 */
public class ScriptValueImpl implements ScriptOutput {

    private final ResultStringValue value;

    public ScriptValueImpl(ResultStringValue value) {
        this.value = value;
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public int getRowCount() {
        return 1;
    }

    @Override
    public String getValueAt(int irow, int colindex) {
        return value.getValue();
    }

    @Override
    public String getValueAt(int index, String name) {
        return value.getValue();
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public String getColumnAt(int icolumn) {
        return "VALUE";
    }

}
