package org.jwellman.virtualdesktop.state.model;

/**
 * Immutable state for the taskbar view.
 *
 * @author rwellman
 */
public final class TaskbarState {

    private final boolean groupingEnabled;
    private final GroupingMode groupingMode;
    private final String selectedToolId;

    private TaskbarState(boolean groupingEnabled, GroupingMode groupingMode, String selectedToolId) {
        this.groupingEnabled = groupingEnabled;
        this.groupingMode = groupingMode;
        this.selectedToolId = selectedToolId;
    }

    /**
     * Create initial taskbar state.
     */
    public static TaskbarState initial() {
        return new TaskbarState(false, GroupingMode.BY_TYPE, null);
    }

    // ========== Getters ==========

    public boolean isGroupingEnabled() {
        return groupingEnabled;
    }

    public GroupingMode getGroupingMode() {
        return groupingMode;
    }

    public String getSelectedToolId() {
        return selectedToolId;
    }

    // ========== Copy-on-write modifiers ==========

    public TaskbarState withGroupingEnabled(boolean enabled) {
        return new TaskbarState(enabled, groupingMode, selectedToolId);
    }

    public TaskbarState withGroupingMode(GroupingMode mode) {
        return new TaskbarState(groupingEnabled, mode, selectedToolId);
    }

    public TaskbarState withSelectedToolId(String toolId) {
        return new TaskbarState(groupingEnabled, groupingMode, toolId);
    }

    @Override
    public String toString() {
        return "TaskbarState{grouping=" + groupingEnabled + ", mode=" + groupingMode +
               ", selected=" + selectedToolId + "}";
    }

}
