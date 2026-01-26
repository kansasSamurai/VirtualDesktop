package org.jwellman.bsh.state.reducers;

import org.jwellman.bsh.state.actions.BshAction;
import org.jwellman.bsh.state.model.BshState;
import org.jwellman.bsh.state.model.InterpreterState;
import org.jwellman.bsh.state.model.WorkspaceState;

/**
 * Root reducer that composes sub-reducers for different state slices.
 *
 * Delegates to WorkspaceReducer and InterpreterReducer for their
 * respective state domains.
 *
 * @author rwellman
 */
public class RootBshReducer implements BshReducer {

    private final WorkspaceReducer workspaceReducer;
    private final InterpreterReducer interpreterReducer;

    public RootBshReducer() {
        this.workspaceReducer = new WorkspaceReducer();
        this.interpreterReducer = new InterpreterReducer();
    }

    @Override
    public BshState reduce(BshState currentState, BshAction action) {
        WorkspaceState newWorkspaces = workspaceReducer.reduce(currentState.getWorkspaces(), action);
        InterpreterState newInterpreter = interpreterReducer.reduce(currentState.getInterpreter(), action);

        // Only create new state if something changed
        if (newWorkspaces == currentState.getWorkspaces() &&
            newInterpreter == currentState.getInterpreter()) {
            return currentState;
        }

        return BshState.create(newWorkspaces, newInterpreter);
    }

}
