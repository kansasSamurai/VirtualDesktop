package org.jwellman.virtualdesktop.taskbar;

import java.awt.Point;

/**
 * Callback interface from a TaskbarView to its controller.
 *
 * The view fires these events in response to user interaction. The controller
 * owns all action logic (activating tools, building context menus, dispatching
 * Redux actions) — the view just reports what the user did and where.
 */
public interface TaskbarViewListener {

    /**
     * The user selected a tool or group item in the taskbar.
     *
     * @param toolId  the ToolInstance id for leaf items; the group type key
     *                (class name) for group items
     * @param isGroup true if the selected item is a group header
     */
    void onItemSelected(String toolId, boolean isGroup);

    /**
     * The user requested a context menu (right-click or equivalent).
     * The controller is responsible for building and showing the popup.
     *
     * @param id          tool id or group type key, matching onItemSelected semantics
     * @param isGroup     true if context was requested on a group header
     * @param screenPoint location to anchor the popup
     */
    void onContextRequested(String id, boolean isGroup, Point screenPoint);

}
