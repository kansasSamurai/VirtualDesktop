package org.jwellman.bsh.state.reducers;

import org.jwellman.bsh.state.actions.BshAction;
import org.jwellman.bsh.state.actions.BshActionTypes;
import org.jwellman.bsh.state.actions.BshActions;
import org.jwellman.bsh.state.model.WorkspaceInfo;
import org.jwellman.bsh.state.model.WorkspaceState;

/**
 * Reducer for workspace-related state changes.
 *
 * Handles workspace creation, deletion, switching, and reset actions.
 *
 * @author rwellman
 */
public class WorkspaceReducer {

    /**
     * Reduce workspace state based on the given action.
     *
     * @param currentState the current workspace state
     * @param action the action being processed
     * @return the new workspace state (may be same instance if no change)
     */
    public WorkspaceState reduce(WorkspaceState currentState, BshAction action) {
        String type = action.getType();

        if (BshActionTypes.WORKSPACE_CREATED.equals(type)) {
            return handleWorkspaceCreated(currentState, action);
        } else if (BshActionTypes.WORKSPACE_DELETED.equals(type)) {
            return handleWorkspaceDeleted(currentState, action);
        } else if (BshActionTypes.WORKSPACE_SWITCHED.equals(type)) {
            return handleWorkspaceSwitched(currentState, action);
        } else if (BshActionTypes.WORKSPACE_RESET.equals(type)) {
            return handleWorkspaceReset(currentState, action);
        }

        return currentState;
    }

    private WorkspaceState handleWorkspaceCreated(WorkspaceState state, BshAction action) {
        Object payload = action.getPayload();
        if (payload instanceof BshActions.WorkspaceCreatedPayload) {
            BshActions.WorkspaceCreatedPayload p = (BshActions.WorkspaceCreatedPayload) payload;
            WorkspaceInfo workspace = WorkspaceInfo.create(p.workspaceId, p.name, p.isSandbox);
            return state.withWorkspaceAdded(workspace);
        }
        return state;
    }

    private WorkspaceState handleWorkspaceDeleted(WorkspaceState state, BshAction action) {
        Object payload = action.getPayload();
        if (payload instanceof String) {
            String workspaceId = (String) payload;
            return state.withWorkspaceRemoved(workspaceId);
        }
        return state;
    }

    private WorkspaceState handleWorkspaceSwitched(WorkspaceState state, BshAction action) {
        Object payload = action.getPayload();
        if (payload instanceof String) {
            String workspaceId = (String) payload;
            return state.withActiveWorkspace(workspaceId);
        }
        return state;
    }

    private WorkspaceState handleWorkspaceReset(WorkspaceState state, BshAction action) {
        // Reset switches back to global workspace
        return state.withActiveWorkspace(WorkspaceState.GLOBAL_WORKSPACE_ID);
    }

}
