package org.jwellman.virtualdesktop.docking;

import org.jwellman.virtualdesktop.docking.impl.DockingServiceImpl;

/**
 * Factory for obtaining the singleton DockingService instance.
 *
 * <p>Usage:</p>
 * <pre>
 * DockingService service = DockingServiceFactory.getInstance();
 * </pre>
 *
 * @author Rick Wellman
 */
public class DockingServiceFactory {

    private static DockingService instance;

    /**
     * Get the singleton docking service instance.
     *
     * @return The docking service
     */
    public static synchronized DockingService getInstance() {
        if (instance == null) {
            instance = new DockingServiceImpl();
        }
        return instance;
    }

    /**
     * Reset the singleton for testing purposes only.
     * Should not be used in production code.
     */
    static synchronized void resetForTesting() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
    }

    // Private constructor to prevent instantiation
    private DockingServiceFactory() {
    }
}
