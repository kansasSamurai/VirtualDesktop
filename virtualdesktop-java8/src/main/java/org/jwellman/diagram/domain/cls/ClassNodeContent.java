package org.jwellman.diagram.domain.cls;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.jwellman.diagram.api.CanvasTheme;

/**
 * Pure-Swing JPanel representing a class or interface in a class diagram.
 * Has no dependency on diagram framework classes.
 */
public class ClassNodeContent extends JPanel {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public ClassNodeContent(String name, String stereotype, Object fields, Object methods,
                            CanvasTheme theme) {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createLineBorder(theme.getNodeBorderColor(), 1));
        setBackground(theme.getNodeBodyBackground());

        // Header
        boolean isInterface = "INTERFACE".equalsIgnoreCase(stereotype);
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(theme.getNodeHeaderBackground(stereotype));
        header.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));


        final String stereoString = isInterface ? "«interface»" : " ";
        JLabel stereoLabel = new JLabel(stereoString, SwingConstants.CENTER);
        stereoLabel.setFont(new Font("SansSerif", Font.PLAIN, 9));
        stereoLabel.setForeground(new Color(60, 100, 60));
        header.add(stereoLabel, BorderLayout.NORTH);

        JLabel nameLabel = new JLabel(name, SwingConstants.CENTER);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        nameLabel.setForeground(theme.getTextColor());
        header.add(nameLabel, BorderLayout.CENTER);

        add(header, itemConstraints());
        add(new JSeparator(SwingConstants.HORIZONTAL), itemConstraints());

        // Fields
        JPanel fieldsPanel = makeSection(theme);

        if (fields instanceof List) {
            for (Object f : (List<Object>) fields) {
                fieldsPanel.add(makeEntryLabel(f.toString(), theme));
            }
        } else if (fields instanceof String && !((String) fields).isEmpty()) {
            for (String f : ((String) fields).split(",")) {
                fieldsPanel.add(makeEntryLabel(f.trim(), theme));
            }
        }
        add(fieldsPanel, itemConstraints());
        add(new JSeparator(SwingConstants.HORIZONTAL), itemConstraints());

        // Methods
        JPanel methodsPanel = makeSection(theme);

        if (methods instanceof List) {
            for (Object m : (List<Object>) methods) {
                methodsPanel.add(makeEntryLabel(m.toString(), theme));
            }
        } else if (methods instanceof String && !((String) methods).isEmpty()) {
            for (String m : ((String) methods).split(",")) {
                methodsPanel.add(makeEntryLabel(m.trim(), theme));
            }
        }
        add(methodsPanel, itemConstraints());
        add(new JSeparator(SwingConstants.HORIZONTAL), itemConstraints());

        // Filler: absorbs all remaining vertical space
        JPanel filler = new JPanel();
        filler.setBackground(theme.getNodeBodyBackground());
        GridBagConstraints fillerGbc = itemConstraints();
        fillerGbc.weighty = 1.0;
        fillerGbc.fill = GridBagConstraints.BOTH;
        add(filler, fillerGbc);
    }

    // TODO: replace with equivalent utility from org.jwellman:swing-utils
    private GridBagConstraints itemConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.NORTH;
        return gbc;
    }

    private JPanel makeSection(CanvasTheme theme) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(theme.getNodeBodyBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        return panel;
    }

    private JLabel makeEntryLabel(String text, CanvasTheme theme) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Monospaced", Font.PLAIN, 11));
        label.setForeground(theme.getTextColor());
        return label;
    }

}
