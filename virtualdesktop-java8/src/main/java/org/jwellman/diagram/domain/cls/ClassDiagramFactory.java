package org.jwellman.diagram.domain.cls;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

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
        abstractCheck.setEnabled(!"INTERFACE".equalsIgnoreCase(nodeType));
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

    @Override
    public JPanel createDetailsPanelFor(String nodeType,
                                         Map<String, Object> properties,
                                         Runnable onCommit) {
        List<Map<String, String>> fieldsList  = detailsPromote(properties.get("fields"));
        List<Map<String, String>> methodsList = detailsPromote(properties.get("methods"));

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        body.add(detailsLabel("Fields"));
        final JPanel fieldsRows = new JPanel();
        fieldsRows.setLayout(new BoxLayout(fieldsRows, BoxLayout.Y_AXIS));
        fieldsRows.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Map<String, String> entry : fieldsList) {
            fieldsRows.add(detailsMemberRow(entry, fieldsRows));
        }
        body.add(fieldsRows);
        body.add(detailsAddRow(fieldsRows));
        body.add(Box.createVerticalStrut(4));
        body.add(detailsSeparator());
        body.add(Box.createVerticalStrut(4));

        body.add(detailsLabel("Methods"));
        final JPanel methodsRows = new JPanel();
        methodsRows.setLayout(new BoxLayout(methodsRows, BoxLayout.Y_AXIS));
        methodsRows.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Map<String, String> entry : methodsList) {
            methodsRows.add(detailsMemberRow(entry, methodsRows));
        }
        body.add(methodsRows);
        body.add(detailsAddRow(methodsRows));

        JScrollPane scroll = new JScrollPane(body);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);

        JButton applyBtn = new JButton("Apply Changes");
        applyBtn.addActionListener(e -> {
            properties.put("fields",  detailsCollect(fieldsRows));
            properties.put("methods", detailsCollect(methodsRows));
            if (onCommit != null) {
                onCommit.run();
            }
        });
        JPanel btnRow = new JPanel(new BorderLayout());
        btnRow.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        btnRow.add(applyBtn, BorderLayout.EAST);
        panel.add(btnRow, BorderLayout.SOUTH);

        return panel;
    }

    private static JLabel detailsLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 10f));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(BorderFactory.createEmptyBorder(2, 0, 1, 0));
        return lbl;
    }

    private static JSeparator detailsSeparator() {
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        return sep;
    }

    private static JPanel detailsVisPanel(String initial) {
        String[] opts = {"+", "-", "#", "~"};
        JPanel p = new JPanel(new GridLayout(1, opts.length, 0, 0));
        p.setOpaque(false);
        ButtonGroup g = new ButtonGroup();
        for (String vis : opts) {
            JToggleButton btn = new JToggleButton(vis);
            btn.setFont(new Font("Monospaced", Font.BOLD, 11));
            btn.setFocusPainted(false);
            btn.setMargin(new Insets(1, 3, 1, 3));
            btn.setSelected(vis.equals(initial));
            g.add(btn);
            p.add(btn);
        }
        return p;
    }

    private static String detailsGetVis(JPanel visPanel) {
        for (Component c : visPanel.getComponents()) {
            if (c instanceof JToggleButton && ((JToggleButton) c).isSelected()) {
                return ((JToggleButton) c).getText();
            }
        }
        return "+";
    }

    private static JPanel detailsMemberRow(Map<String, String> entry, JPanel rowsPanel) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel vis    = detailsVisPanel(entry.getOrDefault("visibility", "+"));
        JTextField tf = new JTextField(entry.getOrDefault("type", ""), 6);
        JTextField nf = new JTextField(entry.getOrDefault("name", ""), 10);
        JButton del   = new JButton("×");
        del.setFont(del.getFont().deriveFont(10f));
        del.setFocusPainted(false);
        del.addActionListener(e -> {
            rowsPanel.remove(row);
            rowsPanel.revalidate();
            rowsPanel.repaint();
        });
        detailsLayoutRow(row, vis, tf, nf, del);
        return row;
    }

    private static JPanel detailsAddRow(JPanel targetRows) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel vis    = detailsVisPanel("+");
        JTextField tf = new JTextField(6);
        JTextField nf = new JTextField(10);
        JButton add   = new JButton("+");
        add.setFont(add.getFont().deriveFont(10f));
        add.setFocusPainted(false);
        add.addActionListener(e -> {
            String v = detailsGetVis(vis);
            String t = tf.getText().trim();
            String n = nf.getText().trim();
            if (!n.isEmpty()) {
                targetRows.add(detailsMemberRow(detailsEntry(v, t, n), targetRows));
                targetRows.revalidate();
                targetRows.repaint();
                ((JToggleButton) vis.getComponent(0)).setSelected(true);
                tf.setText("");
                nf.setText("");
            }
        });
        detailsLayoutRow(row, vis, tf, nf, add);
        return row;
    }

    private static void detailsLayoutRow(JPanel row, JPanel vis,
                                          JTextField tf, JTextField nf, Component btn) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(1, 1, 1, 1);
        c.fill   = GridBagConstraints.HORIZONTAL;
        c.gridy  = 0;

        c.gridx   = 0;
        c.weightx = 0;
        row.add(vis, c);

        c.gridx   = 1;
        c.weightx = 0.35;
        row.add(tf, c);

        c.gridx   = 2;
        c.weightx = 0.65;
        row.add(nf, c);

        c.gridx   = 3;
        c.weightx = 0;
        row.add(btn, c);
    }

    private static List<Map<String, String>> detailsCollect(JPanel rowsPanel) {
        List<Map<String, String>> list = new ArrayList<>();
        for (Component comp : rowsPanel.getComponents()) {
            if (!(comp instanceof JPanel)) {
                continue;
            }
            JPanel row = (JPanel) comp;
            Component[] ch = row.getComponents();
            if (ch.length < 3 || !(ch[0] instanceof JPanel) || !(ch[2] instanceof JTextField)) {
                continue;
            }
            String vis  = detailsGetVis((JPanel) ch[0]);
            String type = ch[1] instanceof JTextField ? ((JTextField) ch[1]).getText().trim() : "";
            String name = ((JTextField) ch[2]).getText().trim();
            if (!name.isEmpty()) {
                list.add(detailsEntry(vis, type, name));
            }
        }
        return list;
    }

    private static Map<String, String> detailsEntry(String vis, String type, String name) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("visibility", vis  != null ? vis  : "+");
        m.put("type",       type != null ? type : "");
        m.put("name",       name != null ? name : "");
        return m;
    }

    @SuppressWarnings("rawtypes")
    private static List<Map<String, String>> detailsPromote(Object raw) {
        List<Map<String, String>> list = new ArrayList<>();
        if (raw == null || !(raw instanceof List)) {
            return list;
        }
        for (Object item : (List) raw) {
            if (item instanceof Map) {
                Map<?, ?> m  = (Map<?, ?>) item;
                String vis   = m.get("visibility") != null ? m.get("visibility").toString() : "+";
                String type  = m.get("type")       != null ? m.get("type").toString()       : "";
                String name  = m.get("name")       != null ? m.get("name").toString()       : "";
                list.add(detailsEntry(vis, type, name));
            } else if (item != null) {
                list.add(detailsLegacy(item.toString()));
            }
        }
        return list;
    }

    private static Map<String, String> detailsLegacy(String s) {
        s = s.trim();
        String vis  = "+";
        String rest = s;
        if (!s.isEmpty() && "+-#~".indexOf(s.charAt(0)) >= 0) {
            vis  = String.valueOf(s.charAt(0));
            rest = s.substring(1).trim();
        }
        int colon = rest.indexOf(':');
        if (colon >= 0) {
            return detailsEntry(vis, rest.substring(colon + 1).trim(),
                                rest.substring(0, colon).trim());
        }
        return detailsEntry(vis, "", rest);
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
