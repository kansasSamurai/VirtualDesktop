package org.jwellman.diagram.domain.cls;

import java.awt.BorderLayout;
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jwellman.diagram.api.CanvasComponentFactory;
import org.jwellman.diagram.api.CanvasTheme;

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
        Object fields     = properties.get("fields");
        Object methods    = properties.get("methods");
        return new ClassNodeContent(name, nodeType, stereotype, fields, methods, theme, onModified);
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
        return "Class Nodes";
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
        form.add(stereoRow);
        form.add(Box.createVerticalStrut(4));

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
        // Enter key commits via focus-lost (single commit path)
        nameField.addActionListener(e -> nameField.transferFocus());

        nameRow.add(nameField, BorderLayout.CENTER);
        form.add(nameRow);

        panel.add(form, BorderLayout.NORTH);
        return panel;
    }
}
