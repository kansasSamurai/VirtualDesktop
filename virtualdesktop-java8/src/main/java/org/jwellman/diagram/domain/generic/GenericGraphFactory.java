package org.jwellman.diagram.domain.generic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
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
import javax.swing.border.Border;

import org.jwellman.diagram.FontPickerPanel;
import org.jwellman.diagram.api.CanvasComponentFactory;
import org.jwellman.swing.colorchooser.SwatchColorPicker;

/**
 * Factory for generic graph nodes on plain (domain-less) diagrams. Supports node
 * types "RECT_NODE" and "CIRCLE_NODE" — connectable counterparts to the
 * decorative {@code DiagramShape} rectangle/circle, which remain available
 * separately (via the toolbar) for non-connectable, purely visual use.
 */
public class GenericGraphFactory implements CanvasComponentFactory {

    /** Node type identifiers, shared across this package to avoid literal-string drift. */
    public static final String NODE_TYPE_RECT   = "RECT_NODE";
    public static final String NODE_TYPE_CIRCLE = "CIRCLE_NODE";

    private static final Border PALETTE_PADDING = BorderFactory.createEmptyBorder(8, 8, 8, 8);
    private static final Border EDITOR_PADDING  = BorderFactory.createEmptyBorder(8, 8, 8, 8);

    private static final Color DEFAULT_FILL   = new Color(173, 216, 230, 200);
    private static final Color DEFAULT_BORDER = new Color(70, 130, 180);

    // Matches DiagramText's own default font exactly, per "same as the text shape."
    private static final Font DEFAULT_FONT = new Font("Segoe UI", Font.BOLD, 16);

    // Deliberately theme-independent, like the decorative DiagramShape rectangle/circle
    // it mirrors — fixed default colors, no setTheme() override (default no-op applies).

    @Override
    public JPanel createContentFor(String nodeType, Map<String, Object> properties) {
        String label = (String) properties.getOrDefault("label", "");
        Color fill   = decodeColor(properties.get("fillColor"), DEFAULT_FILL);
        Color border = decodeColor(properties.get("borderColor"), DEFAULT_BORDER);
        // No fixed default: mirrors whatever the border color currently is until the
        // user explicitly picks a text color, so it keeps tracking border changes too.
        Color text = decodeColor(properties.get("textColor"), border);
        Font font  = decodeFont(properties);
        return new GenericNodeContent(nodeType, label, fill, border, text, font);
    }

    @Override
    public String[] getPortIds(String nodeType) {
        return new String[]{"N", "S", "E", "W"};
    }

    /** Must match the name this factory is registered under in SpecDiagramTool. */
    @Override
    public String getDomainTypeId() {
        return "Plain Diagram";
    }

    @Override
    public String getNodePaletteTitle() {
        return "Shapes";
    }

    @Override
    public JPanel createNodePalettePanel(BiConsumer<String, Map<String, Object>> addNode) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(PALETTE_PADDING);

        JButton addRectBtn = new JButton("Add Rectangle");
        addRectBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        addRectBtn.addActionListener(e -> {
            Map<String, Object> props = new HashMap<>();
            props.put("label", "Node");
            addNode.accept(NODE_TYPE_RECT, props);
        });

        JButton addCircleBtn = new JButton("Add Circle");
        addCircleBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        addCircleBtn.addActionListener(e -> {
            Map<String, Object> props = new HashMap<>();
            props.put("label", "Node");
            addNode.accept(NODE_TYPE_CIRCLE, props);
        });

