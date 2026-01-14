package org.jwellman.virtualdesktop.docking.impl;

import javax.swing.JFrame;

import org.jwellman.virtualdesktop.docking.DockingException;
import org.jwellman.virtualdesktop.docking.DockingService;
import org.jwellman.virtualdesktop.docking.DockingTheme;
import org.jwellman.virtualdesktop.docking.DockingWorkspace;
import org.jwellman.virtualdesktop.docking.impl.bibliothek.BibliothekDockingProvider;
import org.jwellman.virtualdesktop.docking.spi.DockingProvider;

/**
 * Implementation of the DockingService interface.
 * Delegates all operations to the configured provider.
 *
 * <p>Currently hardcoded to use BibliothekDockingProvider.
 * In the future, this could use ServiceLoader for dynamic provider selection.</p>
 *
 * @author Rick Wellman
 */
public class DockingServiceImpl implements DockingService {

    private final DockingProvider provider;

    /**
     * Create a new docking service implementation.
     * Currently hardcodes the Bibliothek provider.
     */
    public DockingServiceImpl() {
        // For now, hardcode the Bibliothek provider
        // In the future, this could use ServiceLoader or configuration
        this.provider = new BibliothekDockingProvider();
    }

    @Override
    public void initialize(JFrame mainFrame) throws DockingException {
        provider.initialize(mainFrame);
    }

    @Override
    public boolean isInitialized() {
        return provider.isInitialized();
    }

    @Override
    public DockingWorkspace createWorkspace(String workspaceId) throws DockingException {
        return provider.createWorkspace(workspaceId);
    }

    @Override
    public DockingWorkspace getDefaultWorkspace() {
        return provider.getDefaultWorkspace();
    }

    @Override
    public void setTheme(DockingTheme theme) {
        provider.setTheme(theme);
    }

    @Override
    public void shutdown() {
        provider.shutdown();
    }

    /**
     * Get the underlying provider.
     * Package-private to allow access from docking package but not external code.
     *
     * @return the provider
     */
    public DockingProvider getProvider() {
        return provider;
    }
}
