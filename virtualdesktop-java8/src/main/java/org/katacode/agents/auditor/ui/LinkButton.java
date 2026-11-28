package org.katacode.agents.auditor.ui;

import java.awt.Cursor;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;

/**
 * A button that looks like a text link.
 */
@SuppressWarnings("serial")
class LinkButton extends JButton {

    LinkButton(String text) {
        super(text);
        setFont(AuditorTheme.uiFont(Font.PLAIN, 14f));
        setForeground(AuditorTheme.accentText());
        setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
