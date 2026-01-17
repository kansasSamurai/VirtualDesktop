package org.jwellman.virtualdesktop.state.store;

import org.jwellman.virtualdesktop.state.actions.Action;

/**
 * Middleware interface for intercepting actions before they reach reducers.
 *
 * Middleware can:
 * - Log actions for debugging
 * - Transform actions
 * - Perform side effects
 * - Cancel actions by returning null
 *
 * @author rwellman
 */
public interface Middleware {

    /**
     * Process an action before it reaches the reducer.
     *
     * @param action the action being dispatched
     * @param store the store (for accessing current state)
     * @return the action to continue with, or null to cancel
     */
    Action process(Action action, AppStore store);

}
