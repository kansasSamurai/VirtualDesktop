package org.jwellman.virtualdesktop.state.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger(LoggingMiddleware.class);

    private boolean enabled = true;

    public LoggingMiddleware() {
    }

    @Override
    public Action process(Action action, AppStore store) {
        if (enabled) {
            LOG.debug("{} | payload={} | tools={}",
                action.getType(), action.getPayload(),
                store.getState().getTools().getToolCount());
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
