package org.jwellman.bsh.state.store;

import org.jwellman.bsh.state.actions.BshAction;

/**
 * Middleware interface for intercepting BeanShell actions before they reach reducers.
 *
 * Middleware can:
 * - Log actions for debugging
 * - Transform actions
 * - Perform side effects
 * - Cancel actions by returning null
 *
 * @author rwellman
 */
public interface BshMiddleware {

    /**
     * Process an action before it reaches the reducer.
     *
     * @param action the action being dispatched
     * @param store the store (for accessing current state)
     * @return the action to continue with, or null to cancel
     */
    BshAction process(BshAction action, BshStore store);

}
