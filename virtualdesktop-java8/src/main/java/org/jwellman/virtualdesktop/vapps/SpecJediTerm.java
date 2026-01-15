package org.jwellman.virtualdesktop.vapps;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Placeholder for JediTerm terminal emulator.
 *
 * The JediTerm library requires Java 11+ and is not compatible
 * with the Java 8 build of VirtualDesktop. See the virtualdesktop-java9
 * module for the full implementation.
 *
 * @author rwellman
 */
public class SpecJediTerm extends VirtualAppSpec {

    public SpecJediTerm() {
        super();

        this.setTitle("JEDI Terminal");
        this.setContent(createPlaceholderPanel());
    }

    private JPanel createPlaceholderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel label = new JLabel(
            "<html><center>" +
            "<b>JEDI Terminal</b><br><br>" +
            "This feature requires Java 11+<br><br>" +
            "See the <i>virtualdesktop-java9</i> module<br>" +
            "for the full terminal implementation." +
            "</center></html>",
            SwingConstants.CENTER
        );
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

}
