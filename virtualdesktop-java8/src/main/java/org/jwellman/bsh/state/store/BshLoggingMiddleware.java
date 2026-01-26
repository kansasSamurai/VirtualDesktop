package org.jwellman.bsh.state.store;

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

    private boolean enabled = true;
    private final String prefix;

    public BshLoggingMiddleware() {
        this("[BshRedux]");
    }

    public BshLoggingMiddleware(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public BshAction process(BshAction action, BshStore store) {
        if (enabled) {
            System.out.println(prefix + " " + action.getType() +
                " | payload=" + action.getPayload() +
                " | workspaces=" + store.getState().getWorkspaces().getWorkspaceCount() +
                " | interpreter=" + store.getState().getInterpreter().isInitialized());
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
