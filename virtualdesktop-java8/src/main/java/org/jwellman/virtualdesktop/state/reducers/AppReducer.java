package org.jwellman.virtualdesktop.state.reducers;

import org.jwellman.virtualdesktop.state.actions.Action;
import org.jwellman.virtualdesktop.state.model.AppState;
import org.jwellman.virtualdesktop.state.model.WindowListState;
import org.jwellman.virtualdesktop.state.model.ToolsState;

/**
 * Root reducer that composes sub-reducers for different state slices.
 *
 * @author rwellman
 */
public class AppReducer implements Reducer {

    private final ToolsReducer toolsReducer;
    private final WindowListReducer windowListReducer;

    public AppReducer() {
        this.toolsReducer = new ToolsReducer();
        this.windowListReducer = new WindowListReducer();
    }

    @Override
    public AppState reduce(AppState currentState, Action action) {
        ToolsState newTools = toolsReducer.reduce(currentState.getTools(), action);
        WindowListState newWindowList = windowListReducer.reduce(currentState.getWindowList(), action);

        // Only create new state if something changed
        if (newTools == currentState.getTools() && newWindowList == currentState.getWindowList()) {
            return currentState;
        }

        return AppState.create(newTools, newWindowList);
    }

}
