package org.jwellman.virtualdesktop.state.store;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.SwingUtilities;

import org.jwellman.virtualdesktop.state.actions.Action;
import org.jwellman.virtualdesktop.state.model.AppState;
import org.jwellman.virtualdesktop.state.reducers.Reducer;

/**
 * Singleton Redux-style store for application state.
 *
 * The store:
 * - Holds the single source of truth (AppState)
 * - Dispatches actions through middleware and reducers
 * - Notifies subscribers of state changes on the EDT
 *
 * @author rwellman
 */
public final class AppStore {

    private static AppStore INSTANCE;

    private volatile AppState state;
    private final List<StoreSubscriber> subscribers;
    private final List<Middleware> middleware;
    private Reducer reducer;

    private AppStore() {
        this.state = AppState.initial();
        this.subscribers = new CopyOnWriteArrayList<>();
        this.middleware = new CopyOnWriteArrayList<>();
    }

    /**
     * Get the singleton store instance.
     */
    public static synchronized AppStore get() {
        if (INSTANCE == null) {
            INSTANCE = new AppStore();
        }
        return INSTANCE;
    }

    /**
     * Set the root reducer.
     * Should be called during application initialization.
     */
    public void setReducer(Reducer reducer) {
        this.reducer = reducer;
    }

    /**
     * Add middleware to the processing chain.
     * Middleware is processed in the order added.
     */
    public void addMiddleware(Middleware mw) {
        middleware.add(mw);
    }

    /**
     * Dispatch an action to update state.
     *
     * The action passes through middleware, then the reducer computes
     * new state, and subscribers are notified on the EDT.
     *
     * @param action the action to dispatch
     */
    public void dispatch(Action action) {
        if (action == null) {
            return;
        }

        // Process through middleware chain
        Action processed = action;
        for (Middleware mw : middleware) {
            processed = mw.process(processed, this);
            if (processed == null) {
                // Middleware cancelled the action
                return;
            }
        }

        // Apply reducer if set
        if (reducer != null) {
            final AppState oldState = this.state;
            this.state = reducer.reduce(oldState, processed);

            // Notify subscribers if state changed
            if (!Objects.equals(oldState, this.state)) {
                notifySubscribers();
            }
        }
    }

    /**
     * Get the current application state.
     */
    public AppState getState() {
        return state;
    }

    /**
     * Subscribe to state changes.
     *
     * @param subscriber the subscriber to add
     * @return a Subscription handle for unsubscribing
     */
    public Subscription subscribe(StoreSubscriber subscriber) {
        subscribers.add(subscriber);
        return () -> subscribers.remove(subscriber);
    }

    /**
     * Notify all subscribers of state change on the EDT.
     */
    private void notifySubscribers() {
        final AppState currentState = this.state;
        if (SwingUtilities.isEventDispatchThread()) {
            for (StoreSubscriber sub : subscribers) {
                sub.onStateChanged(currentState);
            }
        } else {
            SwingUtilities.invokeLater(() -> {
                for (StoreSubscriber sub : subscribers) {
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
     * Reset the store to initial state.
     * Primarily for testing.
     */
    public void reset() {
        this.state = AppState.initial();
        notifySubscribers();
    }

}
