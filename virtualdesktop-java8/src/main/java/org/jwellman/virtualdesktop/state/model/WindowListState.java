package org.jwellman.virtualdesktop.state.model;

/**
 * Immutable state for the window list view (Zone 2 of the taskbar).
 *
 * @author rwellman
 */
public final class WindowListState {

    private final boolean groupingEnabled;
    private final GroupingMode groupingMode;
    private final String selectedToolId;

    private WindowListState(boolean groupingEnabled, GroupingMode groupingMode, String selectedToolId) {
        this.groupingEnabled = groupingEnabled;
        this.groupingMode = groupingMode;
        this.selectedToolId = selectedToolId;
    }

    /**
     * Create initial window list state.
     */
    public static WindowListState initial() {
        return new WindowListState(false, GroupingMode.BY_TYPE, null);
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

    public WindowListState withGroupingEnabled(boolean enabled) {
        return new WindowListState(enabled, groupingMode, selectedToolId);
    }

    public WindowListState withGroupingMode(GroupingMode mode) {
        return new WindowListState(groupingEnabled, mode, selectedToolId);
    }

    public WindowListState withSelectedToolId(String toolId) {
        return new WindowListState(groupingEnabled, groupingMode, toolId);
    }

    @Override
    public String toString() {
        return "WindowListState{grouping=" + groupingEnabled + ", mode=" + groupingMode +
               ", selected=" + selectedToolId + "}";
    }

}
