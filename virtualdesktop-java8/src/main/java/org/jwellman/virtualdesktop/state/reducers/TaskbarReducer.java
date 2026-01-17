package org.jwellman.virtualdesktop.state.reducers;

import org.jwellman.virtualdesktop.state.actions.Action;
import org.jwellman.virtualdesktop.state.actions.ActionTypes;
import org.jwellman.virtualdesktop.state.model.GroupingMode;
import org.jwellman.virtualdesktop.state.model.TaskbarState;

/**
 * Reducer for taskbar-related state changes.
 *
 * @author rwellman
 */
public class TaskbarReducer {

    /**
     * Reduce taskbar state based on the action.
     *
     * @param state current taskbar state
     * @param action the action to process
     * @return new taskbar state
     */
    public TaskbarState reduce(TaskbarState state, Action action) {
        switch (action.getType()) {
            case ActionTypes.TASKBAR_GROUPING_TOGGLED:
                return handleGroupingToggled(state, action);

            case ActionTypes.TASKBAR_TOOL_SELECTED:
                return handleToolSelected(state, action);

            case ActionTypes.TOOL_ACTIVATED:
                return handleToolActivated(state, action);

            case ActionTypes.TOOL_CLOSED:
                return handleToolClosed(state, action);

            default:
                return state;
        }
    }

    private TaskbarState handleGroupingToggled(TaskbarState state, Action action) {
        Object payload = action.getPayload();
        if (payload instanceof Boolean) {
            return state.withGroupingEnabled((Boolean) payload);
        } else if (payload instanceof GroupingMode) {
            return state.withGroupingMode((GroupingMode) payload);
        }
        // Toggle current state
        return state.withGroupingEnabled(!state.isGroupingEnabled());
    }

    private TaskbarState handleToolSelected(TaskbarState state, Action action) {
        Object payload = action.getPayload();
        if (payload instanceof String) {
            return state.withSelectedToolId((String) payload);
        }
        return state;
    }

    private TaskbarState handleToolActivated(TaskbarState state, Action action) {
        Object payload = action.getPayload();
        if (payload instanceof String) {
            return state.withSelectedToolId((String) payload);
        }
        return state;
    }

    private TaskbarState handleToolClosed(TaskbarState state, Action action) {
        // If the closed tool was selected, clear selection
        Object payload = action.getPayload();
        if (payload instanceof String) {
            String toolId = (String) payload;
            if (toolId.equals(state.getSelectedToolId())) {
                return state.withSelectedToolId(null);
            }
        }
        return state;
    }

}