        panel.add(addRectBtn);
        panel.add(Box.createVerticalStrut(4));
        panel.add(addCircleBtn);
        return panel;
    }

    @Override
    public JPanel createPropertyEditorFor(String nodeType,
                                          Map<String, Object> properties,
                                          Runnable onChanged) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(EDITOR_PADDING);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        String typeText = NODE_TYPE_CIRCLE.equals(nodeType) ? "Type: Circle" : "Type: Rectangle";
        JLabel typeLabel = new JLabel(typeText);
        typeLabel.setFont(typeLabel.getFont().deriveFont(Font.BOLD));
        form.add(typeLabel);
        form.add(Box.createVerticalStrut(8));

        // Label
        JPanel labelRow = new JPanel(new BorderLayout(4, 0));
        labelRow.add(new JLabel("Label:"), BorderLayout.WEST);
        JTextField labelField = new JTextField((String) properties.getOrDefault("label", ""));
        labelField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                String val = labelField.getText().trim();
                if (!val.equals(properties.getOrDefault("label", ""))) {
                    properties.put("label", val);
                    onChanged.run();
                }
            }
        });
        labelField.addActionListener(e -> labelField.transferFocus());
        labelRow.add(labelField, BorderLayout.CENTER);
        labelRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, labelRow.getPreferredSize().height));
        form.add(labelRow);
        form.add(Box.createVerticalStrut(8));

        // Fill color
        JLabel fillLabel = new JLabel("Fill color:");
        fillLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(fillLabel);
        form.add(Box.createVerticalStrut(2));
        Color currentFill = decodeColor(properties.get("fillColor"), DEFAULT_FILL);
        SwatchColorPicker fillPicker = new SwatchColorPicker(currentFill, chosen -> {
            properties.put("fillColor", encodeColor(chosen));
            onChanged.run();
        });
        fillPicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(fillPicker);
        form.add(Box.createVerticalStrut(8));

        // Border color
        JLabel borderLabel = new JLabel("Border color:");
        borderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(borderLabel);
        form.add(Box.createVerticalStrut(2));
        Color currentBorder = decodeColor(properties.get("borderColor"), DEFAULT_BORDER);
        SwatchColorPicker borderPicker = new SwatchColorPicker(currentBorder, chosen -> {
            properties.put("borderColor", encodeColor(chosen));
            onChanged.run();
        });
        borderPicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(borderPicker);
        form.add(Box.createVerticalStrut(8));

        // Text color — defaults to (and keeps tracking) the border color until picked explicitly
        JLabel textColorLabel = new JLabel("Text color:");
        textColorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(textColorLabel);
        form.add(Box.createVerticalStrut(2));
        Color currentText = decodeColor(properties.get("textColor"), currentBorder);
        SwatchColorPicker textColorPicker = new SwatchColorPicker(currentText, chosen -> {
            properties.put("textColor", encodeColor(chosen));
            onChanged.run();
        });
        textColorPicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(textColorPicker);
        form.add(Box.createVerticalStrut(8));

        // Font — the same picker used for the decorative text shape
        Font currentFont = decodeFont(properties);
        FontPickerPanel fontPicker = new FontPickerPanel(
            currentFont.getName(), currentFont.getSize(), currentFont.getStyle(), font -> {
                properties.put("fontName", font.getName());
                properties.put("fontSize", font.getSize());
                properties.put("fontStyle", font.getStyle());
                onChanged.run();
            });
        fontPicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(fontPicker);

        panel.add(form, BorderLayout.NORTH);
        return panel;
    }

    // Color stored as "#RRGGBB" hex string for clean JSON round-trip (matches ClassDiagramFactory).
    private static String encodeColor(Color c) {
        return String.format("#%06X", c.getRGB() & 0xFFFFFF);
    }

    private static Color decodeColor(Object value, Color fallback) {
        if (value instanceof String) {
            try {
                return Color.decode((String) value);
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        return fallback;
    }

    private static Font decodeFont(Map<String, Object> properties) {
        String name = (String) properties.getOrDefault("fontName", DEFAULT_FONT.getName());
        Object sizeVal = properties.get("fontSize");
        int size = (sizeVal instanceof Number) ? ((Number) sizeVal).intValue() : DEFAULT_FONT.getSize();
        Object styleVal = properties.get("fontStyle");
        int style = (styleVal instanceof Number) ? ((Number) styleVal).intValue() : DEFAULT_FONT.getStyle();
        return new Font(name, style, size);
    }
}
