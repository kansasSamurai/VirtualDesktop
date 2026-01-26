package org.jwellman.bsh.state.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable state representing all workspaces.
 *
 * Maintains a map of workspaces by ID and tracks the active workspace.
 *
 * @author rwellman
 */
public final class WorkspaceState {

    /** ID of the global workspace */
    public static final String GLOBAL_WORKSPACE_ID = "global";

    private final Map<String, WorkspaceInfo> workspacesById;
    private final String activeWorkspaceId;
    private final String globalNamespaceId;

    private WorkspaceState(Map<String, WorkspaceInfo> workspacesById,
                           String activeWorkspaceId,
                           String globalNamespaceId) {
        this.workspacesById = Collections.unmodifiableMap(new LinkedHashMap<>(workspacesById));
        this.activeWorkspaceId = activeWorkspaceId;
        this.globalNamespaceId = globalNamespaceId;
    }

    /**
     * Create initial workspace state with just the global workspace.
     */
    public static WorkspaceState initial() {
        Map<String, WorkspaceInfo> workspaces = new LinkedHashMap<>();
        WorkspaceInfo global = WorkspaceInfo.createGlobal();
        workspaces.put(GLOBAL_WORKSPACE_ID, global);
        return new WorkspaceState(workspaces, GLOBAL_WORKSPACE_ID, GLOBAL_WORKSPACE_ID);
    }

    // ========== Getters ==========

    public Map<String, WorkspaceInfo> getWorkspacesById() {
        return workspacesById;
    }

    public String getActiveWorkspaceId() {
        return activeWorkspaceId;
    }

    public String getGlobalNamespaceId() {
        return globalNamespaceId;
    }

    public WorkspaceInfo getWorkspace(String workspaceId) {
        return workspacesById.get(workspaceId);
    }

    public WorkspaceInfo getActiveWorkspace() {
        return workspacesById.get(activeWorkspaceId);
    }

    public int getWorkspaceCount() {
        return workspacesById.size();
    }

    public boolean isGlobalActive() {
        return GLOBAL_WORKSPACE_ID.equals(activeWorkspaceId);
    }

    // ========== Copy-on-write modifiers ==========

    public WorkspaceState withWorkspaceAdded(WorkspaceInfo workspace) {
        Map<String, WorkspaceInfo> newWorkspaces = new LinkedHashMap<>(workspacesById);
        newWorkspaces.put(workspace.getId(), workspace);
        return new WorkspaceState(newWorkspaces, activeWorkspaceId, globalNamespaceId);
    }

    public WorkspaceState withWorkspaceRemoved(String workspaceId) {
        if (GLOBAL_WORKSPACE_ID.equals(workspaceId)) {
            // Cannot remove global workspace
            return this;
        }
        if (!workspacesById.containsKey(workspaceId)) {
            return this;
        }

        Map<String, WorkspaceInfo> newWorkspaces = new LinkedHashMap<>(workspacesById);
        newWorkspaces.remove(workspaceId);

        // If removing the active workspace, switch to global
        String newActiveId = activeWorkspaceId;
        if (workspaceId.equals(activeWorkspaceId)) {
            newActiveId = GLOBAL_WORKSPACE_ID;
        }

        return new WorkspaceState(newWorkspaces, newActiveId, globalNamespaceId);
    }

    public WorkspaceState withActiveWorkspace(String workspaceId) {
        if (!workspacesById.containsKey(workspaceId)) {
            return this;
        }
        if (workspaceId.equals(activeWorkspaceId)) {
            return this;
        }
        return new WorkspaceState(workspacesById, workspaceId, globalNamespaceId);
    }

    public WorkspaceState withWorkspaceUpdated(WorkspaceInfo workspace) {
        if (!workspacesById.containsKey(workspace.getId())) {
            return this;
        }
        Map<String, WorkspaceInfo> newWorkspaces = new LinkedHashMap<>(workspacesById);
        newWorkspaces.put(workspace.getId(), workspace);
        return new WorkspaceState(newWorkspaces, activeWorkspaceId, globalNamespaceId);
    }

    @Override
    public String toString() {
        return "WorkspaceState{count=" + workspacesById.size() + ", active=" + activeWorkspaceId + "}";
    }

}
