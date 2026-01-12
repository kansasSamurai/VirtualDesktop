package org.jwellman.virtualdesktop.docking.impl.bibliothek;

import java.awt.Container;
import java.util.HashMap;
import java.util.Map;

import org.jwellman.virtualdesktop.docking.Dockable;
import org.jwellman.virtualdesktop.docking.DockingWorkspace;

import bibliothek.gui.dock.common.CContentArea;
import bibliothek.gui.dock.common.CControl;

/**
 * Workspace implementation that wraps Bibliothek's CContentArea.
 *
 * @author Rick Wellman
 */
class BibliothekWorkspace implements DockingWorkspace {

    private final CControl control;
    private final String workspaceId;
    private final CContentArea contentArea;
    private final Map<String, BibliothekDockable> dockables;

    BibliothekWorkspace(CControl control, String workspaceId, CContentArea contentArea) {
        this.control = control;
        this.workspaceId = workspaceId;
        this.contentArea = contentArea;
        this.dockables = new HashMap<String, BibliothekDockable>();
    }

    @Override
    public String getWorkspaceId() {
        return workspaceId;
    }

    @Override
    public Container getContainer() {
        return contentArea;
    }

    @Override
    public void addDockable(Dockable dockable) {
        if (!(dockable instanceof BibliothekDockable)) {
            throw new IllegalArgumentException("Dockable must be a BibliothekDockable");
        }

        BibliothekDockable bibDockable = (BibliothekDockable) dockable;
        dockables.put(dockable.getId(), bibDockable);

        // Add to control FIRST (required by Docking Frames)
        control.addDockable(bibDockable.getNativeDockable());

        // Now apply initial settings (location and visibility)
        bibDockable.applyInitialSettings();
    }

    @Override
    public void removeDockable(String dockableId) {
        BibliothekDockable dockable = dockables.remove(dockableId);
        if (dockable != null) {
            control.removeDockable(dockable.getNativeDockable());
        }
    }

    @Override
    public Dockable getDockable(String dockableId) {
        return dockables.get(dockableId);
    }

    @Override
    public boolean hasDockable(String dockableId) {
        return dockables.containsKey(dockableId);
    }

    /**
     * Get the underlying CContentArea.
     * Package-private for internal use.
     *
     * @return the content area
     */
    CContentArea getContentArea() {
        return contentArea;
    }
}
