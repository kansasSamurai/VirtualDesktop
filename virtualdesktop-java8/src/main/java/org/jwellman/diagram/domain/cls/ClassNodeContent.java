package org.jwellman.diagram.domain.cls;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

/**
 * Pure-Swing JPanel representing a class or interface in a class diagram.
 * Has no dependency on diagram framework classes.
 */
public class ClassNodeContent extends JPanel {

    private static final Color CLASS_HEADER_BG     = new Color(180, 210, 255);
    private static final Color INTERFACE_HEADER_BG = new Color(200, 240, 200);
    private static final Color CONTENT_BG          = new Color(248, 248, 248);
    private static final Color BORDER_COLOR        = new Color(120, 140, 170);

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public ClassNodeContent(String name, String stereotype,
                             Object fields, Object methods) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        setBackground(CONTENT_BG);

        // Header
        boolean isInterface = "INTERFACE".equalsIgnoreCase(stereotype);
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(isInterface ? INTERFACE_HEADER_BG : CLASS_HEADER_BG);
        header.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        if (isInterface) {
            JLabel stereoLabel = new JLabel("«interface»", SwingConstants.CENTER);
            stereoLabel.setFont(new Font("SansSerif", Font.PLAIN, 9));
            stereoLabel.setForeground(new Color(60, 100, 60));
            header.add(stereoLabel, BorderLayout.NORTH);
        }

        JLabel nameLabel = new JLabel(name, SwingConstants.CENTER);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        header.add(nameLabel, BorderLayout.CENTER);

        add(header);
        add(new JSeparator(SwingConstants.HORIZONTAL));

        // Fields
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        fieldsPanel.setBackground(CONTENT_BG);
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));

        if (fields instanceof List) {
            for (Object f : (List<Object>) fields) {
                fieldsPanel.add(makeEntryLabel(f.toString()));
            }
        } else if (fields instanceof String && !((String) fields).isEmpty()) {
            for (String f : ((String) fields).split(",")) {
                fieldsPanel.add(makeEntryLabel(f.trim()));
            }
        }
        add(fieldsPanel);
        add(new JSeparator(SwingConstants.HORIZONTAL));

        // Methods
        JPanel methodsPanel = new JPanel();
        methodsPanel.setLayout(new BoxLayout(methodsPanel, BoxLayout.Y_AXIS));
        methodsPanel.setBackground(CONTENT_BG);
        methodsPanel.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));

        if (methods instanceof List) {
            for (Object m : (List<Object>) methods) {
                methodsPanel.add(makeEntryLabel(m.toString()));
            }
        } else if (methods instanceof String && !((String) methods).isEmpty()) {
            for (String m : ((String) methods).split(",")) {
                methodsPanel.add(makeEntryLabel(m.trim()));
            }
        }
        add(methodsPanel);
    }

    private JLabel makeEntryLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Monospaced", Font.PLAIN, 11));
        return label;
    }
}
