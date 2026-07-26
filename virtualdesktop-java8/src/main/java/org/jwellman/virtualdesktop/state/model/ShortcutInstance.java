package org.jwellman.virtualdesktop.state.model;

/**
 * Immutable model for one desktop shortcut tile.
 *
 * <p>Catalog-backed shortcuts typically use {@code definitionId} as {@code id}
 * so identity is stable across restarts.</p>
 */
public final class ShortcutInstance {

    private final String id;
    private final String label;
    private final String iconKey;
    private final String definitionId;
    private final int x;
    private final int y;
    private final boolean external;
    private final String linkedToolId;

    private ShortcutInstance(String id, String label, String iconKey, String definitionId,
                             int x, int y, boolean external, String linkedToolId) {
        this.id = id;
        this.label = label;
        this.iconKey = iconKey;
        this.definitionId = definitionId;
        this.x = x;
        this.y = y;
        this.external = external;
        this.linkedToolId = linkedToolId;
    }

    public static ShortcutInstance create(String id, String label, String iconKey,
                                          String definitionId, int x, int y, boolean external) {
        return new ShortcutInstance(id, label, iconKey, definitionId, x, y, external, null);
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getIconKey() {
        return iconKey;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isExternal() {
        return external;
    }

    /**
     * @return running tool id linked to this shortcut, or null
     */
    public String getLinkedToolId() {
        return linkedToolId;
    }

    public ShortcutInstance withPosition(int newX, int newY) {
        return new ShortcutInstance(id, label, iconKey, definitionId, newX, newY, external, linkedToolId);
    }

    public ShortcutInstance withLinkedToolId(String toolId) {
        return new ShortcutInstance(id, label, iconKey, definitionId, x, y, external, toolId);
    }

    @Override
    public String toString() {
        return "ShortcutInstance{id=" + id + ", label=" + label + ", pos=(" + x + "," + y + ")}";
    }

}
