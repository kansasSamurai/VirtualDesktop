package org.jwellman.swing.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple list-backed GridModel. Load all rows before attaching a SmartGrid,
 * or call {@link #notifyDataChanged()} after bulk mutations.
 *
 * Supports tree/hierarchy via depth-based visible-row filtering. For flat
 * data (all depth=0, hasChildren=false) the filter is transparent.
 */
public class DefaultGridModel implements GridModel {

    private final List<GridRow> rows = new ArrayList<>();
    private final List<ColumnDef> columns = new ArrayList<>();
    private final List<GridModelListener> listeners = new ArrayList<>();

    private final List<GridRow> visibleRows = new ArrayList<>();
    private boolean visibleRowsDirty = true;

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
     * Recomputes the visible-row projection (respects tree expand/collapse state)
     * then fires a model-reset event to all registered listeners.
     */
    public DefaultGridModel notifyDataChanged() {
        computeVisibleRows();
        for (GridModelListener l : listeners) {
            l.modelReset();
        }
        return this;
    }

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

    @Override public List<ColumnDef> getColumns() { return Collections.unmodifiableList(columns); }
    @Override public void addGridModelListener(GridModelListener l) { listeners.add(l); }
    @Override public void removeGridModelListener(GridModelListener l) { listeners.remove(l); }

    // -------------------------------------------------------------------------
    // Visible-row computation (tree support)
    // -------------------------------------------------------------------------

    private void ensureVisible() {
        if (visibleRowsDirty) computeVisibleRows();
    }

    private void computeVisibleRows() {
        visibleRows.clear();
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
        visibleRowsDirty = false;
    }
}
