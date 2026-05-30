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
    private final long timestamp;

    private AppState(ToolsState tools, WindowListState windowList, long timestamp) {
        this.tools = tools;
        this.windowList = windowList;
        this.timestamp = timestamp;
    }

    /**
     * Create the initial application state.
     */
    public static AppState initial() {
        return new AppState(
            ToolsState.empty(),
            WindowListState.initial(),
            System.currentTimeMillis()
        );
    }

    /**
     * Create a new state with updated components.
     */
    public static AppState create(ToolsState tools, WindowListState windowList) {
        return new AppState(tools, windowList, System.currentTimeMillis());
    }

    // ========== Getters ==========

    public ToolsState getTools() {
        return tools;
    }

    public WindowListState getWindowList() {
        return windowList;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // ========== Copy-on-write modifiers ==========

    public AppState withTools(ToolsState newTools) {
        return new AppState(newTools, windowList, System.currentTimeMillis());
    }

    public AppState withWindowList(WindowListState newWindowList) {
        return new AppState(tools, newWindowList, System.currentTimeMillis());
    }

    @Override
    public String toString() {
        return "AppState{tools=" + tools.getToolCount() + ", windowList=" + windowList + "}";
    }

}
