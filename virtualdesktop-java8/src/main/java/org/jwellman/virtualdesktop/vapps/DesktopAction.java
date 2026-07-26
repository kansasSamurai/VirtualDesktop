package org.jwellman.virtualdesktop.vapps;

import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.SwingUtilities;

import org.jwellman.virtualdesktop.tools.ToolDefinition;
import org.jwellman.virtualdesktop.tools.ToolEnvironment;

/**
 * Swing Action adapter that launches a catalogued tool by definition id.
 *
 * <p>This class is presentation + command binding only. It is not the tool
 * catalog. On activation it delegates to {@code ToolService.open(definitionId)}.</p>
 *
 * @author rwellman
 */
public class DesktopAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private final String definitionId;

    private final boolean desktopOnly;

    private final boolean external;

    /** DSP icon key for this adapter (may differ from catalog definition for desktop shortcuts). */
    private final String iconKey;

    /**
     * @param definition catalog entry this action opens
     * @param desktopOnly true if this adapter is for a desktop shortcut tile only
     */
    public DesktopAction(ToolDefinition definition, boolean desktopOnly) {
        this(definition, desktopOnly, definition.getIconKey());
    }

    /**
     * @param definition catalog entry this action opens
     * @param desktopOnly true if this adapter is for a desktop shortcut tile only
     * @param iconKey DSP icon key for this tile/menu entry (shortcut-specific override allowed)
     */
    public DesktopAction(ToolDefinition definition, boolean desktopOnly, String iconKey) {
        super(definition.getTitle());
        this.definitionId = definition.getId();
        this.desktopOnly = desktopOnly;
        this.external = definition.isExternal();
        this.iconKey = iconKey;
        if (definition.getClassName() != null) {
            putValue(ACTION_COMMAND_KEY, definition.getClassName());
        }
    }

    /**
     * Legacy constructor — prefer {@link #DesktopAction(ToolDefinition, boolean)}.
     *
     * @param title display name
     */
    public DesktopAction(String title) {
        super(title);
        this.definitionId = null;
        this.desktopOnly = false;
        this.external = false;
        this.iconKey = null;
    }

    /**
     * Legacy constructor — prefer {@link #DesktopAction(ToolDefinition, boolean)}.
     *
     * @param title display name
     * @param icon display icon
     */
    public DesktopAction(String title, Icon icon) {
        super(title, icon);
        this.definitionId = null;
        this.desktopOnly = false;
        this.external = false;
        this.iconKey = null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (definitionId == null || definitionId.isEmpty()) {
            Logger.getLogger(DesktopAction.class.getName()).log(
                Level.SEVERE,
                "DesktopAction has no definitionId — cannot open tool"
            );
            return;
        }
        final String id = definitionId;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ToolEnvironment.service().open(id);
            }
        });
    }

    /**
     * @return catalog definition id, or null for legacy actions
     */
    public String getDefinitionId() {
        return definitionId;
    }

    /**
     * @return DSP icon key for this adapter, or null
     */
    public String getIconKey() {
        return iconKey;
    }

    /**
     * Stable id for a desktop tile. Desktop shortcuts that share a definition
     * (e.g. Home and Trash both targeting SpecJCXConsole) must not collide.
     *
     * @return unique shortcut id for DesktopState
     */
    public String getShortcutId() {
        if (!desktopOnly || definitionId == null) {
            return definitionId;
        }
        Object name = getValue(NAME);
        String label = name != null ? name.toString() : "untitled";
        return "shortcut:" + definitionId + ":" + label;
    }

    /**
     * True if this action is only for desktop icons (UI placement).
     *
     * @return the isDesktopOnly
     */
    public boolean isDesktopOnly() {
        return desktopOnly;
    }

    /**
     * @return true when the underlying definition launches an external process
     */
    public boolean isExternal() {
        return external;
    }

    /**
     * @deprecated retained for menu matching fallbacks; prefer definition className via catalog
     */
    @Deprecated
    public String getClazzName() {
        Object cmd = getValue(ACTION_COMMAND_KEY);
        return cmd == null ? null : cmd.toString();
    }

    /**
     * @deprecated no longer used by the launch path
     */
    @Deprecated
    public void setClazzName(String clazzName) {
        putValue(ACTION_COMMAND_KEY, clazzName);
    }

    /**
     * @deprecated desktopOnly is set at construction from the adapter role
     */
    @Deprecated
    public void setDesktopOnly(boolean value) {
        // no-op: field is final; kept so older call sites compile during migration
    }

    /**
     * @deprecated retained temporarily for binary compatibility
     */
    @Deprecated
    public static void setDesktop(org.jwellman.virtualdesktop.App a) {
        // no-op
    }

}
