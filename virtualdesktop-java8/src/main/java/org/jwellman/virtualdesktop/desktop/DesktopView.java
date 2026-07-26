package org.jwellman.virtualdesktop.desktop;

import java.util.List;

import javax.swing.JComponent;

/**
 * Abstraction for the desktop surface (shortcut tiles on the wallpaper pane).
 *
 * <p>The controller holds this interface only — swapping implementations
 * (classic free-form icons, grid desktop, etc.) requires no controller changes.</p>
 */
public interface DesktopView {

    /**
     * @return component to embed as the desktop host (typically a JDesktopPane)
     */
    JComponent getComponent();

    /**
     * Replaces the full shortcut tile list from current DesktopState.
     */
    void setShortcuts(List<DesktopShortcutItem> shortcuts);

    /**
     * Syncs visual selection. Pass null to clear.
     */
    void setSelectedId(String shortcutId);

    /**
     * Registers the listener for user intents. Called once at wiring time.
     */
    void setListener(DesktopViewListener listener);

}
