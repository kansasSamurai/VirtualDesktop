package org.jwellman.virtualdesktop.docking;

import java.awt.Container;

/**
 * Represents a docking workspace where dockable components can be added.
 * Each VirtualAppFrame typically has one workspace.
 *
 * <p>A workspace provides a container that manages the layout and positioning
 * of dockable components within it.</p>
 *
 * @author Rick Wellman
 */
public interface DockingWorkspace {

    /**
     * Get the unique identifier for this workspace.
     *
     * @return workspace ID
     */
    String getWorkspaceId();

    /**
     * Get the container component to be added to the frame.
     * This container should be set as the content pane of the JInternalFrame.
     *
     * @return The visual container for this workspace
     */
    Container getContainer();

    /**
     * Add a dockable component to this workspace.
     *
     * @param dockable The dockable component to add
     */
    void addDockable(Dockable dockable);

    /**
     * Remove a dockable component from this workspace.
     *
     * @param dockableId The ID of the dockable to remove
     */
    void removeDockable(String dockableId);

    /**
     * Get a dockable by its ID.
     *
     * @param dockableId The dockable ID
     * @return The dockable, or null if not found
     */
    Dockable getDockable(String dockableId);

    /**
     * Check if a dockable with the given ID exists in this workspace.
     *
     * @param dockableId The dockable ID to check
     * @return true if exists
     */
    boolean hasDockable(String dockableId);
}
