package org.jwellman.virtualdesktop.state.reducers;

import org.jwellman.virtualdesktop.state.actions.Action;
import org.jwellman.virtualdesktop.state.actions.ActionTypes;
import org.jwellman.virtualdesktop.state.actions.SimpleAction;
import org.jwellman.virtualdesktop.state.model.FrameState;
import org.jwellman.virtualdesktop.state.model.ToolInstance;
import org.jwellman.virtualdesktop.state.model.ToolsState;

/**
 * Reducer for tools-related state changes.
 *
 * @author rwellman
 */
public class ToolsReducer {

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
                tp.workspaceId
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

}
