package org.jwellman.virtualdesktop.docking.impl.bibliothek;

import javax.swing.Icon;
import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jwellman.virtualdesktop.docking.Dockable;
import org.jwellman.virtualdesktop.docking.DockableBuilder;
import org.jwellman.virtualdesktop.docking.DockableLocation;
import org.jwellman.virtualdesktop.docking.DockingException;
import org.jwellman.virtualdesktop.state.actions.SimpleAction;
import org.jwellman.virtualdesktop.state.store.AppStore;

import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.CLocation;
import bibliothek.gui.dock.common.DefaultSingleCDockable;
import bibliothek.gui.dock.common.event.CDockableLocationEvent;
import bibliothek.gui.dock.common.event.CDockableLocationListener;

/**
 * Builder implementation for creating Bibliothek dockables.
 *
 * @author Rick Wellman
 */
class BibliothekDockableBuilder implements DockableBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(BibliothekDockableBuilder.class);

    private String id;
    private String title;
    private JComponent component;
    private Icon icon;
    private DockableLocation location;
    private boolean visible = false;

    @SuppressWarnings("unused")
    private final CControl control;

    BibliothekDockableBuilder(CControl control) {
        this.control = control;
    }

    @Override
    public DockableBuilder withId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public DockableBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    @Override
    public DockableBuilder withComponent(JComponent component) {
        this.component = component;
        return this;
    }

    @Override
    public DockableBuilder withIcon(Icon icon) {
        this.icon = icon;
        return this;
    }

    @Override
    public DockableBuilder withLocation(DockableLocation location) {
        this.location = location;
        return this;
    }

    @Override
    public DockableBuilder withVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    @Override
    public Dockable build() throws DockingException {
        // Validate required fields
        if (id == null || id.isEmpty()) {
            throw new DockingException("Dockable ID is required");
        }
        if (title == null) {
            title = id;
        }
        if (component == null) {
            throw new DockingException("Dockable component is required");
        }

        // Create the native dockable
        DefaultSingleCDockable nativeDockable = new DefaultSingleCDockable(id, title, component);

        // Apply icon if provided
        if (icon != null) {
            nativeDockable.setTitleIcon(icon);
        }

        // Add location listener for Redux integration
        final String dockableId = id;
        nativeDockable.addCDockableLocationListener(new CDockableLocationListener() {
            @Override
            public void changed(CDockableLocationEvent event) {
                if (event.isLocationChanged()) {
                    CLocation oldLoc = event.getOldLocation();
                    CLocation newLoc = event.getNewLocation();

                    String oldWorkspace = oldLoc != null ? extractWorkspaceId(oldLoc) : null;
                    String newWorkspace = newLoc != null ? extractWorkspaceId(newLoc) : null;

                    LOG.debug("Panel {} location changed: {} -> {}", dockableId, oldWorkspace, newWorkspace);

                    // Dispatch action if workspace changed
                    if (newWorkspace != null && !newWorkspace.equals(oldWorkspace)) {
                        AppStore.get().dispatch(
                            SimpleAction.panelLocationChanged(dockableId, newWorkspace));
                    }
                }
            }

            private String extractWorkspaceId(CLocation location) {
                // Extract workspace ID from location string
                // CLocation.toString() includes the workspace/area info
                String locStr = location.toString();
                // For now, just use the string representation
                // More sophisticated parsing can be added later
                return locStr;
            }
        });

        // Wrap in our dockable
        BibliothekDockable dockable = new BibliothekDockable(nativeDockable, id, title, component, visible, location);

        return dockable;
    }
}
