package org.jwellman.bsh.state.reducers;

import org.jwellman.bsh.state.actions.BshAction;
import org.jwellman.bsh.state.model.BshState;

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
public interface BshReducer {

    /**
     * Compute new state based on current state and an action.
     *
     * @param currentState the current BeanShell state
     * @param action the action being processed
     * @return the new BeanShell state
     */
    BshState reduce(BshState currentState, BshAction action);

}
