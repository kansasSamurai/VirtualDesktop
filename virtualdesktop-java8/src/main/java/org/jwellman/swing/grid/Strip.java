package org.jwellman.swing.grid;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * A fixed-width column of components positioned to the left of the data columns
 * inside VirtualCanvas. Both the checkbox selection strip and the tree zone strip
 * implement this interface; SmartGrid manages {@code List<Strip>} and iterates them
 * when allocating slots, binding rows, and building header/filter/footer panels.
 *
 * <p>Strips live inside VirtualCanvas alongside the data slot panels, so they scroll
 * for free with no synchronization code. SmartGrid computes each strip's x-offset as
 * the cumulative sum of preceding visible strip widths.
 *
 * <p>{@code bind()} receives the absolute canvas {@code xOffset} and {@code y} so
 * each strip can position its own components without a back-reference to SmartGrid.
 */
public interface Strip {

    /** Pixel width of this strip. Fixed for the lifetime of the strip. */
    int getWidth();

    /** Whether this strip is currently active/visible. */
    boolean isVisible();

    /**
     * Allocates ({@code slotCount > 0}) or releases ({@code slotCount == 0}) the
     * strip's per-slot components on the given canvas. Called by SmartGrid whenever
     * the visible slot count changes.
     */
    void reallocate(int slotCount, JPanel canvas);

    /**
     * Binds slot {@code slotIdx} to the given model row. The strip positions its
     * component at {@code (xOffset, y, getWidth(), rowHeight)} within the canvas
     * and updates content/state for the row.
     */
    void bind(int slotIdx, int xOffset, int y, int rowHeight, int modelIdx, GridRow row);

    /** Hides the strip's component for slot {@code slotIdx} (off-screen row). */
    void hide(int slotIdx);

    /**
     * Returns the component to show in the header label row at this strip's position.
     * The caller sets its bounds; the component width must equal {@link #getWidth()}.
     */
    JComponent headerCell(int height);

    /**
     * Returns an opaque-false spacer for the per-column filter row at this strip's
     * position. The caller sets its bounds.
     */
    JComponent filterSpacer(int height);

    /**
     * Returns an opaque-false spacer for the footer row at this strip's position.
     * The caller sets its bounds.
     */
    JComponent footerSpacer(int height);
}
