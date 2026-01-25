package org.jwellman.scripttester;

import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * Table model for the script lines.
 *
 * @author Rick Wellman
 */
public class ScriptTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMN_NAMES = {"Status", "#", "Code"};

    private final List<ScriptLine> lines;

    public ScriptTableModel(List<ScriptLine> lines) {
        this.lines = lines;
    }

    @Override
    public int getRowCount() {
        return lines.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Object getValueAt(int row, int column) {
        ScriptLine line = lines.get(row);
        switch (column) {
            case 0: return line.getStatus();
            case 1: return Integer.valueOf(line.getLineNumber());
            case 2: return line.getText();
            default: return "";
        }
    }

    @Override
    public Class<?> getColumnClass(int column) {
        switch (column) {
            case 0: return String.class;
            case 1: return Integer.class;
            case 2: return String.class;
            default: return Object.class;
        }
    }

    /**
     * Notify listeners that the data has changed.
     */
    public void fireDataChanged() {
        fireTableDataChanged();
    }
}
