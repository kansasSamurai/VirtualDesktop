package org.jwellman.virtualdesktop.state.model;

/**
 * Root immutable state for the entire application.
 *
 * This is the single source of truth in the Redux-style architecture.
 *
 * @author rwellman
 */
public final class AppState {

    private final ToolsState tools;
    private final WindowListState windowList;
    private final DesktopState desktop;
    private final long timestamp;

    private AppState(ToolsState tools, WindowListState windowList, DesktopState desktop, long timestamp) {
        this.tools = tools;
        this.windowList = windowList;
        this.desktop = desktop;
        this.timestamp = timestamp;
    }

    /**
     * Create the initial application state.
     */
    public static AppState initial() {
        return new AppState(
            ToolsState.empty(),
            WindowListState.initial(),
            DesktopState.empty(),
            System.currentTimeMillis()
        );
    }

    /**
     * Create a new state with updated components.
     */
    public static AppState create(ToolsState tools, WindowListState windowList, DesktopState desktop) {
        return new AppState(tools, windowList, desktop, System.currentTimeMillis());
    }

    // ========== Getters ==========

    public ToolsState getTools() {
        return tools;
    }

    public WindowListState getWindowList() {
        return windowList;
    }

    public DesktopState getDesktop() {
        return desktop;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // ========== Copy-on-write modifiers ==========

    public AppState withTools(ToolsState newTools) {
        return new AppState(newTools, windowList, desktop, System.currentTimeMillis());
    }

    public AppState withWindowList(WindowListState newWindowList) {
        return new AppState(tools, newWindowList, desktop, System.currentTimeMillis());
    }

    public AppState withDesktop(DesktopState newDesktop) {
        return new AppState(tools, windowList, newDesktop, System.currentTimeMillis());
    }

    @Override
    public String toString() {
        return "AppState{tools=" + tools.getToolCount()
            + ", windowList=" + windowList
            + ", desktop=" + desktop.getShortcutCount()
            + "}";
    }

}
