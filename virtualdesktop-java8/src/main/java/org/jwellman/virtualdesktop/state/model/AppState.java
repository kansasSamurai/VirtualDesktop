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
    private final TaskbarState taskbar;
    private final long timestamp;

    private AppState(ToolsState tools, TaskbarState taskbar, long timestamp) {
        this.tools = tools;
        this.taskbar = taskbar;
        this.timestamp = timestamp;
    }

    /**
     * Create the initial application state.
     */
    public static AppState initial() {
        return new AppState(
            ToolsState.empty(),
            TaskbarState.initial(),
            System.currentTimeMillis()
        );
    }

    /**
     * Create a new state with updated components.
     */
    public static AppState create(ToolsState tools, TaskbarState taskbar) {
        return new AppState(tools, taskbar, System.currentTimeMillis());
    }

    // ========== Getters ==========

    public ToolsState getTools() {
        return tools;
    }

    public TaskbarState getTaskbar() {
        return taskbar;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // ========== Copy-on-write modifiers ==========

    public AppState withTools(ToolsState newTools) {
        return new AppState(newTools, taskbar, System.currentTimeMillis());
    }

    public AppState withTaskbar(TaskbarState newTaskbar) {
        return new AppState(tools, newTaskbar, System.currentTimeMillis());
    }

    @Override
    public String toString() {
        return "AppState{tools=" + tools.getToolCount() + ", taskbar=" + taskbar + "}";
    }

}
