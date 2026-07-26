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

    public static Action toolOpened(String toolId, String toolType, String title,
                                    String workspaceId, String definitionId, String iconKey) {
        return new SimpleAction(ActionTypes.TOOL_OPENED,
            new ToolPayload(toolId, toolType, title, workspaceId, definitionId, iconKey));
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

    // ========== Factory Methods for Window List ==========

    public static Action windowListGroupingToggled(boolean enabled) {
        return new SimpleAction(ActionTypes.WINDOWLIST_GROUPING_TOGGLED, enabled);
    }

    public static Action windowListToolSelected(String toolId) {
        return new SimpleAction(ActionTypes.WINDOWLIST_TOOL_SELECTED, toolId);
    }

    // ========== Factory Methods for Desktop Shortcuts ==========

    public static Action shortcutAdded(org.jwellman.virtualdesktop.state.model.ShortcutInstance shortcut) {
        return new SimpleAction(ActionTypes.SHORTCUT_ADDED, shortcut);
    }

    public static Action shortcutRemoved(String shortcutId) {
        return new SimpleAction(ActionTypes.SHORTCUT_REMOVED, shortcutId);
    }

    public static Action shortcutSelected(String shortcutId) {
        return new SimpleAction(ActionTypes.SHORTCUT_SELECTED, shortcutId);
    }

    public static Action shortcutDeselected() {
        return new SimpleAction(ActionTypes.SHORTCUT_DESELECTED, null);
    }

    public static Action shortcutMoved(String shortcutId, int x, int y) {
        return new SimpleAction(ActionTypes.SHORTCUT_MOVED,
            new ShortcutMovedPayload(shortcutId, x, y));
    }

    public static Action shortcutInvoked(String shortcutId) {
        return new SimpleAction(ActionTypes.SHORTCUT_INVOKED, shortcutId);
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
        public final String definitionId;
        public final String iconKey;

        public ToolPayload(String toolId, String toolType, String title, String workspaceId,
                           String definitionId, String iconKey) {
            this.toolId = toolId;
            this.toolType = toolType;
            this.title = title;
            this.workspaceId = workspaceId;
            this.definitionId = definitionId;
            this.iconKey = iconKey;
        }

        @Override
        public String toString() {
            return "ToolPayload{id=" + toolId + ", type=" + toolType + ", title=" + title +
                   ", def=" + definitionId + "}";
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

    /**
     * Payload for SHORTCUT_MOVED.
     */
    public static final class ShortcutMovedPayload {
        public final String shortcutId;
        public final int x;
        public final int y;

        public ShortcutMovedPayload(String shortcutId, int x, int y) {
            this.shortcutId = shortcutId;
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "ShortcutMovedPayload{id=" + shortcutId + ", x=" + x + ", y=" + y + "}";
        }
    }

}
