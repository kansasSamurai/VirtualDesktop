package org.jwellman.bsh.state.actions;

/**
 * Base interface for all BeanShell Redux-style actions.
 *
 * Actions are plain objects describing state changes. They are
 * dispatched to the store and processed by reducers.
 *
 * This is separate from the desktop Redux (org.jwellman.virtualdesktop.state)
 * to maintain architectural separation between tools and the desktop container.
 *
 * @author rwellman
 */
public interface BshAction {

    /**
     * Get the action type identifier.
     * @return the action type (e.g., "WORKSPACE_CREATED")
     */
    String getType();

    /**
     * Get the timestamp when this action was created.
     * @return timestamp in milliseconds
     */
    long getTimestamp();

    /**
     * Get the action payload containing event-specific data.
     * @return the payload object, or null if no payload
     */
    Object getPayload();

}
