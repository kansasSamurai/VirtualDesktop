package org.jwellman.console;

import java.util.List;

/**
 * Interface for managing command history in a console.
 *
 * <p>Provides navigation through previously entered commands
 * and persistence support.</p>
 *
 * @author Rick Wellman
 */
public interface CommandHistory {

    /**
     * Add a command to the history.
     *
     * <p>Typically called after a command has been executed.
     * Implementations may deduplicate or limit history size.</p>
     *
     * @param command the command to add
     */
    void add(String command);

    /**
     * Get the previous command in history (older).
     *
     * <p>Navigation moves backward through history.
     * Returns null if at the beginning of history.</p>
     *
     * @return the previous command, or null if none
     */
    String getPrevious();

    /**
     * Get the next command in history (newer).
     *
     * <p>Navigation moves forward through history.
     * Returns null if at the end of history (current input).</p>
     *
     * @return the next command, or null if at end
     */
    String getNext();

    /**
     * Reset navigation position to the end (most recent).
     *
     * <p>Typically called when starting a new command entry.</p>
     */
    void reset();

    /**
     * Get all commands in history.
     *
     * <p>Returns commands in chronological order (oldest first).</p>
     *
     * @return list of all commands
     */
    List<String> getAll();

    /**
     * Get the current size of the history.
     *
     * @return the number of commands in history
     */
    int size();

    /**
     * Clear all history.
     */
    void clear();

}
