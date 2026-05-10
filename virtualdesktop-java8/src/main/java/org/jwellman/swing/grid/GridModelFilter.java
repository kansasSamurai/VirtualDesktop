package org.jwellman.swing.grid;

/**
 * Predicate for client-side row filtering in {@link DefaultGridModel}.
 * Applied after the tree-collapse filter and before sorting.
 *
 * Example — case-insensitive text search across all columns:
 * <pre>
 * grid.setFilter(row -> {
 *     String q = searchField.getText().trim().toLowerCase();
 *     for (ColumnDef col : model.getColumns()) {
 *         Object v = row.get(col.getKey());
 *         if (v != null && v.toString().toLowerCase().contains(q)) return true;
 *     }
 *     return false;
 * });
 * </pre>
 */
@FunctionalInterface
public interface GridModelFilter {
    boolean accept(GridRow row);
}
