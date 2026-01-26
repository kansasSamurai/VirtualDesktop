package org.jwellman.bsh.state.store;

import org.jwellman.bsh.state.model.BshState;

/**
 * Interface for components that subscribe to BeanShell state changes.
 *
 * Subscribers are notified on the EDT (Event Dispatch Thread)
 * whenever the BeanShell state changes.
 *
 * @author rwellman
 */
@FunctionalInterface
public interface BshSubscriber {

    /**
     * Called when the BeanShell state has changed.
     *
     * @param newState the new BeanShell state
     */
    void onStateChanged(BshState newState);

}
