package org.jwellman.virtualdesktop.desktop;

import java.awt.Point;

/**
 * Callback interface from a DesktopView to its controller.
 *
 * <p>The view reports user gestures; the controller owns store dispatch and ToolService.</p>
 */
public interface DesktopViewListener {

    /**
     * User selected a shortcut (single click / press).
     *
     * @param shortcutId shortcut id
     */
    void onShortcutSelected(String shortcutId);

    /**
     * User activated a shortcut (double-click).
     *
     * @param shortcutId shortcut id
     */
    void onShortcutActivated(String shortcutId);

    /**
     * User finished dragging a shortcut to a new location.
     *
     * @param shortcutId shortcut id
     * @param x new x
     * @param y new y
     */
    void onShortcutMoved(String shortcutId, int x, int y);

    /**
     * User requested a context menu on a shortcut.
     *
     * @param shortcutId shortcut id
     * @param screenPoint anchor point in screen coordinates
     */
    void onShortcutContextRequested(String shortcutId, Point screenPoint);

    /**
     * User clicked empty desktop background — clear selection.
     */
    void onBackgroundClicked();

}
