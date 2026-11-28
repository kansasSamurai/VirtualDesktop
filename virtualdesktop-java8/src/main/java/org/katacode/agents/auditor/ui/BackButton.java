package org.katacode.agents.auditor.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JButton;

import jiconfont.icons.GoogleMaterialDesignIcons;
import jiconfont.swing.IconFontSwing;

/**
 * The breadcrumb's rounded "Back" button with a leading chevron.
 */
@SuppressWarnings("serial")
class BackButton extends JButton {

    private static final int HEIGHT = 28;
    private static final int RADIUS = 6;

    BackButton(String text) {
        super(text);
        setIcon(IconFontSwing.buildIcon(
                GoogleMaterialDesignIcons.CHEVRON_LEFT, 16, AuditorTheme.textSecondary()));
        setIconTextGap(4);
        setFont(AuditorTheme.uiFont(Font.BOLD, 14f));
        setForeground(AuditorTheme.textPrimary());
        setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(AuditorTheme.buttonBorder(), RADIUS),
                BorderFactory.createEmptyBorder(0, 7, 0, 10)));
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.height = HEIGHT;

        return size;
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getModel().isRollover()
                ? AuditorTheme.buttonHoverBackground()
                : AuditorTheme.buttonBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), RADIUS * 2, RADIUS * 2);
        g2.dispose();

        super.paintComponent(g);
    }
}
