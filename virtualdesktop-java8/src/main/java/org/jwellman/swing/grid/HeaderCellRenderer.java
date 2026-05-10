package org.jwellman.swing.grid;

import javax.swing.JComponent;

/** Renders a single cell in the SmartGrid column header row. */
public interface HeaderCellRenderer {
    JComponent render(ColumnDef col);
}
