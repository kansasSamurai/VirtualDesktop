package org.jwellman.bsh.state.model;

/**
 * Immutable state representing the BeanShell interpreter context.
 *
 * Tracks interpreter lifecycle state and namespace information.
 *
 * @author rwellman
 */
public final class InterpreterState {

    private final boolean initialized;
    private final String currentNamespacePath;
    private final int variableCount;

    private InterpreterState(boolean initialized, String currentNamespacePath, int variableCount) {
        this.initialized = initialized;
        this.currentNamespacePath = currentNamespacePath;
        this.variableCount = variableCount;
    }

    /**
     * Create initial interpreter state (not yet initialized).
     */
    public static InterpreterState initial() {
        return new InterpreterState(false, "/", 0);
    }

    /**
     * Create initialized interpreter state.
     */
    public static InterpreterState initialized() {
        return new InterpreterState(true, "/", 0);
    }

    // ========== Getters ==========

    public boolean isInitialized() {
        return initialized;
    }

    public String getCurrentNamespacePath() {
        return currentNamespacePath;
    }

    public int getVariableCount() {
        return variableCount;
    }

    // ========== Copy-on-write modifiers ==========

    public InterpreterState withInitialized(boolean newInitialized) {
        return new InterpreterState(newInitialized, currentNamespacePath, variableCount);
    }

    public InterpreterState withNamespacePath(String newPath) {
        return new InterpreterState(initialized, newPath, variableCount);
    }

    public InterpreterState withVariableCount(int newCount) {
        return new InterpreterState(initialized, currentNamespacePath, newCount);
    }

    public InterpreterState incrementVariableCount() {
        return new InterpreterState(initialized, currentNamespacePath, variableCount + 1);
    }

    public InterpreterState decrementVariableCount() {
        return new InterpreterState(initialized, currentNamespacePath, Math.max(0, variableCount - 1));
    }

    public InterpreterState reset() {
        return new InterpreterState(false, "/", 0);
    }

    @Override
    public String toString() {
        return "InterpreterState{initialized=" + initialized +
               ", namespace=" + currentNamespacePath +
               ", variables=" + variableCount + "}";
    }

}
