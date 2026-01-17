package org.jwellman.virtualdesktop.state.actions;

/**
 * Base interface for all Redux-style actions.
 *
 * Actions are plain objects describing state changes. They are
 * dispatched to the store and processed by reducers.
 *
 * @author rwellman
 */
public interface Action {

    /**
     * Get the action type identifier.
     * @return the action type (e.g., "TOOL_OPENED")
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
