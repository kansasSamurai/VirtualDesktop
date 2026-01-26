package org.jwellman.bsh.state.model;

/**
 * Immutable representation of a single workspace.
 *
 * A workspace represents an isolated BeanShell execution context
 * with its own namespace and variables.
 *
 * @author rwellman
 */
public final class WorkspaceInfo {

    private final String id;
    private final String name;
    private final long createdAt;
    private final boolean isSandbox;

    private WorkspaceInfo(String id, String name, long createdAt, boolean isSandbox) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.isSandbox = isSandbox;
    }

    /**
     * Create a new workspace info.
     */
    public static WorkspaceInfo create(String id, String name, boolean isSandbox) {
        return new WorkspaceInfo(id, name, System.currentTimeMillis(), isSandbox);
    }

    /**
     * Create a global workspace info.
     */
    public static WorkspaceInfo createGlobal() {
        return new WorkspaceInfo("global", "Global", System.currentTimeMillis(), false);
    }

    // ========== Getters ==========

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isSandbox() {
        return isSandbox;
    }

    // ========== Copy-on-write modifiers ==========

    public WorkspaceInfo withName(String newName) {
        return new WorkspaceInfo(id, newName, createdAt, isSandbox);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkspaceInfo that = (WorkspaceInfo) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "WorkspaceInfo{id=" + id + ", name=" + name + ", sandbox=" + isSandbox + "}";
    }

}
