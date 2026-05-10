package org.jwellman.swing.grid;

import javax.swing.JComponent;

/** Renders a single cell in the SmartGrid column header row. */
public interface HeaderCellRenderer {
    /**
     * @param col       the column to render
     * @param sortOrder the current sort state for this column (NONE if unsorted)
     */
    JComponent render(ColumnDef col, SortOrder sortOrder);
}
