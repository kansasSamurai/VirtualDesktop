package org.jwellman.swing.grid;

/**
 * Optional interface for row panels that want selection state pushed to them
 * by SmartGrid rather than polling the {@link javax.swing.ListSelectionModel}
 * themselves inside {@link Recyclable#bind(GridRow, int)}.
 *
 * SmartGrid calls {@link #setSelected(boolean)} immediately after every
 * {@code bind()} call. Row panels that handle selection in {@code bind()}
 * directly (e.g. {@link StandardRowPanel}) do not need to implement this.
 */
public interface Selectable {

    /**
     * Called by SmartGrid after {@code bind()} with the current selection state.
     * Implementations should update their visual appearance accordingly.
     */
    void setSelected(boolean selected);
}
