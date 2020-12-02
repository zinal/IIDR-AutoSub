/*
 * AutoSub sample code
 */
package com.ibm.idrcdc.autosub;

import com.ibm.replication.cdc.scripting.ResultStringTable;

/**
 * Wrapper implementation around ResultStringTable to allow mock tests.
 * @author zinal
 */
public class ScriptTableImpl implements ScriptOutput {

    private final ResultStringTable table;

    public ScriptTableImpl(ResultStringTable table) {
        this.table = table;
    }

    @Override
    public int getColumnCount() {
        return table.getColumnCount();
    }

    @Override
    public int getRowCount() {
        return table.getRowCount();
    }

    @Override
    public String getValueAt(int index, int colindex) {
        return table.getValueAt(index, colindex);
    }

    @Override
    public String getValueAt(int index, String name) {
        return table.getValueAt(index, name);
    }

    @Override
    public String toString() {
        return table.toString();
    }

    @Override
    public String getColumnAt(int colindex) {
        return table.getColumnAt(colindex);
    }

}
