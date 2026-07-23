package org.jwellman.virtualdesktop.docking;

import java.awt.Container;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;

import org.jwellman.virtualdesktop.docking.impl.DockingServiceImpl;
import org.jwellman.virtualdesktop.docking.spi.DockingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ToolId-scoped docking host: owns the workspace and dockable registration
 * for one open tool.
 *
 * <p>Created by DesktopManager when hosting a tool — not by VirtualAppSpec.
 * Spec authors that only need a panel never touch this class.</p>
 */
public final class DockingSession {

    private static final Logger LOG = LoggerFactory.getLogger(DockingSession.class);

    /** Global dockable ids to prevent duplicates across workspaces (Bibliothek requirement). */
    private static final Set<String> usedDockableIds = new HashSet<String>();

    private final String toolId;
    private final DockingWorkspace workspace;
    private final Set<String> sessionDockableIds = new HashSet<String>();
    private String primaryDockableId;

    private DockingSession(String toolId, DockingWorkspace workspace) {
        this.toolId = toolId;
        this.workspace = workspace;
    }

    /**
     * Opens a docking session for a running tool instance.
     *
     * @param toolId stable running-instance id (also preferred primary dockable id)
     * @return a new session with an empty workspace
     */
    public static DockingSession open(String toolId) {
        if (toolId == null || toolId.isEmpty()) {
            throw new IllegalArgumentException("toolId is required");
        }
        DockingService service = DockingServiceFactory.getInstance();
        if (!service.isInitialized()) {
            throw new IllegalStateException("DockingService not initialized — call DockingBootstrap.initialize first");
        }

        String workspaceId = toolId;
        DockingWorkspace workspace = null;
        int counter = 1;
        DockingException last = null;
        while (workspace == null && counter < 100) {
            try {
                workspace = service.createWorkspace(workspaceId);
            } catch (DockingException e) {
                last = e;
                workspaceId = toolId + "-" + counter++;
            }
        }
        if (workspace == null) {
            throw new RuntimeException("Failed to create docking workspace for toolId " + toolId, last);
        }
        LOG.debug("DockingSession opened: toolId={}, workspaceId={}", toolId, workspaceId);
        return new DockingSession(toolId, workspace);
    }

    public String getToolId() {
        return toolId;
    }

    public DockingWorkspace getWorkspace() {
        return workspace;
    }

    public Container getContainer() {
        return workspace.getContainer();
    }

    /**
     * Adds the primary content dockable using {@code toolId} as the dockable id.
     *
     * @param title display title
     * @param component content panel
     */
    public void addPrimaryDockable(String title, JComponent component) {
        reserveDockableId(toolId);
        addDockableWithId(toolId, title, component, DockableLocation.normalIn(workspace));
        primaryDockableId = toolId;
    }

    public void addDockable(String title, JComponent component) {
        addDockable(title, component, DockableLocation.normalIn(workspace));
    }

    public void addDockable(String title, JComponent component, DockableLocation location) {
        String dockId = allocateDockableId(title);
        addDockableWithId(dockId, title, component, location);
    }

    private void addDockableWithId(String dockId, String title, JComponent component, DockableLocation location) {
        try {
            DockingService service = DockingServiceFactory.getInstance();
            DockingProvider provider = ((DockingServiceImpl) service).getProvider();
            Dockable dockable = provider.createDockableBuilder()
                .withId(dockId)
                .withTitle(title)
                .withComponent(component)
                .withLocation(location)
                .withVisible(true)
                .build();
            workspace.addDockable(dockable);
            sessionDockableIds.add(dockId);
        } catch (DockingException e) {
            synchronized (usedDockableIds) {
                usedDockableIds.remove(dockId);
            }
            throw new RuntimeException("Failed to add dockable: " + dockId, e);
        }
    }

    private String allocateDockableId(String title) {
        synchronized (usedDockableIds) {
            String dockId = title;
            int counter = 1;
            while (usedDockableIds.contains(dockId) || toolId.equals(dockId)) {
                dockId = title + "-" + counter++;
            }
            usedDockableIds.add(dockId);
            return dockId;
        }
    }

    private void reserveDockableId(String dockId) {
        synchronized (usedDockableIds) {
            if (usedDockableIds.contains(dockId)) {
                throw new IllegalStateException("Dockable id already in use: " + dockId);
            }
            usedDockableIds.add(dockId);
        }
    }

    /**
     * Removes registered dockables and frees ids. Safe to call more than once.
     */
    public void release() {
        for (String dockId : new HashSet<String>(sessionDockableIds)) {
            try {
                workspace.removeDockable(dockId);
            } catch (Exception ex) {
                LOG.debug("removeDockable failed: {}", dockId, ex);
            }
            synchronized (usedDockableIds) {
                usedDockableIds.remove(dockId);
            }
        }
        sessionDockableIds.clear();
        primaryDockableId = null;
        LOG.debug("DockingSession released: toolId={}", toolId);
    }

}
