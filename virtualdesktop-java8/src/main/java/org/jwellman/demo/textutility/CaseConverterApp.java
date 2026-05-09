package org.jwellman.demo.textutility;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <h1>Programmer's Case Converter</h1>
 * A comprehensive string manipulation tool designed to bridge the gap between 
 * various naming conventions used in software development.
 * <p>
 * This utility facilitates the rapid transformation of variable names, 
 * database columns, and CSS identifiers into readable or code-ready formats.
 * </p>
 * <h3>Key Features:</h3>
 * <ul>
 * <li><b>Multi-Convention Support:</b> Converts to camelCase, PascalCase, 
 * snake_case, kebab-case, and Plain Space-Delimited text.</li>
 * <li><b>Dual-Mode Variants:</b> Specialized buttons for snake and kebab cases 
 * to provide both LOWER and UPPER (Screaming) versions instantly.</li>
 * <li><b>Intelligent Splitting:</b> The 'Auto' mode detects word boundaries 
 * across underscores, hyphens, and spaces.</li>
 * <li><b>Camel-Splitting:</b> An optional mode to break existing CamelCase 
 * strings back into individual words.</li>
 * <li><b>Live Item Counter:</b> Tracks the number of phrases being processed 
 * from the input area.</li>
 * <li><b>Plain Case Logic:</b> Includes a toggle to capitalize the first word 
 * for sentence-like output.</li>
 * <li><b>Output Delimiter Toggle:</b> Choose between comma-separated or 
 * space-separated results for bulk lists.</li>
 * </ul>
 */
@SuppressWarnings("serial")
public class CaseConverterApp extends JFrame {

    public CaseConverterApp() {
        setTitle("Programmer's Case Converter");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        add(new CaseConverterPanel());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CaseConverterApp().setVisible(true));
    }
}

@SuppressWarnings("serial")
class CaseConverterPanel extends JPanel {

    private JTextArea inputArea;
    private JLabel itemCountLabel;
    private JComboBox<String> sourceFormatCombo;
    private JCheckBox capitalizePlainCheckbox, commaDelimitResults;
    private JTextField camelField, pascalField, snakeField, kebabField, plainField;

    public CaseConverterPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Left Side: Input ---
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setPreferredSize(new Dimension(380, 0));

        sourceFormatCombo = new JComboBox<>(new String[]{
            "Auto (Space/Underscore/Hyphen)", 
            "Strict Space Delimited", 
            "Camel/Pascal Case Splitting"
        });
        sourceFormatCombo.addActionListener(e -> updateFields());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(new JLabel("Input Phrases (One per line):"), BorderLayout.WEST);
        headerPanel.add(sourceFormatCombo, BorderLayout.EAST);

        inputArea = new JTextArea();
        inputArea.getDocument().addDocumentListener(new SimpleListener());

        JPanel inputFooter = new JPanel(new BorderLayout());
        itemCountLabel = new JLabel("Items: 0");
        JButton clearBtn = new JButton("Clear All");
        clearBtn.addActionListener(e -> inputArea.setText(""));
        
        inputFooter.add(itemCountLabel, BorderLayout.WEST);
        inputFooter.add(clearBtn, BorderLayout.EAST);

        leftPanel.add(headerPanel, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        leftPanel.add(inputFooter, BorderLayout.SOUTH);

        // --- Right Side: Results ---
        JPanel rightPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 5, 12, 5);

        // Global Output Settings
        JPanel outputSettings = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        commaDelimitResults = new JCheckBox("Comma-separate results", true);
        commaDelimitResults.addActionListener(e -> updateFields());
        outputSettings.add(commaDelimitResults);
        outputSettings.setBorder(BorderFactory.createTitledBorder("Result Formatting"));
        rightPanel.add(outputSettings, gbc);

        camelField = createReadOnlyField();
        rightPanel.add(createResultComponent("camelCase", camelField), gbc);

        pascalField = createReadOnlyField();
        rightPanel.add(createResultComponent("PascalCase", pascalField), gbc);

        snakeField = createReadOnlyField();
        rightPanel.add(createMultiButtonComponent("snake_case", snakeField), gbc);

        kebabField = createReadOnlyField();
        rightPanel.add(createMultiButtonComponent("kebab-case", kebabField), gbc);

