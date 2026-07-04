package org.jwellman.diagram.domain.cls;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import org.jwellman.diagram.api.CanvasTheme;

/**
 * Pure-Swing JPanel representing a class or interface in a class diagram.
 * Has no dependency on diagram framework classes.
 *
 * Supports two display modes toggled by a button in the header:
 *   view  — readonly labels (default)
 *   edit  — editable rows with visibility combo, type field, name field
 *
 * Member data is stored as List<Map<String,String>> with keys "visibility", "type", "name".
 * Legacy List<String> entries are accepted (deprecated) and promoted to structured format
 * on first entry into edit mode.
 */
public class ClassNodeContent extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String[] VISIBILITY_OPTIONS = {"+", "-", "#", "~"};
    private static final String CARD_VIEW = "view";
    private static final String CARD_EDIT = "edit";

    private List<Map<String, String>> fieldsList;
    private List<Map<String, String>> methodsList;
    @SuppressWarnings("unused")
    private final String nodeType;
    private final CanvasTheme theme;
    private final Runnable onModified;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private JPanel viewCard;
    private JPanel fieldsRowsPanel;
    private JPanel methodsRowsPanel;

    public ClassNodeContent(String name, String nodeType, String stereotype,
                            java.awt.Color headerBackground,
                            Object fields, Object methods,
                            CanvasTheme theme, Runnable onModified) {
        this.nodeType   = nodeType;
        this.theme      = theme;
        this.onModified = onModified;
        this.fieldsList  = promoteToStructured(fields);
        this.methodsList = promoteToStructured(methods);

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createLineBorder(theme.getNodeBorderColor(), 1));
        setBackground(theme.getNodeBodyBackground());

        // Header — always visible above the card panel
        JPanel header = buildHeader(name, nodeType, stereotype, headerBackground);
        add(header, itemConstraints(false));

        // Card panel fills remaining space
        viewCard = buildViewCard();
        cardPanel.add(viewCard, CARD_VIEW);
        cardPanel.add(buildEditCard(), CARD_EDIT);
        cardLayout.show(cardPanel, CARD_VIEW);

        add(cardPanel, itemConstraints(true));
    }

    // ---------------------------------------------------------------
    // Header
    // ---------------------------------------------------------------

    private JPanel buildHeader(String name, String nodeType, String stereotype,
                               java.awt.Color headerBackground) {
        JPanel header = new JPanel(new BorderLayout());
        java.awt.Color bg = (headerBackground != null)
                ? headerBackground
                : theme.getNodeHeaderBackground(nodeType);
        header.setBackground(bg);
        header.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 4));

        JPanel labels = new JPanel();
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        labels.setOpaque(false);

        String stereoString;
        if (stereotype != null && !stereotype.isEmpty()) {
            stereoString = "«" + stereotype + "»";
        } else if ("INTERFACE".equalsIgnoreCase(nodeType)) {
            stereoString = "«interface»";
        } else {
            stereoString = " ";
        }
        JLabel stereoLabel = new JLabel(stereoString, SwingConstants.CENTER);
        stereoLabel.setFont(new Font("SansSerif", Font.PLAIN, 9));
        stereoLabel.setForeground(theme.getStereotypeTextColor());
        stereoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        labels.add(stereoLabel);

        JLabel nameLabel = new JLabel(name, SwingConstants.CENTER);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        nameLabel.setForeground(theme.getTextColor());
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        labels.add(nameLabel);

        JToggleButton editToggle = new JToggleButton("✎");
        editToggle.setFont(editToggle.getFont().deriveFont(10f));
        editToggle.setForeground(theme.getTextColor());
        editToggle.setFocusPainted(false);
        editToggle.setContentAreaFilled(false);
        editToggle.setOpaque(false);
        editToggle.setToolTipText("Edit fields and methods");
        editToggle.addActionListener(e -> {
            if (editToggle.isSelected()) {
                enterEditMode();
            } else {
                commitAndShowView();
            }
        });

        // West spacer mirrors the toggle width so the center labels stay truly centered.
        header.add(Box.createRigidArea(editToggle.getPreferredSize()), BorderLayout.WEST);
        header.add(labels, BorderLayout.CENTER);
        header.add(editToggle, BorderLayout.EAST);

        return header;
    }

    // ---------------------------------------------------------------
    // View card
    // ---------------------------------------------------------------

    private JPanel buildViewCard() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(theme.getNodeBodyBackground());
        populateViewCard(card);
        return card;
    }

    private void populateViewCard(JPanel card) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx   = 0;
        gbc.gridy   = GridBagConstraints.RELATIVE;
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;

        card.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);

        JPanel fieldsSection = makeSection();
        for (Map<String, String> entry : fieldsList) {
            fieldsSection.add(makeEntryLabel(formatEntry(entry)));
        }
        card.add(fieldsSection, gbc);
        card.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);

        JPanel methodsSection = makeSection();
        for (Map<String, String> entry : methodsList) {
            methodsSection.add(makeEntryLabel(formatEntry(entry)));
        }
        card.add(methodsSection, gbc);
        card.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);

        // Filler row absorbs all remaining vertical space, anchoring content to the top.
        GridBagConstraints fillerGbc = new GridBagConstraints();
        fillerGbc.gridx   = 0;
        fillerGbc.gridy   = GridBagConstraints.RELATIVE;
        fillerGbc.fill    = GridBagConstraints.BOTH;
        fillerGbc.weightx = 1.0;
        fillerGbc.weighty = 1.0;
        JPanel filler = new JPanel();
        filler.setBackground(theme.getNodeBodyBackground());
        card.add(filler, fillerGbc);
    }

    private void rebuildViewCard() {
        viewCard.removeAll();
        populateViewCard(viewCard);
        viewCard.revalidate();
        viewCard.repaint();
    }

    // ---------------------------------------------------------------
    // Edit card
    // ---------------------------------------------------------------

    private JPanel buildEditCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(theme.getNodeBodyBackground());
        card.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        card.add(makeSectionLabel("Fields"));

        fieldsRowsPanel = new JPanel();
        fieldsRowsPanel.setLayout(new BoxLayout(fieldsRowsPanel, BoxLayout.Y_AXIS));
        fieldsRowsPanel.setBackground(theme.getNodeBodyBackground());
        JScrollPane fieldsScroll = new JScrollPane(fieldsRowsPanel);
        fieldsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        fieldsScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        fieldsScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(fieldsScroll);

        card.add(buildNewEntryRow(fieldsRowsPanel));
        card.add(Box.createVerticalStrut(4));
        card.add(new JSeparator(SwingConstants.HORIZONTAL));
        card.add(Box.createVerticalStrut(4));

        card.add(makeSectionLabel("Methods"));

        methodsRowsPanel = new JPanel();
        methodsRowsPanel.setLayout(new BoxLayout(methodsRowsPanel, BoxLayout.Y_AXIS));
        methodsRowsPanel.setBackground(theme.getNodeBodyBackground());
        JScrollPane methodsScroll = new JScrollPane(methodsRowsPanel);
        methodsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        methodsScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        methodsScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(methodsScroll);

        card.add(buildNewEntryRow(methodsRowsPanel));

        return card;
    }

    private JLabel makeSectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 10f));
        lbl.setForeground(theme.getTextColor());
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(BorderFactory.createEmptyBorder(2, 0, 1, 0));
        return lbl;
    }

    private JPanel buildVisibilityPanel(String initialValue) {
        JPanel panel = new JPanel(new GridLayout(1, VISIBILITY_OPTIONS.length, 0, 0));
        panel.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (String vis : VISIBILITY_OPTIONS) {
            JToggleButton btn = new JToggleButton(vis);
            btn.setFont(new Font("Monospaced", Font.BOLD, 11));
            btn.setFocusPainted(false);
            btn.setMargin(new Insets(1, 3, 1, 3));
            btn.setSelected(vis.equals(initialValue));
            group.add(btn);
            panel.add(btn);
        }
        return panel;
    }

    private static String getSelectedVisibility(JPanel visPanel) {
        for (Component c : visPanel.getComponents()) {
            if (c instanceof JToggleButton && ((JToggleButton) c).isSelected()) {
                return ((JToggleButton) c).getText();
            }
        }
        return "+";
    }

    private JPanel buildNewEntryRow(JPanel targetRowsPanel) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setBackground(theme.getNodeBodyBackground());
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel visPanel = buildVisibilityPanel("+");
        JTextField typeField = new JTextField(6);
        JTextField nameField = new JTextField(10);
        JButton addBtn = new JButton("+");
        addBtn.setFont(addBtn.getFont().deriveFont(10f));
        addBtn.setFocusPainted(false);

        addBtn.addActionListener(e -> {
            String vis  = getSelectedVisibility(visPanel);
            String type = typeField.getText().trim();
            String nm   = nameField.getText().trim();
            if (!nm.isEmpty()) {
                Map<String, String> entry = makeEntry(vis, type, nm);
                addMemberRow(targetRowsPanel, entry);
                // Reset form: restore "+" selection, clear text fields
                ((JToggleButton) visPanel.getComponent(0)).setSelected(true);
                typeField.setText("");
                nameField.setText("");
            }
        });

        layoutRowComponents(row, visPanel, typeField, nameField, addBtn);
        return row;
    }

    private void addMemberRow(JPanel rowsPanel, Map<String, String> entry) {
        JPanel row = buildMemberRow(entry, rowsPanel);
        rowsPanel.add(row);
        rowsPanel.revalidate();
        rowsPanel.repaint();
    }

    private JPanel buildMemberRow(Map<String, String> entry, JPanel rowsPanel) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setBackground(theme.getNodeBodyBackground());
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel visPanel = buildVisibilityPanel(entry.getOrDefault("visibility", "+"));
        JTextField typeField = new JTextField(entry.getOrDefault("type", ""), 6);
        JTextField nameField = new JTextField(entry.getOrDefault("name", ""), 10);

        JButton deleteBtn = new JButton("×");
        deleteBtn.setFont(deleteBtn.getFont().deriveFont(10f));
        deleteBtn.setFocusPainted(false);
        deleteBtn.addActionListener(e -> {
            rowsPanel.remove(row);
            rowsPanel.revalidate();
            rowsPanel.repaint();
        });

        layoutRowComponents(row, visPanel, typeField, nameField, deleteBtn);
        return row;
    }

    private void layoutRowComponents(JPanel row,
                                     JPanel visPanel,
                                     JTextField typeField,
                                     JTextField nameField,
                                     Component actionBtn) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(1, 1, 1, 1);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0;

        c.gridx = 0;
        c.weightx = 0;
        row.add(visPanel, c);

        c.gridx = 1;
        c.weightx = 0.35;
        row.add(typeField, c);

        c.gridx = 2;
        c.weightx = 0.65;
        row.add(nameField, c);

        c.gridx = 3;
        c.weightx = 0;
        row.add(actionBtn, c);
    }

    // ---------------------------------------------------------------
    // Mode switching
    // ---------------------------------------------------------------

    private void enterEditMode() {
        fieldsRowsPanel.removeAll();
        for (Map<String, String> entry : fieldsList) {
            fieldsRowsPanel.add(buildMemberRow(entry, fieldsRowsPanel));
        }
        fieldsRowsPanel.revalidate();

        methodsRowsPanel.removeAll();
        for (Map<String, String> entry : methodsList) {
            methodsRowsPanel.add(buildMemberRow(entry, methodsRowsPanel));
        }
        methodsRowsPanel.revalidate();

        cardLayout.show(cardPanel, CARD_EDIT);
    }

    private void commitAndShowView() {
        fieldsList  = collectRows(fieldsRowsPanel);
        methodsList = collectRows(methodsRowsPanel);
        rebuildViewCard();
        cardLayout.show(cardPanel, CARD_VIEW);
        if (onModified != null) {
            onModified.run();
        }
    }

    // ---------------------------------------------------------------
    // Data helpers
    // ---------------------------------------------------------------

    private List<Map<String, String>> collectRows(JPanel rowsPanel) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Component comp : rowsPanel.getComponents()) {
            if (!(comp instanceof JPanel)) {
                continue;
            }
            JPanel row = (JPanel) comp;
            Component[] children = row.getComponents();
            // Row layout: [0]=visPanel  [1]=typeField  [2]=nameField  [3]=action button
            if (children.length < 3) {
                continue;
            }
            if (!(children[0] instanceof JPanel) || !(children[2] instanceof JTextField)) {
                continue;
            }
            JPanel visPanel   = (JPanel) children[0];
            JTextField typeField = children[1] instanceof JTextField ? (JTextField) children[1] : null;
            JTextField nameField = (JTextField) children[2];

            String vis  = getSelectedVisibility(visPanel);
            String type = typeField != null ? typeField.getText().trim() : "";
            String nm   = nameField.getText().trim();
            if (!nm.isEmpty()) {
                result.add(makeEntry(vis, type, nm));
            }
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
    private List<Map<String, String>> promoteToStructured(Object raw) {
        List<Map<String, String>> result = new ArrayList<>();
        if (raw == null) {
            return result;
        }
        if (raw instanceof List) {
            for (Object item : (List) raw) {
                if (item instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) item;
                    result.add(makeEntry(
                        str(m.get("visibility"), "+"),
                        str(m.get("type"), ""),
                        str(m.get("name"), "")));
                } else if (item != null) {
                    // Deprecated string format — promote best-effort
                    result.add(parseLegacyString(item.toString()));
                }
            }
        } else if (raw instanceof String) {
            // Comma-delimited deprecated format
            String s = (String) raw;
            if (!s.trim().isEmpty()) {
                for (String part : s.split(",")) {
                    result.add(parseLegacyString(part.trim()));
                }
            }
        }
        return result;
    }

    private Map<String, String> parseLegacyString(String s) {
        s = s.trim();
        String vis = "+";
        String rest = s;
        if (s.length() > 0 && "+-#~".indexOf(s.charAt(0)) >= 0) {
            vis  = String.valueOf(s.charAt(0));
            rest = s.substring(1).trim();
        }
        int colon = rest.indexOf(':');
        if (colon >= 0) {
            String name = rest.substring(0, colon).trim();
            String type = rest.substring(colon + 1).trim();
            return makeEntry(vis, type, name);
        }
        return makeEntry(vis, "", rest);
    }

    private static Map<String, String> makeEntry(String vis, String type, String name) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("visibility", vis != null ? vis : "+");
        m.put("type",       type != null ? type : "");
        m.put("name",       name != null ? name : "");
        return m;
    }

    private static String str(Object o, String fallback) {
        return o != null ? o.toString() : fallback;
    }

    private String formatEntry(Map<String, String> entry) {
        String vis  = entry.getOrDefault("visibility", "+");
        String type = entry.getOrDefault("type", "");
        String name = entry.getOrDefault("name", "");
        if (type.isEmpty()) {
            return vis + " " + name;
        }
        return vis + " " + name + " : " + type;
    }

    // ---------------------------------------------------------------
    // Layout helpers
    // ---------------------------------------------------------------

    private GridBagConstraints itemConstraints(boolean fillVertical) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx   = 0;
        gbc.gridy   = GridBagConstraints.RELATIVE;
        gbc.fill    = fillVertical ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = fillVertical ? 1.0 : 0.0;
        gbc.anchor  = GridBagConstraints.NORTH;
        return gbc;
    }

    private JPanel makeSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(theme.getNodeBodyBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        return panel;
    }

    private JLabel makeEntryLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Monospaced", Font.PLAIN, 11));
        label.setForeground(theme.getTextColor());
        return label;
    }
}
