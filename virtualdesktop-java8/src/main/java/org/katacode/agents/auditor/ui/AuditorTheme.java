package org.katacode.agents.auditor.ui;

import java.awt.Color;
import java.awt.Font;

import javax.swing.UIManager;

/**
 * Design tokens for the Agent Auditor ("Workspace Dark"). Each colour can be
 * overridden through its UIManager key; the design value is the fallback.
 */
public final class AuditorTheme {

    private AuditorTheme() {
    }

    public static Color sidebarBackground() {
        return color("Workspace.sidebar.background", 0x181A1F);
    }

    public static Color contentBackground() {
        return color("Workspace.content.background", 0x1C1E23);
    }

    public static Color lineSubtle() {
        return color("Workspace.line.subtle", 0x2C3037);
    }

    public static Color textPrimary() {
        return color("Workspace.text.primary", 0xE6E8EB);
    }

    public static Color textSecondary() {
        return color("Workspace.text.secondary", 0xA9AFB8);
    }

    public static Color textWaived() {
        return color("Workspace.text.waived", 0xC7CBD1);
    }

    public static Color accent() {
        return color("Workspace.accent", 0x7B9BFF);
    }

    public static Color accentText() {
        return color("Workspace.accent.text", 0x8FA9FF);
    }

    public static Color waivedMarker() {
        return color("Workspace.text.disabled", 0x5A5F68);
    }

    public static Color cellBackground() {
        return color("Workspace.cell.background", 0x1F2227);
    }

    public static Color cellBorder() {
        return color("Workspace.cell.border", 0x2E3239);
    }

    public static Color cellHoverBackground() {
        return color("Workspace.cell.hoverBackground", 0x262930);
    }

    public static Color cellHoverBorder() {
        return color("Workspace.cell.hoverBorder", 0x3D424B);
    }

    public static Color cellSelectedBackground() {
        return color("Workspace.cell.selectedBackground", 0x232A3D);
    }

    public static Color cellSelectedBorder() {
        return color("Workspace.cell.selectedBorder", 0x6F8BEA);
    }

    /** Translucent ring drawn around the selected cell. */
    public static Color cellSelectedGlow() {
        Color accent = accent();
        return new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 31);
    }

    public static Color color(String key, int fallbackRgb) {
        Color color = UIManager.getColor(key);
        if (color != null) {
            return color;
        }

        return new Color(fallbackRgb);
    }

    /** The application's default font at the given size. */
    public static Font uiFont(int style, float size) {
        Font base = UIManager.getFont("defaultFont");
        if (base == null) {
            base = UIManager.getFont("Label.font");
        }

        return base.deriveFont(style, size);
    }

    public static Font monoFont(int size) {
        return new Font("Consolas", Font.PLAIN, size);
    }
}
