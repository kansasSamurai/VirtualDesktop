package org.jwellman.virtualdesktop.docking;

import javax.swing.JComponent;

/**
 * Represents a dockable component that can be added to a workspace.
 *
 * <p>A dockable wraps a Swing component and provides docking-specific
 * operations like positioning, visibility control, and closing.</p>
 *
 * @author Rick Wellman
 */
public interface Dockable {

    /**
     * Get the unique identifier for this dockable.
     *
     * @return dockable ID
     */
    String getId();

    /**
     * Get the display title for this dockable.
     *
     * @return title
     */
    String getTitle();

    /**
     * Get the actual Swing component being docked.
     *
     * @return the component
     */
    JComponent getComponent();

    /**
     * Set the location/position of this dockable within its workspace.
     *
     * @param location The location descriptor
     */
    void setLocation(DockableLocation location);

    /**
     * Set the visibility of this dockable.
     *
     * @param visible true to show, false to hide
     */
    void setVisible(boolean visible);

    /**
     * Check if this dockable is currently visible.
     *
     * @return true if visible
     */
    boolean isVisible();

    /**
     * Close this dockable (may trigger cleanup events).
     */
    void close();
}
