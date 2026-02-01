package org.jwellman.bsh.state.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jwellman.bsh.state.actions.BshAction;

/**
 * Middleware that logs all dispatched BeanShell actions to the console.
 *
 * Useful for debugging state changes. Can be enabled/disabled
 * via the enabled flag.
 *
 * @author rwellman
 */
public class BshLoggingMiddleware implements BshMiddleware {

    private static final Logger LOG = LoggerFactory.getLogger(BshLoggingMiddleware.class);

    private boolean enabled = true;

    public BshLoggingMiddleware() {
    }

    @Override
    public BshAction process(BshAction action, BshStore store) {
        if (enabled) {
            LOG.debug("{} | payload={} | workspaces={} | interpreter={}",
                action.getType(), action.getPayload(),
                store.getState().getWorkspaces().getWorkspaceCount(),
                store.getState().getInterpreter().isInitialized());
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
