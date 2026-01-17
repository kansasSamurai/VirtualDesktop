package org.jwellman.virtualdesktop.state.store;

import org.jwellman.virtualdesktop.state.model.AppState;

/**
 * Interface for components that subscribe to state changes.
 *
 * Subscribers are notified on the EDT (Event Dispatch Thread)
 * whenever the application state changes.
 *
 * @author rwellman
 */
@FunctionalInterface
public interface StoreSubscriber {

    /**
     * Called when the application state has changed.
     *
     * @param newState the new application state
     */
    void onStateChanged(AppState newState);

}
