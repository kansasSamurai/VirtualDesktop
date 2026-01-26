package org.jwellman.bsh.state.actions;

/**
 * Constants for all action types in the BeanShell state domain.
 *
 * Action types follow SCREAMING_SNAKE_CASE convention.
 *
 * @author rwellman
 */
public final class BshActionTypes {

    private BshActionTypes() {
        // Prevent instantiation
    }

    // ========== Workspace Actions ==========

    /** Dispatched when a new workspace is created */
    public static final String WORKSPACE_CREATED = "WORKSPACE_CREATED";

    /** Dispatched when a workspace is removed */
    public static final String WORKSPACE_DELETED = "WORKSPACE_DELETED";

    /** Dispatched when the active workspace changes */
    public static final String WORKSPACE_SWITCHED = "WORKSPACE_SWITCHED";

    /** Dispatched when a workspace is reset to clean state */
    public static final String WORKSPACE_RESET = "WORKSPACE_RESET";

    // ========== Interpreter Actions ==========

    /** Dispatched when the interpreter is first created */
    public static final String INTERPRETER_INITIALIZED = "INTERPRETER_INITIALIZED";

    /** Dispatched when the interpreter is cleared/reset */
    public static final String INTERPRETER_RESET = "INTERPRETER_RESET";

    /** Dispatched when the current namespace changes */
    public static final String NAMESPACE_CHANGED = "NAMESPACE_CHANGED";

    /** Dispatched when a variable is added/updated in namespace */
    public static final String VARIABLE_SET = "VARIABLE_SET";

    /** Dispatched when a variable is removed from namespace */
    public static final String VARIABLE_REMOVED = "VARIABLE_REMOVED";

    // ========== Script Execution Actions ==========

    /** Dispatched when script execution begins */
    public static final String SCRIPT_STARTED = "SCRIPT_STARTED";

    /** Dispatched when a single line is executed (for ScriptTester) */
    public static final String SCRIPT_LINE_EXECUTED = "SCRIPT_LINE_EXECUTED";

    /** Dispatched when script finishes successfully */
    public static final String SCRIPT_COMPLETED = "SCRIPT_COMPLETED";

    /** Dispatched when script execution fails */
    public static final String SCRIPT_ERROR = "SCRIPT_ERROR";

}
