package org.jwellman.virtualdesktop.state.actions;

/**
 * Constants for all action types in the application.
 *
 * Action types follow SCREAMING_SNAKE_CASE convention.
 *
 * @author rwellman
 */
public final class ActionTypes {

    private ActionTypes() {
        // Prevent instantiation
    }

    // ========== Tool Lifecycle ==========

    /** Dispatched when a new tool window is opened */
    public static final String TOOL_OPENED = "TOOL_OPENED";

    /** Dispatched when a tool window is closed */
    public static final String TOOL_CLOSED = "TOOL_CLOSED";

    /** Dispatched when a tool window is minimized/iconified */
    public static final String TOOL_MINIMIZED = "TOOL_MINIMIZED";

    /** Dispatched when a tool window is restored from minimized state */
    public static final String TOOL_RESTORED = "TOOL_RESTORED";

    /** Dispatched when a tool window is maximized */
    public static final String TOOL_MAXIMIZED = "TOOL_MAXIMIZED";

    /** Dispatched when a tool window gains focus */
    public static final String TOOL_ACTIVATED = "TOOL_ACTIVATED";

    /** Dispatched when a tool window loses focus */
    public static final String TOOL_DEACTIVATED = "TOOL_DEACTIVATED";

    /** Dispatched when a tool's title changes */
    public static final String TOOL_TITLE_CHANGED = "TOOL_TITLE_CHANGED";

    // ========== Docking ==========

    /** Dispatched when a panel is docked into a different frame */
    public static final String PANEL_DOCKED_IN = "PANEL_DOCKED_IN";

    /** Dispatched when a panel is docked out of its original frame */
    public static final String PANEL_DOCKED_OUT = "PANEL_DOCKED_OUT";

    /** Dispatched when a panel's docking location changes */
    public static final String PANEL_LOCATION_CHANGED = "PANEL_LOCATION_CHANGED";

    // ========== Taskbar ==========

    /** Dispatched when taskbar grouping is toggled */
    public static final String TASKBAR_GROUPING_TOGGLED = "TASKBAR_GROUPING_TOGGLED";

    /** Dispatched when a tool is selected in the taskbar */
    public static final String TASKBAR_TOOL_SELECTED = "TASKBAR_TOOL_SELECTED";

}
