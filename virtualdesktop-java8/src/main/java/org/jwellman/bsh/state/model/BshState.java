package org.jwellman.bsh.state.model;

/**
 * Root immutable state for the BeanShell domain.
 *
 * This is the single source of truth for BeanShell-related state,
 * separate from the desktop application state.
 *
 * @author rwellman
 */
public final class BshState {

    private final WorkspaceState workspaces;
    private final InterpreterState interpreter;
    private final long timestamp;

    private BshState(WorkspaceState workspaces, InterpreterState interpreter, long timestamp) {
        this.workspaces = workspaces;
        this.interpreter = interpreter;
        this.timestamp = timestamp;
    }

    /**
     * Create the initial BeanShell state.
     */
    public static BshState initial() {
        return new BshState(
            WorkspaceState.initial(),
            InterpreterState.initial(),
            System.currentTimeMillis()
        );
    }

    /**
     * Create a new state with updated components.
     */
    public static BshState create(WorkspaceState workspaces, InterpreterState interpreter) {
        return new BshState(workspaces, interpreter, System.currentTimeMillis());
    }

    // ========== Getters ==========

    public WorkspaceState getWorkspaces() {
        return workspaces;
    }

    public InterpreterState getInterpreter() {
        return interpreter;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // ========== Copy-on-write modifiers ==========

    public BshState withWorkspaces(WorkspaceState newWorkspaces) {
        return new BshState(newWorkspaces, interpreter, System.currentTimeMillis());
    }

    public BshState withInterpreter(InterpreterState newInterpreter) {
        return new BshState(workspaces, newInterpreter, System.currentTimeMillis());
    }

    @Override
    public String toString() {
        return "BshState{workspaces=" + workspaces.getWorkspaceCount() +
               ", interpreter=" + interpreter + "}";
    }

}
