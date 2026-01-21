package org.jwellman.console.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jwellman.console.CommandHistory;

/**
 * Default implementation of CommandHistory.
 *
 * <p>Maintains an in-memory list of commands with configurable
 * maximum size. Commands are stored in chronological order.</p>
 *
 * @author Rick Wellman
 */
public class DefaultCommandHistory implements CommandHistory {

    /** Default maximum history size. */
    public static final int DEFAULT_MAX_SIZE = 500;

    private final List<String> history;
    private final int maxSize;
    private int position;
    private String currentLine;

    /**
     * Create a history with default max size.
     */
    public DefaultCommandHistory() {
        this(DEFAULT_MAX_SIZE);
    }

    /**
     * Create a history with specified max size.
     *
     * @param maxSize maximum number of commands to retain
     */
    public DefaultCommandHistory(int maxSize) {
        this.maxSize = maxSize;
        this.history = new ArrayList<String>();
        this.position = 0;
        this.currentLine = "";
    }

    @Override
    public void add(String command) {
        if (command == null || command.trim().isEmpty()) {
            return;
        }

        // Avoid consecutive duplicates
        if (!history.isEmpty() && history.get(history.size() - 1).equals(command)) {
            reset();
            return;
        }

        history.add(command);

        // Trim to max size
        while (history.size() > maxSize) {
            history.remove(0);
        }

        reset();
    }

    @Override
    public String getPrevious() {
        if (history.isEmpty()) {
            return null;
        }

        // Save current line on first navigation
        if (position == 0) {
            // currentLine is saved externally via saveCurrentLine() if needed
        }

        if (position < history.size()) {
            position++;
            return history.get(history.size() - position);
        }

        // Already at oldest entry
        return history.get(0);
    }

    @Override
    public String getNext() {
        if (position <= 0) {
            return null;
        }

        position--;

        if (position == 0) {
            return currentLine;
        }

        return history.get(history.size() - position);
    }

    @Override
    public void reset() {
        position = 0;
        currentLine = "";
    }

    /**
     * Save the current input line before navigating history.
     *
     * <p>This preserves the user's partial input when they press up
     * and then down to return to their current entry.</p>
     *
     * @param line the current input line
     */
    public void saveCurrentLine(String line) {
        this.currentLine = line != null ? line : "";
    }

    @Override
    public List<String> getAll() {
        return Collections.unmodifiableList(new ArrayList<String>(history));
    }

    @Override
    public int size() {
        return history.size();
    }

    @Override
    public void clear() {
        history.clear();
        reset();
    }

    /**
     * Get the maximum history size.
     *
     * @return the maximum number of commands retained
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Pre-populate history with initial commands.
     *
     * <p>Useful for adding convenience commands or
     * loading history from persistence.</p>
     *
     * @param commands commands to add
     */
    public void addAll(List<String> commands) {
        for (String cmd : commands) {
            if (cmd != null && !cmd.trim().isEmpty()) {
                history.add(cmd);
            }
        }
        // Trim to max size
        while (history.size() > maxSize) {
            history.remove(0);
        }
        reset();
    }

}
