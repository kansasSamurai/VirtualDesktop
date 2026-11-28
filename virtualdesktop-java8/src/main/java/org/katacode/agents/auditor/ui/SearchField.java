package org.katacode.agents.auditor.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JTextField;

import jiconfont.icons.GoogleMaterialDesignIcons;
import jiconfont.swing.IconFontSwing;

/**
 * Rounded search box. The placeholder and leading icon are FlatLaf features and
 * are simply absent under other look and feels. It is the part of the toolbar
 * that gives up width first.
 */
@SuppressWarnings("serial")
class SearchField extends JTextField {

    private static final int WIDTH = 170;
    private static final int MIN_WIDTH = 80;
    private static final int HEIGHT = 34;
    private static final int RADIUS = 6;

    SearchField() {
        setOpaque(false);
        setFont(AuditorTheme.uiFont(Font.PLAIN, 14f));
        setForeground(AuditorTheme.textPrimary());
        setCaretColor(AuditorTheme.textPrimary());
        setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(AuditorTheme.lineStrong(), RADIUS),
                BorderFactory.createEmptyBorder(0, 9, 0, 9)));
        putClientProperty("JTextField.placeholderText", "Search");
        putClientProperty("JTextField.leadingIcon", IconFontSwing.buildIcon(
                GoogleMaterialDesignIcons.SEARCH, 17, AuditorTheme.textMuted()));
        getAccessibleContext().setAccessibleName("Search projects");
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(WIDTH, HEIGHT);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(MIN_WIDTH, HEIGHT);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(AuditorTheme.inputBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), RADIUS * 2, RADIUS * 2);
        g2.dispose();

        super.paintComponent(g);
    }
}
