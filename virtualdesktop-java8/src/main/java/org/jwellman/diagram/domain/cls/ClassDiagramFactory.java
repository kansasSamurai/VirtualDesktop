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
import java.awt.LayoutManager;
import java.awt.Rectangle;
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
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.Scrollable;
import javax.swing.border.Border;

import org.jwellman.diagram.api.CanvasComponentFactory;
import org.jwellman.diagram.api.CanvasTheme;
import org.jwellman.swing.colorchooser.SwatchColorPicker;
import org.jwellman.swing.layout.FluidConstraint;
import org.jwellman.swing.layout.FluidLayout;

/**
 * Factory for class-diagram nodes. Supports node types "CLASS" and "INTERFACE".
 */
public class ClassDiagramFactory implements CanvasComponentFactory {

    private static final Font   VISIBILITY_FONT  = new Font("Monospaced", Font.BOLD, 11);
    private static final Insets VISIBILITY_INSETS = new Insets(1, 3, 1, 3);
    private static final Insets ROW_INSETS        = new Insets(1, 1, 1, 1);
    private static final Border EDITOR_PADDING    = BorderFactory.createEmptyBorder(8, 8, 8, 8);
    private static final Border PALETTE_PADDING   = BorderFactory.createEmptyBorder(8, 8, 8, 8);
    private static final Border EMPTY_BORDER      = BorderFactory.createEmptyBorder();
    private static final Border BTN_ROW_BORDER    = BorderFactory.createEmptyBorder(4, 0, 0, 0);
    private static final Border DETAILS_BORDER    = BorderFactory.createEmptyBorder(4, 6, 4, 6);

    private CanvasTheme theme;

    public ClassDiagramFactory(CanvasTheme theme) {
        this.theme = theme;
    }

