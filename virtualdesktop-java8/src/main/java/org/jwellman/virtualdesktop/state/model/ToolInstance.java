package org.jwellman.virtualdesktop.state.model;

/**
 * Immutable state for a single running tool instance.
 *
 * @author rwellman
 */
public final class ToolInstance {

    private final String id;
    private final String toolType;
    private final String title;
    private final FrameState frameState;
    private final DockingState dockingState;
    private final long createdAt;

    private ToolInstance(String id, String toolType, String title,
                         FrameState frameState, DockingState dockingState, long createdAt) {
        this.id = id;
        this.toolType = toolType;
        this.title = title;
        this.frameState = frameState;
        this.dockingState = dockingState;
        this.createdAt = createdAt;
    }

    /**
     * Create a new tool instance with initial state.
     */
    public static ToolInstance create(String id, String toolType, String title, String workspaceId) {
        return new ToolInstance(
            id,
            toolType,
            title,
            FrameState.NORMAL,
            DockingState.initial(workspaceId),
            System.currentTimeMillis()
        );
    }

    // ========== Getters ==========

    public String getId() {
        return id;
    }

    public String getToolType() {
        return toolType;
    }

    public String getTitle() {
        return title;
    }

    public FrameState getFrameState() {
        return frameState;
    }

    public DockingState getDockingState() {
        return dockingState;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // ========== Copy-on-write modifiers ==========

    public ToolInstance withFrameState(FrameState newState) {
        return new ToolInstance(id, toolType, title, newState, dockingState, createdAt);
    }

    public ToolInstance withTitle(String newTitle) {
        return new ToolInstance(id, toolType, newTitle, frameState, dockingState, createdAt);
    }

    public ToolInstance withDockingState(DockingState newDockingState) {
        return new ToolInstance(id, toolType, title, frameState, newDockingState, createdAt);
    }

    @Override
    public String toString() {
        return "ToolInstance{id=" + id + ", type=" + toolType + ", title=" + title +
               ", frame=" + frameState + "}";
    }

}
