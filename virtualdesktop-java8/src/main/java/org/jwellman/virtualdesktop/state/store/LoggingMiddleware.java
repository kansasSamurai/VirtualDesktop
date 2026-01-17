package org.jwellman.virtualdesktop.state.store;

import org.jwellman.virtualdesktop.state.actions.Action;

/**
 * Middleware that logs all dispatched actions to the console.
 *
 * Useful for debugging state changes. Can be enabled/disabled
 * via the enabled flag.
 *
 * @author rwellman
 */
public class LoggingMiddleware implements Middleware {

    private boolean enabled = true;
    private final String prefix;

    public LoggingMiddleware() {
        this("[Redux]");
    }

    public LoggingMiddleware(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Action process(Action action, AppStore store) {
        if (enabled) {
            System.out.println(prefix + " " + action.getType() +
                " | payload=" + action.getPayload() +
                " | tools=" + store.getState().getTools().getToolCount());
        }
        return action;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

}
