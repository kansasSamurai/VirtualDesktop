package org.jwellman.virtualdesktop.docking.spi;

import javax.swing.JFrame;

import org.jwellman.virtualdesktop.docking.DockableBuilder;
import org.jwellman.virtualdesktop.docking.DockingException;
import org.jwellman.virtualdesktop.docking.DockingTheme;
import org.jwellman.virtualdesktop.docking.DockingWorkspace;

/**
 * Service Provider Interface for docking framework implementations.
 * This allows swapping out the underlying docking framework.
 *
 * <p>Implementations should provide adapters for specific docking frameworks
 * (e.g., Docking Frames, FlexDock, MyDoggy, etc.)</p>
 *
 * @author Rick Wellman
 */
public interface DockingProvider {

    /**
     * Get the name of this provider.
     *
     * @return provider name (e.g., "Docking Frames", "FlexDock")
     */
    String getProviderName();

    /**
     * Get the version of this provider.
     *
     * @return version string
     */
    String getProviderVersion();

    /**
     * Initialize this provider with the main application frame.
     *
     * @param mainFrame The application's main JFrame
     * @throws DockingException if initialization fails
     */
    void initialize(JFrame mainFrame) throws DockingException;

    /**
     * Create a new workspace with the given ID.
     *
     * @param workspaceId Unique workspace identifier
     * @return The created workspace
     * @throws DockingException if creation fails
     */
    DockingWorkspace createWorkspace(String workspaceId) throws DockingException;

    /**
     * Get the default workspace.
     *
     * @return The default workspace
     */
    DockingWorkspace getDefaultWorkspace();

    /**
     * Create a builder for creating dockables.
     *
     * @return A new builder instance
     */
    DockableBuilder createDockableBuilder();

    /**
     * Set the visual theme for the docking framework.
     *
     * @param theme The theme to apply
     */
    void setTheme(DockingTheme theme);

    /**
     * Check if this provider has been initialized.
     *
     * @return true if initialized
     */
    boolean isInitialized();

    /**
     * Shutdown this provider and release resources.
     */
    void shutdown();
}
