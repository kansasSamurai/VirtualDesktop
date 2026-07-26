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

    // ========== Window List ==========

    /** Dispatched when window list grouping is toggled */
    public static final String WINDOWLIST_GROUPING_TOGGLED = "WINDOWLIST_GROUPING_TOGGLED";

    /** Dispatched when a tool is selected in the window list */
    public static final String WINDOWLIST_TOOL_SELECTED = "WINDOWLIST_TOOL_SELECTED";

    // ========== Desktop shortcuts ==========

    /** Dispatched when a shortcut is added to the desktop */
    public static final String SHORTCUT_ADDED = "SHORTCUT_ADDED";

    /** Dispatched when a shortcut is removed from the desktop */
    public static final String SHORTCUT_REMOVED = "SHORTCUT_REMOVED";

    /** Dispatched when a shortcut is selected */
    public static final String SHORTCUT_SELECTED = "SHORTCUT_SELECTED";

    /** Dispatched when desktop shortcut selection is cleared */
    public static final String SHORTCUT_DESELECTED = "SHORTCUT_DESELECTED";

    /** Dispatched when a shortcut is moved */
    public static final String SHORTCUT_MOVED = "SHORTCUT_MOVED";

    /** Dispatched when a shortcut is activated (double-click); open is a side effect */
    public static final String SHORTCUT_INVOKED = "SHORTCUT_INVOKED";

}
