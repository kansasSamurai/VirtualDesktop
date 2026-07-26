package org.jwellman.virtualdesktop.state.reducers;

import org.jwellman.virtualdesktop.state.actions.Action;
import org.jwellman.virtualdesktop.state.model.AppState;
import org.jwellman.virtualdesktop.state.model.DesktopState;
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
    private final DesktopReducer desktopReducer;

    public AppReducer() {
        this.toolsReducer = new ToolsReducer();
        this.windowListReducer = new WindowListReducer();
        this.desktopReducer = new DesktopReducer();
    }

    @Override
    public AppState reduce(AppState currentState, Action action) {
        ToolsState newTools = toolsReducer.reduce(currentState.getTools(), action);
        WindowListState newWindowList = windowListReducer.reduce(currentState.getWindowList(), action);
        DesktopState newDesktop = desktopReducer.reduce(currentState.getDesktop(), action);

        // Only create new state if something changed
        if (newTools == currentState.getTools()
                && newWindowList == currentState.getWindowList()
                && newDesktop == currentState.getDesktop()) {
            return currentState;
        }

        return AppState.create(newTools, newWindowList, newDesktop);
    }

}
