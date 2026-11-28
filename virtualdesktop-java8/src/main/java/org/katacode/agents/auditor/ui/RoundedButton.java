package org.katacode.agents.auditor.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JButton;

/**
 * The tool's standard push button: rounded fill and outline at a fixed height,
 * independent of the installed look and feel.
 */
@SuppressWarnings("serial")
class RoundedButton extends JButton {

    private static final int RADIUS = 6;

    private final int height;

    /**
     * @param text   the button label
     * @param height fixed height in pixels
     */
    RoundedButton(String text, int height) {
        super(text);
        this.height = height;

        setFont(AuditorTheme.uiFont(Font.PLAIN, 14f));
        setForeground(AuditorTheme.textPrimary());
        setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(AuditorTheme.buttonBorder(), RADIUS),
                BorderFactory.createEmptyBorder(0, 13, 0, 13)));
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.height = height;

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