    @Override
    public void setTheme(CanvasTheme theme) {
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
        panel.setBorder(PALETTE_PADDING);

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
        panel.setBorder(EDITOR_PADDING);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JLabel typeLabel = new JLabel("Type: " + nodeType);
        typeLabel.setFont(typeLabel.getFont().deriveFont(Font.BOLD));
        form.add(typeLabel);
        form.add(Box.createVerticalStrut(8));

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
        form.add(Box.createVerticalStrut(6));

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

        final List<JPanel> fieldRows  = new ArrayList<>();
        final List<JPanel> methodRows = new ArrayList<>();

        final JPanel fieldsRowsPanel  = new JPanel(new GridBagLayout());
        final JPanel methodsRowsPanel = new JPanel(new GridBagLayout());

        for (Map<String, String> entry : fieldsList) {
            JPanel row = buildDetailRow(entry, fieldsRowsPanel, fieldRows);
            fieldRows.add(row);
        }
        detailsRebuild(fieldsRowsPanel, fieldRows);

        for (Map<String, String> entry : methodsList) {
            JPanel row = buildDetailRow(entry, methodsRowsPanel, methodRows);
            methodRows.add(row);
        }
        detailsRebuild(methodsRowsPanel, methodRows);

        JPanel fieldsSection = new JPanel(new BorderLayout(0, 2));
        fieldsSection.setBorder(BorderFactory.createTitledBorder("Fields"));
        fieldsSection.add(fieldsRowsPanel, BorderLayout.CENTER);
        fieldsSection.add(buildDetailsAddRow(fieldsRowsPanel, fieldRows, "-"), BorderLayout.SOUTH);

        JPanel methodsSection = new JPanel(new BorderLayout(0, 2));
        methodsSection.setBorder(BorderFactory.createTitledBorder("Methods"));
        methodsSection.add(methodsRowsPanel, BorderLayout.CENTER);
        methodsSection.add(buildDetailsAddRow(methodsRowsPanel, methodRows, "+"), BorderLayout.SOUTH);

        FluidConstraint half = new FluidConstraint(12, 6, 6, 6, 6);
        JPanel body = new ScrollablePanel(new FluidLayout(4, 4));
        body.add(fieldsSection,  half);
        body.add(methodsSection, half);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(EMPTY_BORDER);

        JButton applyBtn = new JButton("Apply Changes");
        applyBtn.addActionListener(e -> {
            properties.put("fields",  detailsCollect(fieldRows));
            properties.put("methods", detailsCollect(methodRows));
            if (onCommit != null) {
                onCommit.run();
            }
        });
        JPanel btnRow = new JPanel(new BorderLayout());
        btnRow.setBorder(BTN_ROW_BORDER);
        btnRow.add(applyBtn, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(DETAILS_BORDER);
        panel.add(scroll,  BorderLayout.CENTER);
        panel.add(btnRow,  BorderLayout.SOUTH);
        return panel;
    }

    private static JPanel detailsVisPanel(String initial) {
        String[] opts = {"+", "-", "#", "~"};
        JPanel p = new JPanel(new GridLayout(1, opts.length, 0, 0));
        p.setOpaque(false);
        ButtonGroup g = new ButtonGroup();
        for (String vis : opts) {
            JToggleButton btn = new JToggleButton(vis);
            btn.setFont(VISIBILITY_FONT);
            btn.setFocusPainted(false);
            btn.setMargin(VISIBILITY_INSETS);
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

    private static JPanel buildDetailRow(Map<String, String> entry,
                                          JPanel rowsPanel, List<JPanel> rows) {
        JPanel row = new JPanel(new GridBagLayout());
        JPanel vis    = detailsVisPanel(entry.getOrDefault("visibility", "+"));
        JTextField tf = new JTextField(entry.getOrDefault("type", ""), 6);
        JTextField nf = new JTextField(entry.getOrDefault("name", ""), 10);
        JButton del   = new JButton("×");
        del.setFont(del.getFont().deriveFont(10f));
        del.setFocusPainted(false);
        del.addActionListener(e -> {
            rows.remove(row);
            detailsRebuild(rowsPanel, rows);
        });
        detailsLayoutRow(row, vis, tf, nf, del);
        return row;
    }

    private static JPanel buildDetailsAddRow(JPanel rowsPanel, List<JPanel> rows,
                                              String defaultVis) {
        JPanel row = new JPanel(new GridBagLayout());
        JPanel vis    = detailsVisPanel(defaultVis);
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
                JPanel newRow = buildDetailRow(detailsEntry(v, t, n), rowsPanel, rows);
                rows.add(newRow);
                detailsRebuild(rowsPanel, rows);
                for (Component c : vis.getComponents()) {
                    if (c instanceof JToggleButton) {
                        ((JToggleButton) c).setSelected(defaultVis.equals(((JToggleButton) c).getText()));
                    }
                }
                tf.setText("");
                nf.setText("");
            }
        });
        detailsLayoutRow(row, vis, tf, nf, add);
        return row;
    }

    private static void detailsRebuild(JPanel rowsPanel, List<JPanel> rows) {
        rowsPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx   = 0;
        gbc.weightx = 1.0;
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0.0;
        for (int i = 0; i < rows.size(); i++) {
            gbc.gridy = i;
            rowsPanel.add(rows.get(i), gbc);
        }
        GridBagConstraints fc = new GridBagConstraints();
        fc.gridx   = 0;
        fc.gridy   = rows.size();
        fc.weightx = 1.0;
        fc.weighty = 1.0;
        fc.fill    = GridBagConstraints.BOTH;
        rowsPanel.add(new JPanel(), fc);
        rowsPanel.revalidate();
        rowsPanel.repaint();
    }

    private static void detailsLayoutRow(JPanel row, JPanel vis,
                                          JTextField tf, JTextField nf, Component btn) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = ROW_INSETS;
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

    private static List<Map<String, String>> detailsCollect(List<JPanel> rows) {
        List<Map<String, String>> list = new ArrayList<>();
        for (JPanel row : rows) {
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

    /**
     * JPanel that tracks the scroll pane viewport width, allowing FluidLayout to
     * recalculate column breakpoints correctly when the enclosing window resizes.
     * Without this, JViewport sizes the panel to its stale preferredSize rather than
     * the current viewport width, causing content to be clipped on resize.
     */
    @SuppressWarnings("serial")
    private static final class ScrollablePanel extends JPanel implements Scrollable {

        ScrollablePanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 20;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 80;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            // Tells JViewport to resize this panel to viewport width on every layout pass
            // instead of using the stale preferredSize from the initial render.
            // This is what lets FluidLayout see the correct width and reflow columns.
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
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