        // Plain Case with "Capitalize First" Toggle
        plainField = createReadOnlyField();
        JPanel plainHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        plainHeader.add(new JLabel("Plain Space Delimited"));
        capitalizePlainCheckbox = new JCheckBox("Capitalize First");
        capitalizePlainCheckbox.addActionListener(e -> updateFields());
        plainHeader.add(capitalizePlainCheckbox);
        rightPanel.add(createResultComponent(plainHeader, plainField), gbc);

        gbc.weighty = 1.0;
        rightPanel.add(new JPanel(), gbc);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
    }

    private JTextField createReadOnlyField() {
        JTextField field = new JTextField();
        field.setEditable(false);
        field.setBackground(new Color(242, 242, 242));
        return field;
    }

    private JPanel createResultComponent(Object header, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(5, 2));
        JButton copyBtn = new JButton("Copy");
        copyBtn.addActionListener(e -> copyToClipboard(field.getText()));

        if (header instanceof String) panel.add(new JLabel((String)header), BorderLayout.NORTH);
        else if (header instanceof Component) panel.add((Component)header, BorderLayout.NORTH);

        panel.add(field, BorderLayout.CENTER);
        panel.add(copyBtn, BorderLayout.EAST);
        return panel;
    }

    private JPanel createMultiButtonComponent(String label, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(5, 2));
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 2, 0));
        JButton lowerBtn = new JButton("LOWER");
        JButton upperBtn = new JButton("UPPER");

        lowerBtn.addActionListener(e -> copyToClipboard(field.getText().toLowerCase()));
        upperBtn.addActionListener(e -> copyToClipboard(field.getText().toUpperCase()));

        btnPanel.add(lowerBtn);
        btnPanel.add(upperBtn);
        panel.add(btnPanel, BorderLayout.EAST);
        return panel;
    }

    private void updateFields() {
        String input = inputArea.getText();
        if (input.trim().isEmpty()) {
            itemCountLabel.setText("Items: 0");
            clearFields();
            return;
        }

        String[] lines = input.split("\\n");
        List<String[]> tokenizedLines = Arrays.stream(lines)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::tokenize)
                .collect(Collectors.toList());

        itemCountLabel.setText("Items: " + tokenizedLines.size());

        String joiner = commaDelimitResults.isSelected() ? ", " : " ";

        camelField.setText(tokenizedLines.stream().map(this::toCamelCase).collect(Collectors.joining(joiner)));
        pascalField.setText(tokenizedLines.stream().map(this::toPascalCase).collect(Collectors.joining(joiner)));
        snakeField.setText(tokenizedLines.stream().map(w -> String.join("_", w)).collect(Collectors.joining(joiner)));
        kebabField.setText(tokenizedLines.stream().map(w -> String.join("-", w)).collect(Collectors.joining(joiner)));
        plainField.setText(tokenizedLines.stream().map(this::toPlainCase).collect(Collectors.joining(joiner)));
    }

    private String[] tokenize(String input) {
        String sourcePref = (String) sourceFormatCombo.getSelectedItem();
        String[] words;
        if (sourcePref.contains("Camel")) {
            words = input.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])");
        } else if (sourcePref.contains("Strict Space")) {
            words = input.split("\\s+");
        } else {
            words = input.split("[\\s+_-]+");
        }
        return Arrays.stream(words).map(String::toLowerCase).filter(s -> !s.isEmpty()).toArray(String[]::new);
    }

    private String toCamelCase(String[] words) {
        if (words.length == 0) return "";
        StringBuilder sb = new StringBuilder(words[0]);
        for (int i = 1; i < words.length; i++) sb.append(capitalize(words[i]));
        return sb.toString();
    }

    private String toPascalCase(String[] words) {
        return Arrays.stream(words).map(this::capitalize).collect(Collectors.joining());
    }

    private String toPlainCase(String[] words) {
        String joined = String.join(" ", words);
        if (capitalizePlainCheckbox.isSelected() && !joined.isEmpty()) {
            return joined.substring(0, 1).toUpperCase() + joined.substring(1);
        }
        return joined;
    }

    private String capitalize(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private void clearFields() {
        camelField.setText(""); pascalField.setText("");
        snakeField.setText(""); kebabField.setText(""); plainField.setText("");
    }

    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    private class SimpleListener implements DocumentListener {
        public void insertUpdate(DocumentEvent e) { updateFields(); }
        public void removeUpdate(DocumentEvent e) { updateFields(); }
        public void changedUpdate(DocumentEvent e) { updateFields(); }
    }

}
