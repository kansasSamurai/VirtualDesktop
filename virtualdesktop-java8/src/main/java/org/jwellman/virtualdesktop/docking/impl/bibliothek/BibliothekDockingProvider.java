package org.jwellman.virtualdesktop.docking.impl.bibliothek;

import javax.swing.JFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jwellman.virtualdesktop.docking.DockableBuilder;
import org.jwellman.virtualdesktop.docking.DockingException;
import org.jwellman.virtualdesktop.docking.DockingTheme;
import org.jwellman.virtualdesktop.docking.DockingWorkspace;
import org.jwellman.virtualdesktop.docking.spi.DockingProvider;

import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.event.CControlListener;
import bibliothek.gui.dock.common.intern.CDockable;
import bibliothek.gui.dock.common.theme.ThemeMap;

/**
 * Docking provider implementation using the Bibliothek Docking Frames library (version 1.1.3).
 *
 * <p>This adapter wraps the CControl and provides a framework-agnostic API.</p>
 *
 * @author Rick Wellman
 */
public class BibliothekDockingProvider implements DockingProvider {

    private static final Logger LOG = LoggerFactory.getLogger(BibliothekDockingProvider.class);

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

            // Add listener for dockable events (Redux integration)
            control.addControlListener(new DockingControlListener());

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

    /**
     * Get the native CControl instance for demo/testing purposes.
     * This breaks abstraction but is needed for legacy demo code.
     *
     * @return the native CControl
     */
    public CControl getNativeControl() {
        return control;
    }

    /**
     * Inner listener class for CControl events.
     * Dispatches Redux actions when dockables are added/removed/relocated.
     */
    private static class DockingControlListener implements CControlListener {

        @Override
        public void added(CControl control, CDockable dockable) {
            String panelId = getDockableId(dockable);
            LOG.debug("Panel added: {}", panelId);
            // Note: TOOL_OPENED is dispatched by DesktopManager, not here
        }

        @Override
        public void removed(CControl control, CDockable dockable) {
            String panelId = getDockableId(dockable);
            LOG.debug("Panel removed: {}", panelId);
            // Note: TOOL_CLOSED is dispatched by DesktopManager, not here
        }

        @Override
        public void opened(CControl control, CDockable dockable) {
            String panelId = getDockableId(dockable);
            LOG.debug("Panel opened (visible): {}", panelId);
        }

        @Override
        public void closed(CControl control, CDockable dockable) {
            String panelId = getDockableId(dockable);
            LOG.debug("Panel closed (hidden): {}", panelId);
        }

        private String getDockableId(CDockable dockable) {
            if (dockable instanceof bibliothek.gui.dock.common.DefaultSingleCDockable) {
                return ((bibliothek.gui.dock.common.DefaultSingleCDockable) dockable).getUniqueId();
            }
            return dockable.toString();
        }
    }
}
