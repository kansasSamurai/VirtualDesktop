package org.jwellman.virtualdesktop.state.reducers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jwellman.virtualdesktop.state.actions.Action;
import org.jwellman.virtualdesktop.state.actions.ActionTypes;
import org.jwellman.virtualdesktop.state.actions.SimpleAction;
import org.jwellman.virtualdesktop.state.model.DockingState;
import org.jwellman.virtualdesktop.state.model.FrameState;
import org.jwellman.virtualdesktop.state.model.ToolInstance;
import org.jwellman.virtualdesktop.state.model.ToolsState;

/**
 * Reducer for tools-related state changes.
 *
 * @author rwellman
 */
public class ToolsReducer {

    private static final Logger LOG = LoggerFactory.getLogger(ToolsReducer.class);

    /**
     * Reduce tools state based on the action.
     *
     * @param state current tools state
     * @param action the action to process
     * @return new tools state
     */
    public ToolsState reduce(ToolsState state, Action action) {
        switch (action.getType()) {
            case ActionTypes.TOOL_OPENED:
                return handleToolOpened(state, action);

            case ActionTypes.TOOL_CLOSED:
                return handleToolClosed(state, action);

            case ActionTypes.TOOL_MINIMIZED:
                return handleFrameStateChange(state, action, FrameState.MINIMIZED);

            case ActionTypes.TOOL_RESTORED:
                return handleFrameStateChange(state, action, FrameState.NORMAL);

            case ActionTypes.TOOL_MAXIMIZED:
                return handleFrameStateChange(state, action, FrameState.MAXIMIZED);

            case ActionTypes.TOOL_ACTIVATED:
                // Tool activated doesn't change tools state, just taskbar
                return state;

            case ActionTypes.TOOL_DEACTIVATED:
                // Tool deactivated doesn't change tools state
                return state;

            case ActionTypes.PANEL_DOCKED_IN:
                return handlePanelDockedIn(state, action);

            case ActionTypes.PANEL_DOCKED_OUT:
                return handlePanelDockedOut(state, action);

            case ActionTypes.PANEL_LOCATION_CHANGED:
                return handlePanelLocationChanged(state, action);

            default:
                return state;
        }
    }

    private ToolsState handleToolOpened(ToolsState state, Action action) {
        Object payload = action.getPayload();
        if (payload instanceof SimpleAction.ToolPayload) {
            SimpleAction.ToolPayload tp = (SimpleAction.ToolPayload) payload;
            ToolInstance tool = ToolInstance.create(
                tp.toolId,
                tp.toolType,
                tp.title,
                tp.workspaceId,
                tp.definitionId,
                tp.iconKey
            );
            return state.withToolAdded(tool);
        }
        return state;
    }

    private ToolsState handleToolClosed(ToolsState state, Action action) {
        Object payload = action.getPayload();
        if (payload instanceof String) {
            String toolId = (String) payload;
            return state.withToolRemoved(toolId);
        }
        return state;
    }

    private ToolsState handleFrameStateChange(ToolsState state, Action action, FrameState newFrameState) {
        Object payload = action.getPayload();
        if (payload instanceof String) {
            String toolId = (String) payload;
            ToolInstance tool = state.getTool(toolId);
            if (tool != null) {
                return state.withToolUpdated(tool.withFrameState(newFrameState));
            }
        }
        return state;
    }

    // ========== Docking Handlers ==========

    private ToolsState handlePanelDockedIn(ToolsState state, Action action) {
        Object payload = action.getPayload();
        if (payload instanceof SimpleAction.DockingPayload) {
            SimpleAction.DockingPayload dp = (SimpleAction.DockingPayload) payload;
            // Update the target tool's docking state to show it has external content
            ToolInstance targetTool = state.getTool(dp.targetToolId);
            if (targetTool != null) {
                DockingState newDockingState = targetTool.getDockingState()
                    .withPanelDockedIn(dp.panelId);
                return state.withToolUpdated(targetTool.withDockingState(newDockingState));
            }
        }
        return state;
    }

    private ToolsState handlePanelDockedOut(ToolsState state, Action action) {
        Object payload = action.getPayload();
        if (payload instanceof SimpleAction.DockingPayload) {
            SimpleAction.DockingPayload dp = (SimpleAction.DockingPayload) payload;
            // Update the source tool's docking state to show original panel is gone
            ToolInstance sourceTool = state.getTool(dp.sourceToolId);
            if (sourceTool != null) {
                DockingState newDockingState = sourceTool.getDockingState()
                    .withOriginalPanelPresent(false);
                return state.withToolUpdated(sourceTool.withDockingState(newDockingState));
            }
        }
        return state;
    }

    private ToolsState handlePanelLocationChanged(ToolsState state, Action action) {
        Object payload = action.getPayload();
        if (payload instanceof SimpleAction.DockingPayload) {
            SimpleAction.DockingPayload dp = (SimpleAction.DockingPayload) payload;
            // Log the location change - full implementation would update workspace tracking
            LOG.debug("Panel {} moved to workspace: {}", dp.panelId, dp.targetToolId);
        }
        return state;
    }

}
