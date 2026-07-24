package org.jwellman.virtualdesktop.state.model;

/**
 * Immutable state for a single running tool instance.
 *
 * <p>Carries catalog linkage ({@code definitionId}) and display metadata
 * ({@code iconKey}) so views can render without consulting Swing frames.</p>
 *
 * @author rwellman
 */
public final class ToolInstance {

    private final String id;
    private final String toolType;
    private final String title;
    private final String definitionId;
    private final String iconKey;
    private final FrameState frameState;
    private final DockingState dockingState;
    private final long createdAt;

    private ToolInstance(String id, String toolType, String title,
                         String definitionId, String iconKey,
                         FrameState frameState, DockingState dockingState, long createdAt) {
        this.id = id;
        this.toolType = toolType;
        this.title = title;
        this.definitionId = definitionId;
        this.iconKey = iconKey;
        this.frameState = frameState;
        this.dockingState = dockingState;
        this.createdAt = createdAt;
    }

    /**
     * Create a new tool instance with initial state.
     *
     * @param id running instance id
     * @param toolType Spec simple class name (grouping)
     * @param title display title
     * @param workspaceId docking workspace id (often same as id)
     * @param definitionId catalog definition id, or null for ad-hoc / scripted opens
     * @param iconKey DSP icon key from the catalog, or null
     */
    public static ToolInstance create(String id, String toolType, String title,
                                      String workspaceId, String definitionId, String iconKey) {
        return new ToolInstance(
            id,
            toolType,
            title,
            definitionId,
            iconKey,
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

    /**
     * @return catalog definition id used to open this tool, or null if not from catalog
     */
    public String getDefinitionId() {
        return definitionId;
    }

    /**
     * @return DSP icon key for taskbar/desktop rendering, or null
     */
    public String getIconKey() {
        return iconKey;
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
        return new ToolInstance(id, toolType, title, definitionId, iconKey,
            newState, dockingState, createdAt);
    }

    public ToolInstance withTitle(String newTitle) {
        return new ToolInstance(id, toolType, newTitle, definitionId, iconKey,
            frameState, dockingState, createdAt);
    }

    public ToolInstance withDockingState(DockingState newDockingState) {
        return new ToolInstance(id, toolType, title, definitionId, iconKey,
            frameState, newDockingState, createdAt);
    }

    @Override
    public String toString() {
        return "ToolInstance{id=" + id + ", type=" + toolType + ", title=" + title +
               ", def=" + definitionId + ", frame=" + frameState + "}";
    }

}
