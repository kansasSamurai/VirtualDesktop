package org.jwellman.virtualdesktop.docking.impl.bibliothek;

import javax.swing.Icon;
import javax.swing.JComponent;

import org.jwellman.virtualdesktop.docking.Dockable;
import org.jwellman.virtualdesktop.docking.DockableBuilder;
import org.jwellman.virtualdesktop.docking.DockableLocation;
import org.jwellman.virtualdesktop.docking.DockingException;

import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.DefaultSingleCDockable;

/**
 * Builder implementation for creating Bibliothek dockables.
 *
 * @author Rick Wellman
 */
class BibliothekDockableBuilder implements DockableBuilder {

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

        // Wrap in our dockable
        BibliothekDockable dockable = new BibliothekDockable(nativeDockable, id, title, component, visible, location);

        return dockable;
    }
}
