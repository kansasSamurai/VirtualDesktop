package org.jwellman.virtualdesktop.state.actions;

/**
 * Generic implementation of the Action interface.
 *
 * Provides static factory methods for creating common actions
 * with type safety for payloads.
 *
 * @author rwellman
 */
public final class SimpleAction implements Action {

    private final String type;
    private final long timestamp;
    private final Object payload;

    /**
     * Create a new action with the given type and payload.
     *
     * @param type the action type
     * @param payload the action payload (may be null)
     */
    public SimpleAction(String type, Object payload) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.payload = payload;
    }

    /**
     * Create a new action with no payload.
     *
     * @param type the action type
     */
    public SimpleAction(String type) {
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
        return "Action{type='" + type + "', payload=" + payload + "}";
    }

    // ========== Factory Methods for Tool Lifecycle ==========

    public static Action toolOpened(String toolId, String toolType, String title, String workspaceId) {
        return new SimpleAction(ActionTypes.TOOL_OPENED,
            new ToolPayload(toolId, toolType, title, workspaceId));
    }

    public static Action toolClosed(String toolId) {
        return new SimpleAction(ActionTypes.TOOL_CLOSED, toolId);
    }

    public static Action toolMinimized(String toolId) {
        return new SimpleAction(ActionTypes.TOOL_MINIMIZED, toolId);
    }

    public static Action toolRestored(String toolId) {
        return new SimpleAction(ActionTypes.TOOL_RESTORED, toolId);
    }

    public static Action toolMaximized(String toolId) {
        return new SimpleAction(ActionTypes.TOOL_MAXIMIZED, toolId);
    }

    public static Action toolActivated(String toolId) {
        return new SimpleAction(ActionTypes.TOOL_ACTIVATED, toolId);
    }

    public static Action toolDeactivated(String toolId) {
        return new SimpleAction(ActionTypes.TOOL_DEACTIVATED, toolId);
    }

    // ========== Factory Methods for Docking ==========

    public static Action panelDockedIn(String targetToolId, String panelId, String sourceToolId) {
        return new SimpleAction(ActionTypes.PANEL_DOCKED_IN,
            new DockingPayload(targetToolId, panelId, sourceToolId));
    }

    public static Action panelDockedOut(String sourceToolId, String panelId) {
        return new SimpleAction(ActionTypes.PANEL_DOCKED_OUT,
            new DockingPayload(null, panelId, sourceToolId));
    }

    public static Action panelLocationChanged(String panelId, String newWorkspaceId) {
        return new SimpleAction(ActionTypes.PANEL_LOCATION_CHANGED,
            new DockingPayload(newWorkspaceId, panelId, null));
    }

    // ========== Factory Methods for Taskbar ==========

    public static Action taskbarGroupingToggled(boolean enabled) {
        return new SimpleAction(ActionTypes.TASKBAR_GROUPING_TOGGLED, enabled);
    }

    public static Action taskbarToolSelected(String toolId) {
        return new SimpleAction(ActionTypes.TASKBAR_TOOL_SELECTED, toolId);
    }

    // ========== Payload Classes ==========

    /**
     * Payload for TOOL_OPENED action.
     */
    public static final class ToolPayload {
        public final String toolId;
        public final String toolType;
        public final String title;
        public final String workspaceId;

        public ToolPayload(String toolId, String toolType, String title, String workspaceId) {
            this.toolId = toolId;
            this.toolType = toolType;
            this.title = title;
            this.workspaceId = workspaceId;
        }

        @Override
        public String toString() {
            return "ToolPayload{id=" + toolId + ", type=" + toolType + ", title=" + title + "}";
        }
    }

    /**
     * Payload for docking-related actions.
     */
    public static final class DockingPayload {
        /** The target tool/workspace receiving the docked panel (for PANEL_DOCKED_IN) */
        public final String targetToolId;
        /** The panel being docked/moved */
        public final String panelId;
        /** The source tool/workspace the panel came from (for PANEL_DOCKED_OUT) */
        public final String sourceToolId;

        public DockingPayload(String targetToolId, String panelId, String sourceToolId) {
            this.targetToolId = targetToolId;
            this.panelId = panelId;
            this.sourceToolId = sourceToolId;
        }

        @Override
        public String toString() {
            return "DockingPayload{target=" + targetToolId + ", panel=" + panelId + ", source=" + sourceToolId + "}";
        }
    }

}
