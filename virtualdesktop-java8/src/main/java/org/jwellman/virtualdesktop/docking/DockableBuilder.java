package org.jwellman.virtualdesktop.docking;

import javax.swing.Icon;
import javax.swing.JComponent;

/**
 * Builder for creating Dockable instances using a fluent API.
 *
 * <p>Usage example:</p>
 * <pre>
 * Dockable dockable = provider.createDockableBuilder()
 *     .withId("myDockable")
 *     .withTitle("My Component")
 *     .withComponent(myJPanel)
 *     .withLocation(DockableLocation.normalIn(workspace))
 *     .withVisible(true)
 *     .build();
 * </pre>
 *
 * @author Rick Wellman
 */
public interface DockableBuilder {

    /**
     * Set the unique ID for this dockable.
     * If not set, an ID may be auto-generated.
     *
     * @param id The unique ID
     * @return this builder for chaining
     */
    DockableBuilder withId(String id);

    /**
     * Set the display title for the dockable.
     *
     * @param title The title
     * @return this builder for chaining
     */
    DockableBuilder withTitle(String title);

    /**
     * Set the component to be docked.
     *
     * @param component The Swing component
     * @return this builder for chaining
     */
    DockableBuilder withComponent(JComponent component);

    /**
     * Set an icon for the dockable.
     *
     * @param icon The icon
     * @return this builder for chaining
     */
    DockableBuilder withIcon(Icon icon);

    /**
     * Set the initial location for the dockable.
     *
     * @param location The location
     * @return this builder for chaining
     */
    DockableBuilder withLocation(DockableLocation location);

    /**
     * Set initial visibility.
     *
     * @param visible true to show immediately
     * @return this builder for chaining
     */
    DockableBuilder withVisible(boolean visible);

    /**
     * Build the dockable instance.
     *
     * @return The created dockable
     * @throws DockingException if required properties are missing
     */
    Dockable build() throws DockingException;
}
