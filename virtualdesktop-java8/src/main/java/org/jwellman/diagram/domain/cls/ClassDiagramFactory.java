package org.jwellman.diagram.domain.cls;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jwellman.diagram.api.CanvasComponentFactory;
import org.jwellman.diagram.api.CanvasTheme;
import org.jwellman.swing.colorchooser.SwatchColorPicker;

/**
 * Factory for class-diagram nodes. Supports node types "CLASS" and "INTERFACE".
 */
public class ClassDiagramFactory implements CanvasComponentFactory {

    private final CanvasTheme theme;

    public ClassDiagramFactory(CanvasTheme theme) {
        this.theme = theme;
    }

    @Override
    public JPanel createContentFor(String nodeType, Map<String, Object> properties) {
        return createContentFor(nodeType, properties, null);
    }

    @Override
    public JPanel createContentFor(String nodeType, Map<String, Object> properties,
                                   Runnable onModified) {
        String name       = (String) properties.getOrDefault("name", "Unnamed");
        String stereotype = (String) properties.getOrDefault("stereotype", null);
        Color  headerBg   = decodeColor(properties.get("headerBackground"));
        Object fields     = properties.get("fields");
        Object methods    = properties.get("methods");
        return new ClassNodeContent(name, nodeType, stereotype, headerBg,
                                    fields, methods, theme, onModified, properties);
    }

    @Override
    public String[] getPortIds(String nodeType) {
        return new String[]{"N", "S", "E", "W"};
    }

    @Override
    public String getDomainTypeId() {
        return "Class Diagram";
    }

    @Override
    public String getNodePaletteTitle() {
        return "Types";
    }

    @Override
    public JPanel createNodePalettePanel(BiConsumer<String, Map<String, Object>> addNode) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton addClassBtn = new JButton("Add Class");
        addClassBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        addClassBtn.addActionListener(e -> {
            Map<String, Object> props = new HashMap<>();
            props.put("name", "NewClass");
            addNode.accept("CLASS", props);
        });

        JButton addIfaceBtn = new JButton("Add Interface");
        addIfaceBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        addIfaceBtn.addActionListener(e -> {
            Map<String, Object> props = new HashMap<>();
            props.put("name", "NewInterface");
            addNode.accept("INTERFACE", props);
        });

        panel.add(addClassBtn);
        panel.add(Box.createVerticalStrut(4));
        panel.add(addIfaceBtn);
        return panel;
    }

    @Override
    public JPanel createPropertyEditorFor(String nodeType,
                                          Map<String, Object> properties,
                                          Runnable onChanged) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JLabel typeLabel = new JLabel("Type: " + nodeType);
        typeLabel.setFont(typeLabel.getFont().deriveFont(Font.BOLD));
        form.add(typeLabel);
        form.add(Box.createVerticalStrut(8));

        // Abstract
        JCheckBox abstractCheck = new JCheckBox("Abstract",
            Boolean.TRUE.equals(properties.get("abstract")));
        abstractCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        abstractCheck.addActionListener(e -> {
            properties.put("abstract", abstractCheck.isSelected());
            onChanged.run();
        });
        form.add(abstractCheck);
        form.add(Box.createVerticalStrut(6));

        // Name
        JPanel nameRow = new JPanel(new BorderLayout(4, 0));
        nameRow.add(new JLabel("Name:"), BorderLayout.WEST);
        JTextField nameField = new JTextField((String) properties.getOrDefault("name", ""));
        nameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                String val = nameField.getText().trim();
                if (!val.isEmpty() && !val.equals(properties.get("name"))) {
                    properties.put("name", val);
                    onChanged.run();
                }
            }
        });
        nameField.addActionListener(e -> nameField.transferFocus());
        nameRow.add(nameField, BorderLayout.CENTER);
        nameRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameRow.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, nameRow.getPreferredSize().height));
        form.add(nameRow);
        form.add(Box.createVerticalStrut(4));

        // Stereotype
        JPanel stereoRow = new JPanel(new BorderLayout(4, 0));
        stereoRow.add(new JLabel("Stereotype:"), BorderLayout.WEST);
        JTextField stereoField = new JTextField((String) properties.getOrDefault("stereotype", ""));
        stereoField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                String val = stereoField.getText().trim();
                if (!val.equals(properties.getOrDefault("stereotype", ""))) {
                    properties.put("stereotype", val);
                    onChanged.run();
                }
            }
        });
        stereoField.addActionListener(e -> stereoField.transferFocus());
        stereoRow.add(stereoField, BorderLayout.CENTER);
        stereoRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        stereoRow.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, stereoRow.getPreferredSize().height));
        form.add(stereoRow);
        form.add(Box.createVerticalStrut(8));

        // Header background color
        JLabel colorLabel = new JLabel("Header color:");
        colorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(colorLabel);
        form.add(Box.createVerticalStrut(2));

        Color currentBg = decodeColor(properties.get("headerBackground"));
        if (currentBg == null) {
            currentBg = theme.getNodeHeaderBackground(nodeType);
        }
        SwatchColorPicker colorPicker = new SwatchColorPicker(currentBg, chosen -> {
            properties.put("headerBackground", encodeColor(chosen));
            onChanged.run();
        });
        colorPicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(colorPicker);

        panel.add(form, BorderLayout.NORTH);
        return panel;
    }

    // Color stored as "#RRGGBB" hex string for clean JSON round-trip.
    private static String encodeColor(Color c) {
        return String.format("#%06X", c.getRGB() & 0xFFFFFF);
    }

    private static Color decodeColor(Object value) {
        if (value instanceof String) {
            try {
                return Color.decode((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
