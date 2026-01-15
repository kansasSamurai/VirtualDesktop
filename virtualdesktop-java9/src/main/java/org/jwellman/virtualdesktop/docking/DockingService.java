package org.jwellman.virtualdesktop.docking;

import javax.swing.JFrame;

/**
 * Central service for managing the docking framework.
 * This is a singleton that abstracts the underlying docking implementation.
 *
 * <p>Usage example:</p>
 * <pre>
 * DockingService service = DockingServiceFactory.getInstance();
 * service.initialize(mainFrame);
 * service.setTheme(DockingTheme.ECLIPSE);
 *
 * DockingWorkspace workspace = service.createWorkspace("myWorkspace");
 * </pre>
 *
 * @author Rick Wellman
 */
public interface DockingService {

    /**
     * Initialize the docking framework with the main application frame.
     * Must be called once during application startup.
     *
     * @param mainFrame The main JFrame of the application
     * @throws DockingException if initialization fails
     */
    void initialize(JFrame mainFrame) throws DockingException;

    /**
     * Check if the docking service has been initialized.
     *
     * @return true if initialized
     */
    boolean isInitialized();

    /**
     * Create a new docking workspace for a virtual application.
     * Each internal frame gets its own workspace.
     *
     * @param workspaceId Unique identifier for this workspace
     * @return A new docking workspace
     * @throws DockingException if workspace creation fails
     */
    DockingWorkspace createWorkspace(String workspaceId) throws DockingException;

    /**
     * Get the default/primary workspace.
     * Used for backward compatibility.
     *
     * @return The default workspace
     */
    DockingWorkspace getDefaultWorkspace();

    /**
     * Set the visual theme for the docking framework.
     *
     * @param theme The theme to apply
     */
    void setTheme(DockingTheme theme);

    /**
     * Shutdown the docking service and release resources.
     */
    void shutdown();
}
