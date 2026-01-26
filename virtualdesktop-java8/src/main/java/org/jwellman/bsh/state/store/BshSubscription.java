package org.jwellman.bsh.state.store;

/**
 * Handle for unsubscribing from BshStore updates.
 *
 * @author rwellman
 */
@FunctionalInterface
public interface BshSubscription {

    /**
     * Unsubscribe from state updates.
     * After calling this, the subscriber will no longer receive notifications.
     */
    void unsubscribe();

}
