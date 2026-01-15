package org.jwellman.jfreechart.editor;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;

/**
 * A base class for all editor panels.
 * 
 * @author rwellman
 *
 */
public class BaseEditor extends JPanel {

    private static final long serialVersionUID = 1L;

    public static class FONTS {
        public static final Font TITLE = new Font("Arial", Font.BOLD, 20);
        public static final Font STATUS = new Font("Arial", Font.ITALIC, 11);
        public static final Font TEXTAREA = new Font("Arial", Font.PLAIN, 12);
    }

    public static class BORDERS {
        public static final Border DEBUG = BorderFactory.createLineBorder(Color.red);
        public static final Border TITLE = BorderFactory.createEmptyBorder(0, 0, 20, 0);
        public static final Border PANEL = BorderFactory.createEmptyBorder(10, 10, 10, 10);
    }

    public JLabel createLabel(String text) {
        JLabel c = new JLabel(text);
        // c.setBorder(BORDERS.DEBUG);
        return c;
    }

    public void showSavedMessage() {
        JOptionPane.showMessageDialog(this, 
                "Changes saved!", 
                "SUCCESS", 
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void showResetMessage() {
        JOptionPane.showMessageDialog(this, 
                "Fields reset to saved values.", 
                "SUCCESS", 
                JOptionPane.INFORMATION_MESSAGE);
    }

}
