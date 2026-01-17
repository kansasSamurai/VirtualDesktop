package org.jwellman.virtualdesktop.state.reducers;

import org.jwellman.virtualdesktop.state.actions.Action;
import org.jwellman.virtualdesktop.state.model.AppState;

/**
 * Interface for reducers that compute new state from current state and an action.
 *
 * Reducers must be pure functions:
 * - No side effects
 * - Same inputs always produce same outputs
 * - Do not mutate the input state
 *
 * @author rwellman
 */
public interface Reducer {

    /**
     * Compute new state based on current state and an action.
     *
     * @param currentState the current application state
     * @param action the action being processed
     * @return the new application state
     */
    AppState reduce(AppState currentState, Action action);

}
