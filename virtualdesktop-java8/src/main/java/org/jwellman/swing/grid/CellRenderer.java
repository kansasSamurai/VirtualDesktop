package org.jwellman.swing.grid;

import javax.swing.JComponent;

/**
 * Renders a single cell within a row panel.
 *
 * The {@code existing} parameter enables Android-style cell recycling: the renderer
 * receives whatever component currently occupies the slot and may update it in-place
 * (cheapest) or return a new component (needed when the component type changes).
 * SmartGrid swaps the component into the panel automatically when a new instance is
 * returned.
 *
 * Registered on a SmartGrid instance via {@link SmartGrid#registerCellRenderer} or
 * the convenience wrapper {@link SmartGrid#registerFormatter}.  A column opts in by
 * setting its {@code fndType} to the registered key (e.g. {@code "currency"}).
 */
public interface CellRenderer {
    /**
     * @param col      column being rendered
     * @param value    raw value from {@link GridRow#get(String)} (may be null)
     * @param row      the full row (for context — tags, sibling values, etc.)
     * @param existing the component currently occupying this cell slot; reuse when possible
     * @return the component to display; may be {@code existing} updated in-place
     */
    JComponent render(ColumnDef col, Object value, GridRow row, JComponent existing);
}
