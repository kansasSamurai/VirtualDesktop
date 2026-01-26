package org.jwellman.bsh.state.reducers;

import org.jwellman.bsh.state.actions.BshAction;
import org.jwellman.bsh.state.actions.BshActionTypes;
import org.jwellman.bsh.state.model.InterpreterState;

/**
 * Reducer for interpreter-related state changes.
 *
 * Handles interpreter initialization, reset, and namespace changes.
 *
 * @author rwellman
 */
public class InterpreterReducer {

    /**
     * Reduce interpreter state based on the given action.
     *
     * @param currentState the current interpreter state
     * @param action the action being processed
     * @return the new interpreter state (may be same instance if no change)
     */
    public InterpreterState reduce(InterpreterState currentState, BshAction action) {
        String type = action.getType();

        if (BshActionTypes.INTERPRETER_INITIALIZED.equals(type)) {
            return handleInterpreterInitialized(currentState);
        } else if (BshActionTypes.INTERPRETER_RESET.equals(type)) {
            return handleInterpreterReset(currentState);
        } else if (BshActionTypes.NAMESPACE_CHANGED.equals(type)) {
            return handleNamespaceChanged(currentState, action);
        } else if (BshActionTypes.VARIABLE_SET.equals(type)) {
            return handleVariableSet(currentState);
        } else if (BshActionTypes.VARIABLE_REMOVED.equals(type)) {
            return handleVariableRemoved(currentState);
        }

        return currentState;
    }

    private InterpreterState handleInterpreterInitialized(InterpreterState state) {
        if (state.isInitialized()) {
            return state;
        }
        return state.withInitialized(true);
    }

    private InterpreterState handleInterpreterReset(InterpreterState state) {
        return state.reset();
    }

    private InterpreterState handleNamespaceChanged(InterpreterState state, BshAction action) {
        Object payload = action.getPayload();
        if (payload instanceof String) {
            String namespacePath = (String) payload;
            return state.withNamespacePath(namespacePath);
        }
        return state;
    }

    private InterpreterState handleVariableSet(InterpreterState state) {
        return state.incrementVariableCount();
    }

    private InterpreterState handleVariableRemoved(InterpreterState state) {
        return state.decrementVariableCount();
    }

}
