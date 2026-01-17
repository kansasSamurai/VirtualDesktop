package org.jwellman.virtualdesktop.state.reducers;

import org.jwellman.virtualdesktop.state.actions.Action;
import org.jwellman.virtualdesktop.state.model.AppState;
import org.jwellman.virtualdesktop.state.model.TaskbarState;
import org.jwellman.virtualdesktop.state.model.ToolsState;

/**
 * Root reducer that composes sub-reducers for different state slices.
 *
 * @author rwellman
 */
public class AppReducer implements Reducer {

    private final ToolsReducer toolsReducer;
    private final TaskbarReducer taskbarReducer;

    public AppReducer() {
        this.toolsReducer = new ToolsReducer();
        this.taskbarReducer = new TaskbarReducer();
    }

    @Override
    public AppState reduce(AppState currentState, Action action) {
        ToolsState newTools = toolsReducer.reduce(currentState.getTools(), action);
        TaskbarState newTaskbar = taskbarReducer.reduce(currentState.getTaskbar(), action);

        // Only create new state if something changed
        if (newTools == currentState.getTools() && newTaskbar == currentState.getTaskbar()) {
            return currentState;
        }

        return AppState.create(newTools, newTaskbar);
    }

}
