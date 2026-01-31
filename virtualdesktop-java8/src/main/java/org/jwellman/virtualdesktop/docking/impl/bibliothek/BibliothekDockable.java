package org.jwellman.virtualdesktop.docking.impl.bibliothek;

import javax.swing.JComponent;

import org.jwellman.virtualdesktop.docking.Dockable;
import org.jwellman.virtualdesktop.docking.DockableLocation;

import bibliothek.gui.dock.common.CLocation;
import bibliothek.gui.dock.common.SingleCDockable;

/**
 * Dockable implementation that wraps Bibliothek's SingleCDockable.
 *
 * @author Rick Wellman
 */
class BibliothekDockable implements Dockable {

    private final SingleCDockable nativeDockable;
    private final String id;
    private final String title;
    private final JComponent component;
    private final boolean initialVisibility;
    private final DockableLocation initialLocation;

    BibliothekDockable(SingleCDockable nativeDockable, String id, String title, JComponent component,
                       boolean initialVisibility, DockableLocation initialLocation) {
        this.nativeDockable = nativeDockable;
        this.id = id;
        this.title = title;
        this.component = component;
        this.initialVisibility = initialVisibility;
        this.initialLocation = initialLocation;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public JComponent getComponent() {
        return component;
    }

    @Override
    public void setLocation(DockableLocation location) {
        Object nativeLoc = location.toNativeLocation();
        if (nativeLoc instanceof CLocation) {
            nativeDockable.setLocation((CLocation) nativeLoc);
        } else if (nativeLoc instanceof Object[]) {
            // Handle our temporary location format
            Object[] locData = (Object[]) nativeLoc;
            CLocation cloc = convertLocationData(locData);
            if (cloc != null) {
                nativeDockable.setLocation(cloc);
            }
        }
    }

    private CLocation convertLocationData(Object[] locData) {
        if (locData.length < 2) return null;

        String type = (String) locData[0];
        BibliothekWorkspace workspace = (BibliothekWorkspace) locData[1];

        switch (type) {
            case "normal":
//                return CLocation.base(workspace.getContentArea()).normalNorth(0.5);
                // The following was the original implementation: I am experimenting
                // to see if there is a "better" way because using normal along with
                // the other types below usually ends up with "stacking" which is
                // not the desired end effect.
                return CLocation.base(workspace.getContentArea()).normal();
            case "minimalNorth":
                int northIndex = (Integer) locData[2];
                return CLocation.base(workspace.getContentArea()).minimalNorth(northIndex);
            case "minimalSouth":
                int southIndex = (Integer) locData[2];
                return CLocation.base(workspace.getContentArea()).minimalSouth(southIndex);
            case "minimalWest":
                int westIndex = (Integer) locData[2];
                return CLocation.base(workspace.getContentArea()).minimalWest(westIndex);
            case "minimalEast":
                int eastIndex = (Integer) locData[2];
                return CLocation.base(workspace.getContentArea()).minimalEast(eastIndex);
            case "north":
                double northSize = (Double) locData[2];
                return CLocation.base(workspace.getContentArea()).normalNorth(northSize);
//              return CLocation.base(workspace.getContentArea()).normal().north(southSize);
            case "south":
                double southSize = (Double) locData[2];
                return CLocation.base(workspace.getContentArea()).normalSouth(southSize);
//                return CLocation.base(workspace.getContentArea()).normal().south(southSize);
            case "west":
                double westSize = (Double) locData[2];
                return CLocation.base(workspace.getContentArea()).normalWest(westSize);
//                return CLocation.base(workspace.getContentArea()).normal().west(westSize);
            case "east":
                double eastSize = (Double) locData[2];
                return CLocation.base(workspace.getContentArea()).normalEast(eastSize);
//                return CLocation.base(workspace.getContentArea()).normal().east(eastSize);
            case "external":
                int x = (Integer) locData[1];
                int y = (Integer) locData[2];
                int width = (Integer) locData[3];
                int height = (Integer) locData[4];
                return CLocation.external(x, y, width, height);
            default:
                return null;
        }
    }

    @Override
    public void setVisible(boolean visible) {
        nativeDockable.setVisible(visible);
    }

    @Override
    public boolean isVisible() {
        return nativeDockable.isVisible();
    }

    @Override
    public void close() {
        nativeDockable.setVisible(false);
    }

    /**
     * Get the underlying SingleCDockable.
     * Package-private for internal use.
     *
     * @return the native dockable
     */
    SingleCDockable getNativeDockable() {
        return nativeDockable;
    }

    /**
     * Apply initial settings (location and visibility).
     * Must be called AFTER the dockable is added to the control.
     * Package-private for internal use.
     */
    void applyInitialSettings() {
        // Apply location if provided
        if (initialLocation != null) {
            setLocation(initialLocation);
        }

        // Apply visibility
        setVisible(initialVisibility);
    }
}
