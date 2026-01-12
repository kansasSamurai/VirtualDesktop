package org.jwellman.virtualdesktop.docking.impl.bibliothek;

import javax.swing.JFrame;

import org.jwellman.virtualdesktop.docking.DockableBuilder;
import org.jwellman.virtualdesktop.docking.DockingException;
import org.jwellman.virtualdesktop.docking.DockingTheme;
import org.jwellman.virtualdesktop.docking.DockingWorkspace;
import org.jwellman.virtualdesktop.docking.spi.DockingProvider;

import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.theme.ThemeMap;

/**
 * Docking provider implementation using the Bibliothek Docking Frames library (version 1.1.3).
 *
 * <p>This adapter wraps the CControl and provides a framework-agnostic API.</p>
 *
 * @author Rick Wellman
 */
public class BibliothekDockingProvider implements DockingProvider {

    private CControl control;
    private BibliothekWorkspace defaultWorkspace;

    @Override
    public String getProviderName() {
        return "Docking Frames (Bibliothek)";
    }

    @Override
    public String getProviderVersion() {
        return "1.1.3";
    }

    @Override
    public void initialize(JFrame mainFrame) throws DockingException {
        if (control != null) {
            throw new DockingException("Docking provider already initialized");
        }

        try {
            control = new CControl(mainFrame);

            // Set default theme (Eclipse)
            final ThemeMap themes = control.getThemes();
            themes.select(ThemeMap.KEY_ECLIPSE_THEME);

            // Create default workspace
            defaultWorkspace = new BibliothekWorkspace(
                control,
                "default",
                control.getContentArea()
            );

        } catch (Exception e) {
            throw new DockingException("Failed to initialize Docking Frames", e);
        }
    }

    @Override
    public DockingWorkspace createWorkspace(String workspaceId) throws DockingException {
        if (!isInitialized()) {
            throw new DockingException("Provider not initialized");
        }

        try {
            return new BibliothekWorkspace(
                control,
                workspaceId,
                control.createContentArea(workspaceId)
            );
        } catch (Exception e) {
            throw new DockingException("Failed to create workspace: " + workspaceId, e);
        }
    }

    @Override
    public DockingWorkspace getDefaultWorkspace() {
        return defaultWorkspace;
    }

    @Override
    public DockableBuilder createDockableBuilder() {
        return new BibliothekDockableBuilder(control);
    }

    @Override
    public void setTheme(DockingTheme theme) {
        if (!isInitialized()) {
            return;
        }

        final ThemeMap themes = control.getThemes();
        switch (theme) {
            case FLAT:
                themes.select(ThemeMap.KEY_FLAT_THEME);
                break;
            case ECLIPSE:
                themes.select(ThemeMap.KEY_ECLIPSE_THEME);
                break;
            case SMOOTH:
                themes.select(ThemeMap.KEY_SMOOTH_THEME);
                break;
            case BASIC:
                themes.select(ThemeMap.KEY_BASIC_THEME);
                break;
            case BUBBLE:
                themes.select(ThemeMap.KEY_BUBBLE_THEME);
                break;
        }
    }

    @Override
    public boolean isInitialized() {
        return control != null;
    }

    @Override
    public void shutdown() {
        if (control != null) {
            // Cleanup if necessary
            control = null;
            defaultWorkspace = null;
        }
    }

    /**
     * Get the underlying CControl instance.
     * Package-private for internal use by adapter classes.
     *
     * @return the CControl
     */
    CControl getControl() {
        return control;
    }
}
