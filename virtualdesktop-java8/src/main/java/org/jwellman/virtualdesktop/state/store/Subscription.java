package org.jwellman.virtualdesktop.state.store;

/**
 * Handle for unsubscribing from store updates.
 *
 * @author rwellman
 */
@FunctionalInterface
public interface Subscription {

    /**
     * Unsubscribe from state updates.
     * After calling this, the subscriber will no longer receive notifications.
     */
    void unsubscribe();

}
