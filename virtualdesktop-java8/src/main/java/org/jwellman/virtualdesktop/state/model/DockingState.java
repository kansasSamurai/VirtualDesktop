package org.jwellman.virtualdesktop.state.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Immutable state representing the docking status of a tool.
 *
 * Tracks:
 * - Which workspace owns this tool
 * - Whether the original panel is still in its frame
 * - Whether the frame has content from other tools
 * - IDs of panels docked into this workspace
 *
 * @author rwellman
 */
public final class DockingState {

    private final String workspaceId;
    private final boolean originalPanelPresent;
    private final boolean hasExternalContent;
    private final Set<String> dockedPanelIds;

    private DockingState(String workspaceId, boolean originalPanelPresent,
                         boolean hasExternalContent, Set<String> dockedPanelIds) {
        this.workspaceId = workspaceId;
        this.originalPanelPresent = originalPanelPresent;
        this.hasExternalContent = hasExternalContent;
        this.dockedPanelIds = Collections.unmodifiableSet(new HashSet<>(dockedPanelIds));
    }

    /**
     * Create initial docking state for a new tool.
     */
    public static DockingState initial(String workspaceId) {
        return new DockingState(workspaceId, true, false, Collections.<String>emptySet());
    }

    // ========== Getters ==========

    public String getWorkspaceId() {
        return workspaceId;
    }

    public boolean isOriginalPanelPresent() {
        return originalPanelPresent;
    }

    public boolean hasExternalContent() {
        return hasExternalContent;
    }

    public Set<String> getDockedPanelIds() {
        return dockedPanelIds;
    }

    // ========== Copy-on-write modifiers ==========

    public DockingState withOriginalPanelPresent(boolean present) {
        return new DockingState(workspaceId, present, hasExternalContent, dockedPanelIds);
    }

    public DockingState withPanelDockedIn(String panelId) {
        Set<String> newSet = new HashSet<>(dockedPanelIds);
        newSet.add(panelId);
        return new DockingState(workspaceId, originalPanelPresent, true, newSet);
    }

    public DockingState withPanelDockedOut(String panelId) {
        Set<String> newSet = new HashSet<>(dockedPanelIds);
        newSet.remove(panelId);
        boolean hasExternal = !newSet.isEmpty();
        return new DockingState(workspaceId, originalPanelPresent, hasExternal, newSet);
    }

    @Override
    public String toString() {
        return "DockingState{workspace=" + workspaceId +
               ", originalPresent=" + originalPanelPresent +
               ", hasExternal=" + hasExternalContent + "}";
    }

}
