package org.jwellman.virtualdesktop.docking;

import javax.swing.JFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes the shared {@link DockingService} once at application startup.
 *
 * <p>Replaces docking bootstrap that previously lived on VirtualAppSpec.
 * Spec authors should not call this — App / composition root does.</p>
 */
public final class DockingBootstrap {

    private static final Logger LOG = LoggerFactory.getLogger(DockingBootstrap.class);

    private DockingBootstrap() {
    }

    /**
     * Initialize docking against the main application frame (idempotent).
     *
     * @param frame the main JFrame
     */
    public static void initialize(JFrame frame) {
        DockingService dockingService = DockingServiceFactory.getInstance();
        if (dockingService.isInitialized()) {
            LOG.warn("Docking service already initialized — ignoring re-init");
            return;
        }
        try {
            dockingService.initialize(frame);
            // Flat is preferred (no animations); Eclipse experiments with JTattoo LNF
            dockingService.setTheme(DockingTheme.ECLIPSE);
        } catch (DockingException e) {
            throw new RuntimeException("Failed to initialize docking service", e);
        }
    }

}
