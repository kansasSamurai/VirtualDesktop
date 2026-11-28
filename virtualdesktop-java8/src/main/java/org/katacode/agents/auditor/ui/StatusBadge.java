package org.katacode.agents.auditor.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.katacode.agents.auditor.model.ProjectStatus;

/**
 * Compact tinted label such as "[OK]" or "[2 Drift]".
 */
@SuppressWarnings("serial")
class StatusBadge extends JLabel {

    private static final int ARC = 8;

    private Color tint;
    private Color outline;

    StatusBadge() {
        setFont(AuditorTheme.monoFont(13));
        setBorder(BorderFactory.createEmptyBorder(3, 7, 3, 7));
        setStatus(ProjectStatus.OK, 0);
    }

    void setStatus(ProjectStatus status, int count) {
        switch (status) {
            case WARN:
                setText(counted(count, "Warn"));
                applyColors("warn", 0xE8B23A, 0x7A6224, new Color(232, 178, 58, 26));
                break;
            case DRIFT:
                setText(counted(count, "Drift"));
                applyColors("drift", 0xF5A15E, 0x8A5A2E, new Color(240, 138, 60, 31));
                break;
            case WAIVED:
                setText("[Waived]");
                applyColors("neutral", 0xA9AFB8, 0x454A53, new Color(139, 145, 154, 26));
                break;
            default:
                setText("[OK]");
                applyColors("ok", 0x6BD685, 0x2F6B40, new Color(76, 194, 107, 26));
                break;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(tint);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
        g2.setColor(outline);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
        g2.dispose();

        super.paintComponent(g);
    }

    private void applyColors(String id, int foregroundRgb, int outlineRgb, Color defaultTint) {
        String prefix = "Workspace.status." + id;
        setForeground(AuditorTheme.color(prefix + ".foreground", foregroundRgb));
        outline = AuditorTheme.color(prefix + ".border", outlineRgb);
        tint = defaultTint;
    }

    private static String counted(int count, String label) {
        if (count > 0) {
            return "[" + count + " " + label + "]";
        }

        return "[" + label + "]";
    }
}
