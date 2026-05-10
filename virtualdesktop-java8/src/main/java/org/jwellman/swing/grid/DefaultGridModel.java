package org.jwellman.swing.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple list-backed GridModel. Load all rows before attaching a SmartGrid,
 * or call {@link #notifyDataChanged()} after bulk mutations.
 *
 * Supports tree/hierarchy via depth-based visible-row filtering, and optional
 * client-side sorting applied after the tree filter step.
 */
public class DefaultGridModel implements GridModel {

    private final List<GridRow> rows = new ArrayList<>();
    private final List<ColumnDef> columns = new ArrayList<>();
    private final List<GridModelListener> listeners = new ArrayList<>();

    private final List<GridRow> visibleRows = new ArrayList<>();
    private boolean visibleRowsDirty = true;

    private String    sortKey   = null;
    private SortOrder sortOrder = SortOrder.NONE;

    // -------------------------------------------------------------------------
    // Builder API
    // -------------------------------------------------------------------------

    public DefaultGridModel addColumn(ColumnDef col) {
        columns.add(col);
        return this;
    }

    public DefaultGridModel addColumn(String key, String header) {
        columns.add(new ColumnDef(key, header));
        return this;
    }

    /** Adds a row without firing events — efficient for bulk loading. */
    public DefaultGridModel addRow(GridRow row) {
        rows.add(row);
        visibleRowsDirty = true;
        return this;
    }

    /** Adds multiple rows without firing events. */
    public DefaultGridModel addRows(List<GridRow> newRows) {
        rows.addAll(newRows);
        visibleRowsDirty = true;
        return this;
    }

    /**
     * Recomputes the visible-row projection (tree filter + sort) then fires
     * a model-reset event to all registered listeners.
     */
    public DefaultGridModel notifyDataChanged() {
        computeVisibleRows();
        for (GridModelListener l : listeners) {
            l.modelReset();
        }
        return this;
    }

    // -------------------------------------------------------------------------
    // Sorting
    // -------------------------------------------------------------------------

    /**
     * Sets the active sort column and direction, then fires a model-reset.
     * Pass {@code SortOrder.NONE} (or {@code null} key) to restore insertion order.
     */
    public void sort(String key, SortOrder order) {
        this.sortKey   = key;
        this.sortOrder = (order != null) ? order : SortOrder.NONE;
        notifyDataChanged();
    }

    public String    getSortKey()   { return sortKey; }
    public SortOrder getSortOrder() { return sortOrder; }

    // -------------------------------------------------------------------------
    // GridModel
    // -------------------------------------------------------------------------

    @Override
    public int getRowCount() {
        ensureVisible();
        return visibleRows.size();
    }

    @Override
    public GridRow getRow(int index) {
        ensureVisible();
        return visibleRows.get(index);
    }

    @Override public List<ColumnDef> getColumns() { return java.util.Collections.unmodifiableList(columns); }
    @Override public void addGridModelListener(GridModelListener l) { listeners.add(l); }
    @Override public void removeGridModelListener(GridModelListener l) { listeners.remove(l); }

    // -------------------------------------------------------------------------
    // Visible-row computation (tree filter + sort)
    // -------------------------------------------------------------------------

    private void ensureVisible() {
        if (visibleRowsDirty) computeVisibleRows();
    }

    private void computeVisibleRows() {
        visibleRows.clear();

        // Step 1: tree filter (depth/expand logic)
        int hiddenDepth = Integer.MAX_VALUE;
        for (GridRow row : rows) {
            if (row.getDepth() > hiddenDepth) continue;
            visibleRows.add(row);
            if (row.isHasChildren() && !row.isExpanded()) {
                hiddenDepth = row.getDepth();
            } else {
                hiddenDepth = Integer.MAX_VALUE;
            }
        }

        // Step 2: sort (applies to flat data; tree structure is not preserved after sort)
        if (sortKey != null && sortOrder != SortOrder.NONE) {
            final String key  = sortKey;
            final boolean desc = sortOrder == SortOrder.DESCENDING;
            Collections.sort(visibleRows, (a, b) -> {
                int cmp = compareValues(a.get(key), b.get(key));
                return desc ? -cmp : cmp;
            });
        }

        visibleRowsDirty = false;
    }

    /**
     * Compares two cell values. Tries numeric comparison after stripping
     * formatting characters; falls back to case-insensitive string compare.
     */
    private static int compareValues(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return  1;
        try {
            double da = Double.parseDouble(a.toString().replaceAll("[^0-9.\\-]", ""));
            double db = Double.parseDouble(b.toString().replaceAll("[^0-9.\\-]", ""));
            return Double.compare(da, db);
        } catch (NumberFormatException e) {
            return a.toString().compareToIgnoreCase(b.toString());
        }
    }
}
