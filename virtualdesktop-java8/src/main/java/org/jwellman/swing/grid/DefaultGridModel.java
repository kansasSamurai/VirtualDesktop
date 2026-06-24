package org.jwellman.swing.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Simple list-backed GridModel.
 *
 * {@link #computeVisibleRows()} runs three steps in order:
 *   1. Tree filter  — respects depth / expand / collapse
 *   2. Text filter  — applies {@link GridModelFilter} predicate (if set)
 *   3. Sort         — applies the current {@link SortSpec} list (composite comparator)
 *
 * Load rows before attaching a SmartGrid, or call {@link #notifyDataChanged()}
 * after bulk mutations.
 */
public class DefaultGridModel implements GridModel {

    private final List<GridRow>           rows      = new ArrayList<>();
    private final List<ColumnDef>         columns   = new ArrayList<>();
    private final List<GridModelListener> listeners = new ArrayList<>();

    private final List<GridRow> visibleRows      = new ArrayList<>();
    private boolean             visibleRowsDirty = true;

    // Multi-column sort specification; index 0 = primary sort
    private final List<SortSpec> sortSpecs = new ArrayList<>();

    // Filter state
    private GridModelFilter filter = null;

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

    /** Removes all rows without firing events. */
    public DefaultGridModel clearRows() {
        rows.clear();
        visibleRowsDirty = true;
        return this;
    }

    /**
     * Populates the model from tab-delimited text, replacing all existing rows and columns.
     *
     * @param tsvData   the raw tab-delimited string (lines separated by \n or \r\n)
     * @param hasHeader true if the first line contains column header labels;
     *                  false to auto-generate headers A, B, C, ...
     */
    public DefaultGridModel loadFromTSV(String tsvData, boolean hasHeader) {
        rows.clear();
        columns.clear();

        if (tsvData == null || tsvData.trim().isEmpty()) {
            return notifyDataChanged();
        }

        String[] lines = tsvData.split("\\r?\\n", -1);
        if (lines.length == 0) {
            return notifyDataChanged();
        }

        int startLine;
        if (hasHeader) {
            String[] headers = lines[0].split("\\t", -1);
            for (String h : headers) {
                String label = h.trim().isEmpty() ? "col" + columns.size() : h.trim();
                columns.add(new ColumnDef(label, label, true));
            }
            startLine = 1;
        } else {
            String[] firstFields = lines[0].split("\\t", -1);
            for (int i = 0; i < firstFields.length; i++) {
                String label = columnLabel(i);
                columns.add(new ColumnDef(label, label, true));
            }
            startLine = 0;
        }

        for (int i = startLine; i < lines.length; i++) {
            if (lines[i].trim().isEmpty()) {
                continue;
            }
            String[] fields = lines[i].split("\\t", -1);
            GridRow row = new GridRow();
            for (int j = 0; j < columns.size(); j++) {
                row.put(columns.get(j).getKey(), j < fields.length ? fields[j] : "");
            }
            rows.add(row);
        }

        visibleRowsDirty = true;
        return notifyDataChanged();
    }

    /** Converts a zero-based column index to an Excel-style letter label (A, B, ..., Z, AA, ...). */
    private static String columnLabel(int index) {
        StringBuilder sb = new StringBuilder();
        index++;
        while (index > 0) {
            index--;
            sb.insert(0, (char) ('A' + (index % 26)));
            index /= 26;
        }
        return sb.toString();
    }

    /** Recomputes the visible-row projection then fires a model-reset event. */
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

    /** Sets the full multi-column sort specification and fires a model-reset. */
    public void sort(List<SortSpec> specs) {
        sortSpecs.clear();
        if (specs != null) {
            sortSpecs.addAll(specs);
        }
        notifyDataChanged();
    }

    /** Convenience: single-column sort (backward-compatible). */
    public void sort(String key, SortOrder order) {
        sortSpecs.clear();
        if (key != null && order != null && order != SortOrder.NONE) {
            sortSpecs.add(new SortSpec(key, order));
        }
        notifyDataChanged();
    }

    /** Returns the primary sort key, or {@code null} if unsorted. */
    public String getSortKey() {
        return sortSpecs.isEmpty() ? null : sortSpecs.get(0).getKey();
    }

    /** Returns the primary sort order, or {@code NONE} if unsorted. */
    public SortOrder getSortOrder() {
        return sortSpecs.isEmpty() ? SortOrder.NONE : sortSpecs.get(0).getOrder();
    }

    /** Returns an unmodifiable view of the full sort specification list. */
    public List<SortSpec> getSortSpecs() {
        return Collections.unmodifiableList(sortSpecs);
    }

    // -------------------------------------------------------------------------
    // Filtering
    // -------------------------------------------------------------------------

    /** Sets the active filter and fires a model-reset. Pass {@code null} to clear. */
    public void setFilter(GridModelFilter f) {
        this.filter = f;
        notifyDataChanged();
    }

    /** Clears the active filter and fires a model-reset. */
    public void clearFilter() {
        setFilter(null);
    }

    public GridModelFilter getFilter() {
        return filter;
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
    public int getTotalRowCount() {
        return rows.size();
    }

    @Override
    public GridRow getRow(int index) {
        ensureVisible();
        return visibleRows.get(index);
    }

    @Override
    public List<ColumnDef> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    @Override
    public void addGridModelListener(GridModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeGridModelListener(GridModelListener l) {
        listeners.remove(l);
    }

    // -------------------------------------------------------------------------
    // Visible-row computation (tree filter → text filter → sort)
    // -------------------------------------------------------------------------

    private void ensureVisible() {
        if (visibleRowsDirty) {
            computeVisibleRows();
        }
    }

    private void computeVisibleRows() {
        visibleRows.clear();

        // Step 1: tree filter — respects depth / expand / collapse
        List<GridRow> treeVisible = new ArrayList<>();
        int hiddenDepth = Integer.MAX_VALUE;
        for (GridRow row : rows) {
            if (row.getDepth() > hiddenDepth) {
                continue;
            }
            treeVisible.add(row);
            if (row.isHasChildren() && !row.isExpanded()) {
                hiddenDepth = row.getDepth();
            } else {
                hiddenDepth = Integer.MAX_VALUE;
            }
        }

        // Step 2: text filter — skips rows that don't pass the predicate
        for (GridRow row : treeVisible) {
            if (filter == null || filter.accept(row)) {
                visibleRows.add(row);
            }
        }

        // Step 3: multi-column sort — composite comparator over the sort spec list
        if (!sortSpecs.isEmpty()) {
            Collections.sort(visibleRows, buildComparator(sortSpecs));
        }

        visibleRowsDirty = false;
    }

    private static Comparator<GridRow> buildComparator(List<SortSpec> specs) {
        return (a, b) -> {
            for (SortSpec spec : specs) {
                int cmp = compareValues(a.get(spec.getKey()), b.get(spec.getKey()));
                if (spec.getOrder() == SortOrder.DESCENDING) {
                    cmp = -cmp;
                }
                if (cmp != 0) {
                    return cmp;
                }
            }
            return 0;
        };
    }

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
