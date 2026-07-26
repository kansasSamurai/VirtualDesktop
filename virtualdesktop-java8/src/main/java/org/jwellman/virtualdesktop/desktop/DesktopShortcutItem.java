package org.jwellman.virtualdesktop.desktop;

import javax.swing.Icon;

/**
 * View model for one desktop shortcut tile.
 *
 * <p>Decouples ClassicDesktopView / VShortcut from Redux ShortcutInstance.</p>
 */
public final class DesktopShortcutItem {

    private final String id;
    private final String label;
    private final Icon icon;
    private final int x;
    private final int y;
    private final boolean external;
    private final String definitionId;

    public DesktopShortcutItem(String id, String label, Icon icon, int x, int y,
                               boolean external, String definitionId) {
        this.id = id;
        this.label = label;
        this.icon = icon;
        this.x = x;
        this.y = y;
        this.external = external;
        this.definitionId = definitionId;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public Icon getIcon() {
        return icon;
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

    public String getDefinitionId() {
        return definitionId;
    }

}
