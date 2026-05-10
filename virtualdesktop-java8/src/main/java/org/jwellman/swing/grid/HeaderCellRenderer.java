package org.jwellman.swing.grid;

import javax.swing.JComponent;

/** Renders a single cell in the SmartGrid column header row. */
public interface HeaderCellRenderer {

    /**
     * Renders the header cell.
     *
     * @param col       the column to render
     * @param sortOrder the current sort direction for this column ({@code NONE} if unsorted)
     * @param sortRank  1-based position in a multi-column sort; 0 if unsorted or if only
     *                  one column is sorted (rank not shown for single-column sort)
     */
    JComponent render(ColumnDef col, SortOrder sortOrder, int sortRank);

    /**
     * Backward-compatible default for callers that do not supply a sort rank.
     * Delegates to {@link #render(ColumnDef, SortOrder, int)} with rank 0.
     */
    default JComponent render(ColumnDef col, SortOrder sortOrder) {
        return render(col, sortOrder, 0);
    }
}
