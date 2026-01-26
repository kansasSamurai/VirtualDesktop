package org.jwellman.bsh.state.store;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.SwingUtilities;

import org.jwellman.bsh.state.actions.BshAction;
import org.jwellman.bsh.state.model.BshState;
import org.jwellman.bsh.state.reducers.BshReducer;
import org.jwellman.bsh.state.reducers.RootBshReducer;

/**
 * Singleton Redux-style store for BeanShell state.
 *
 * The store:
 * - Holds the single source of truth (BshState)
 * - Dispatches actions through middleware and reducers
 * - Notifies subscribers of state changes on the EDT
 *
 * This store is separate from the desktop AppStore to maintain
 * architectural separation between tools and the desktop container.
 *
 * @author rwellman
 */
public final class BshStore {

    private static BshStore INSTANCE;

    private volatile BshState state;
    private final List<BshSubscriber> subscribers;
    private final List<BshMiddleware> middleware;
    private BshReducer reducer;

    private BshStore() {
        this.state = BshState.initial();
        this.subscribers = new CopyOnWriteArrayList<>();
        this.middleware = new CopyOnWriteArrayList<>();
        // Set default reducer
        this.reducer = new RootBshReducer();
    }

    /**
     * Get the singleton store instance.
     */
    public static synchronized BshStore get() {
        if (INSTANCE == null) {
            INSTANCE = new BshStore();
        }
        return INSTANCE;
    }

    /**
     * Set the root reducer.
     * The default is RootBshReducer.
     */
    public void setReducer(BshReducer reducer) {
        this.reducer = reducer;
    }

    /**
     * Add middleware to the processing chain.
     * Middleware is processed in the order added.
     */
    public void addMiddleware(BshMiddleware mw) {
        middleware.add(mw);
    }

    /**
     * Remove middleware from the processing chain.
     */
    public void removeMiddleware(BshMiddleware mw) {
        middleware.remove(mw);
    }

    /**
     * Dispatch an action to update state.
     *
     * The action passes through middleware, then the reducer computes
     * new state, and subscribers are notified on the EDT.
     *
     * @param action the action to dispatch
     */
    public void dispatch(BshAction action) {
        if (action == null) {
            return;
        }

        // Process through middleware chain
        BshAction processed = action;
        for (BshMiddleware mw : middleware) {
            processed = mw.process(processed, this);
            if (processed == null) {
                // Middleware cancelled the action
                return;
            }
        }

        // Apply reducer if set
        if (reducer != null) {
            final BshState oldState = this.state;
            this.state = reducer.reduce(oldState, processed);

            // Notify subscribers if state changed
            if (!Objects.equals(oldState, this.state)) {
                notifySubscribers();
            }
        }
    }

    /**
     * Get the current BeanShell state.
     */
    public BshState getState() {
        return state;
    }

    /**
     * Subscribe to state changes.
     *
     * @param subscriber the subscriber to add
     * @return a BshSubscription handle for unsubscribing
     */
    public BshSubscription subscribe(BshSubscriber subscriber) {
        subscribers.add(subscriber);
        return () -> subscribers.remove(subscriber);
    }

    /**
     * Notify all subscribers of state change on the EDT.
     */
    private void notifySubscribers() {
        final BshState currentState = this.state;
        if (SwingUtilities.isEventDispatchThread()) {
            for (BshSubscriber sub : subscribers) {
                sub.onStateChanged(currentState);
            }
        } else {
            SwingUtilities.invokeLater(() -> {
                for (BshSubscriber sub : subscribers) {
                    sub.onStateChanged(currentState);
                }
            });
        }
    }

    /**
     * Get the number of subscribers.
     * Useful for debugging.
     */
    public int getSubscriberCount() {
        return subscribers.size();
    }

    /**
     * Get the number of middleware.
     * Useful for debugging.
     */
    public int getMiddlewareCount() {
        return middleware.size();
    }

    /**
     * Reset the store to initial state.
     * Primarily for testing.
     */
    public void reset() {
        this.state = BshState.initial();
        notifySubscribers();
    }

    /**
     * Reset the singleton instance.
     * Primarily for testing.
     */
    public static synchronized void resetInstance() {
        INSTANCE = null;
    }

}
