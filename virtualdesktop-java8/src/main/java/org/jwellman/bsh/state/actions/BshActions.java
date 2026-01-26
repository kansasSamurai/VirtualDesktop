package org.jwellman.bsh.state.actions;

/**
 * Factory class for creating BeanShell Redux actions with type-safe payloads.
 *
 * Provides static factory methods for creating common actions
 * and contains payload classes for each action type.
 *
 * @author rwellman
 */
public final class BshActions {

    private BshActions() {
        // Prevent instantiation
    }

    // ========== Factory Methods for Workspace Actions ==========

    /**
     * Create an action for workspace creation.
     */
    public static BshAction workspaceCreated(String workspaceId, String name, boolean isSandbox) {
        return new SimpleBshAction(BshActionTypes.WORKSPACE_CREATED,
            new WorkspaceCreatedPayload(workspaceId, name, isSandbox));
    }

    /**
     * Create an action for workspace deletion.
     */
    public static BshAction workspaceDeleted(String workspaceId) {
        return new SimpleBshAction(BshActionTypes.WORKSPACE_DELETED, workspaceId);
    }

    /**
     * Create an action for workspace switch.
     */
    public static BshAction workspaceSwitched(String workspaceId) {
        return new SimpleBshAction(BshActionTypes.WORKSPACE_SWITCHED, workspaceId);
    }

    /**
     * Create an action for workspace reset.
     */
    public static BshAction workspaceReset(String workspaceId) {
        return new SimpleBshAction(BshActionTypes.WORKSPACE_RESET, workspaceId);
    }

    // ========== Factory Methods for Interpreter Actions ==========

    /**
     * Create an action for interpreter initialization.
     */
    public static BshAction interpreterInitialized() {
        return new SimpleBshAction(BshActionTypes.INTERPRETER_INITIALIZED);
    }

    /**
     * Create an action for interpreter reset.
     */
    public static BshAction interpreterReset() {
        return new SimpleBshAction(BshActionTypes.INTERPRETER_RESET);
    }

    /**
     * Create an action for namespace change.
     */
    public static BshAction namespaceChanged(String namespacePath) {
        return new SimpleBshAction(BshActionTypes.NAMESPACE_CHANGED, namespacePath);
    }

    /**
     * Create an action for variable set.
     */
    public static BshAction variableSet(String variableName, String typeName) {
        return new SimpleBshAction(BshActionTypes.VARIABLE_SET,
            new VariablePayload(variableName, typeName));
    }

    /**
     * Create an action for variable removal.
     */
    public static BshAction variableRemoved(String variableName) {
        return new SimpleBshAction(BshActionTypes.VARIABLE_REMOVED, variableName);
    }

    // ========== Factory Methods for Script Execution Actions ==========

    /**
     * Create an action for script start.
     */
    public static BshAction scriptStarted(String scriptId, String scriptName, int totalLines) {
        return new SimpleBshAction(BshActionTypes.SCRIPT_STARTED,
            new ScriptStartedPayload(scriptId, scriptName, totalLines));
    }

    /**
     * Create an action for script line execution.
     */
    public static BshAction scriptLineExecuted(String scriptId, int lineNumber, String lineContent) {
        return new SimpleBshAction(BshActionTypes.SCRIPT_LINE_EXECUTED,
            new ScriptLinePayload(scriptId, lineNumber, lineContent));
    }

    /**
     * Create an action for script completion.
     */
    public static BshAction scriptCompleted(String scriptId, Object result) {
        return new SimpleBshAction(BshActionTypes.SCRIPT_COMPLETED,
            new ScriptCompletedPayload(scriptId, result));
    }

    /**
     * Create an action for script error.
     */
    public static BshAction scriptError(String scriptId, int lineNumber, String errorMessage) {
        return new SimpleBshAction(BshActionTypes.SCRIPT_ERROR,
            new ScriptErrorPayload(scriptId, lineNumber, errorMessage));
    }

    // ========== Simple Action Implementation ==========

    /**
     * Simple implementation of BshAction.
     */
    private static final class SimpleBshAction implements BshAction {

        private final String type;
        private final long timestamp;
        private final Object payload;

        SimpleBshAction(String type, Object payload) {
            this.type = type;
            this.timestamp = System.currentTimeMillis();
            this.payload = payload;
        }

        SimpleBshAction(String type) {
            this(type, null);
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public Object getPayload() {
            return payload;
        }

        @Override
        public String toString() {
            return "BshAction{type='" + type + "', payload=" + payload + "}";
        }
    }

    // ========== Payload Classes ==========

    /**
     * Payload for WORKSPACE_CREATED action.
     */
    public static final class WorkspaceCreatedPayload {
        public final String workspaceId;
        public final String name;
        public final boolean isSandbox;

        public WorkspaceCreatedPayload(String workspaceId, String name, boolean isSandbox) {
            this.workspaceId = workspaceId;
            this.name = name;
            this.isSandbox = isSandbox;
        }

        @Override
        public String toString() {
            return "WorkspaceCreatedPayload{id=" + workspaceId + ", name=" + name + ", sandbox=" + isSandbox + "}";
        }
    }

    /**
     * Payload for VARIABLE_SET action.
     */
    public static final class VariablePayload {
        public final String variableName;
        public final String typeName;

        public VariablePayload(String variableName, String typeName) {
            this.variableName = variableName;
            this.typeName = typeName;
        }

        @Override
        public String toString() {
            return "VariablePayload{name=" + variableName + ", type=" + typeName + "}";
        }
    }

    /**
     * Payload for SCRIPT_STARTED action.
     */
    public static final class ScriptStartedPayload {
        public final String scriptId;
        public final String scriptName;
        public final int totalLines;

        public ScriptStartedPayload(String scriptId, String scriptName, int totalLines) {
            this.scriptId = scriptId;
            this.scriptName = scriptName;
            this.totalLines = totalLines;
        }

        @Override
        public String toString() {
            return "ScriptStartedPayload{id=" + scriptId + ", name=" + scriptName + ", lines=" + totalLines + "}";
        }
    }

    /**
     * Payload for SCRIPT_LINE_EXECUTED action.
     */
    public static final class ScriptLinePayload {
        public final String scriptId;
        public final int lineNumber;
        public final String lineContent;

        public ScriptLinePayload(String scriptId, int lineNumber, String lineContent) {
            this.scriptId = scriptId;
            this.lineNumber = lineNumber;
            this.lineContent = lineContent;
        }

        @Override
        public String toString() {
            return "ScriptLinePayload{id=" + scriptId + ", line=" + lineNumber + "}";
        }
    }

    /**
     * Payload for SCRIPT_COMPLETED action.
     */
    public static final class ScriptCompletedPayload {
        public final String scriptId;
        public final Object result;

        public ScriptCompletedPayload(String scriptId, Object result) {
            this.scriptId = scriptId;
            this.result = result;
        }

        @Override
        public String toString() {
            return "ScriptCompletedPayload{id=" + scriptId + ", result=" + result + "}";
        }
    }

    /**
     * Payload for SCRIPT_ERROR action.
     */
    public static final class ScriptErrorPayload {
        public final String scriptId;
        public final int lineNumber;
        public final String errorMessage;

        public ScriptErrorPayload(String scriptId, int lineNumber, String errorMessage) {
            this.scriptId = scriptId;
            this.lineNumber = lineNumber;
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            return "ScriptErrorPayload{id=" + scriptId + ", line=" + lineNumber + ", error=" + errorMessage + "}";
        }
    }

}
