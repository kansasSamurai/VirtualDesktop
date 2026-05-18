package org.jwellman.swing.grid;

import javax.swing.JTable;
import javax.swing.table.TableModel;

/**
 * Utility class for migrating from JTable to SmartGrid.
 *
 * Converts a JTable or TableModel into a DefaultGridModel with zero data
 * duplication — the source data is read once and copied into GridRow objects.
 *
 * Column keys are derived from column names: lowercased with spaces replaced
 * by underscores.  Example: "First Name" → "first_name".
 *
 * Usage:
 *   DefaultGridModel model = JTableAdapter.toSmartGrid(myExistingJTable);
 *   SmartGrid grid = new SmartGrid(model);
 */
public class JTableAdapter {

    private JTableAdapter() {}

    /**
     * Full migration adapter.  Reads column widths from the live JTable's
     * ColumnModel so the SmartGrid mirrors the JTable's column sizing exactly.
     * This is the recommended entry point when migrating existing JTable code.
     */
    public static DefaultGridModel toSmartGrid(JTable source) {
        TableModel tm = source.getModel();
        DefaultGridModel model = new DefaultGridModel();
        String[] keys = new String[tm.getColumnCount()];

        for (int col = 0; col < tm.getColumnCount(); col++) {
            String name = tm.getColumnName(col);
            String key  = name.toLowerCase().replace(' ', '_');
            keys[col]   = key;
            int width   = source.getColumnModel().getColumn(col).getPreferredWidth();
            model.addColumn(new ColumnDef(key, name, width, true, true, null));
        }
        for (int row = 0; row < tm.getRowCount(); row++) {
            GridRow gridRow = new GridRow();
            for (int col = 0; col < tm.getColumnCount(); col++) {
                gridRow.put(keys[col], tm.getValueAt(row, col));
            }
            model.addRow(gridRow);
        }
        return model;
    }

    /**
     * Data-only adapter.  Converts a bare TableModel using a uniform column
     * width.  Useful when the JTable is not available (e.g., adapting a
     * shared model used across multiple views).
     */
    public static DefaultGridModel toSmartGrid(TableModel source, int defaultColWidth) {
        DefaultGridModel model = new DefaultGridModel();
        String[] keys = new String[source.getColumnCount()];

        for (int col = 0; col < source.getColumnCount(); col++) {
            String name = source.getColumnName(col);
            String key  = name.toLowerCase().replace(' ', '_');
            keys[col]   = key;
            model.addColumn(new ColumnDef(key, name, defaultColWidth, true, true, null));
        }
        for (int row = 0; row < source.getRowCount(); row++) {
            GridRow gridRow = new GridRow();
            for (int col = 0; col < source.getColumnCount(); col++) {
                gridRow.put(keys[col], source.getValueAt(row, col));
            }
            model.addRow(gridRow);
        }
        return model;
    }
}
