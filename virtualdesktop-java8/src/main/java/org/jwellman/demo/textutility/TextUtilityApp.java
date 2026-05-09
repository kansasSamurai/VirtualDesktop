package org.jwellman.demo.textutility;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <h1>DevData Transformer Pro</h1>
 * A high-efficiency Java Swing utility designed for developers to transform 
 * line-delimited or comma-separated raw text into formatted strings for SQL, 
 * code, or data entry.
 * <p>
 * This utility provides real-time reactive updates as data is entered or 
 * settings are toggled, minimizing manual string manipulation.
 * </p>
 * * <h3>Key Features:</h3>
 * <ul>
 * <li><b>Smart Split:</b> Intelligently parses input by detecting both newlines 
 * and existing commas to normalize fragmented data.</li>
 * <li><b>SQL Formatter:</b> Automatically wraps each item in single quotes 
 * and joins them with commas for immediate use in {@code WHERE IN (...)} clauses.</li>
 * <li><b>Universal Wrapper:</b> Allows custom "Left" and "Right" delimiters 
 * (e.g., brackets, parentheses, or tags) for specialized formatting.</li>
 * <li><b>Unique Only:</b> A de-duplication toggle that filters out redundant 
 * entries from the result set.</li>
 * <li><b>Case Transformation:</b> Normalizes data to <i>UPPERCASE</i>, 
 * <i>lowercase</i>, or maintains the <i>Original Case</i>.</li>
 * <li><b>Live Item Counter:</b> Displays a real-time count of processed items, 
 * serving as a sanity check for large data transfers.</li>
 * <li><b>One-Click Copy:</b> Individual clipboard buttons for each result 
 * component for rapid workflow integration.</li>
 * <li><b>Clear All:</b> Instantly resets the workspace for the next batch of data.</li>
 * </ul>
 *
 * @author Rick Wellman
 * @author Gemini
 * @version 1.2
 */
@SuppressWarnings("serial")
public class TextUtilityApp extends JFrame {

    public TextUtilityApp() {
        setTitle("DevData Transformer Pro");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);
        add(new TextTransformerPanel());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TextUtilityApp().setVisible(true));
    }
}

@SuppressWarnings("serial")
class TextTransformerPanel extends JPanel {

    private JTextArea inputArea;
    private JLabel itemCountLabel;
    private JTextField commaField, sqlField, customResultField;
    private JTextField leftDelimiter, rightDelimiter;
    
    // Toggles
    private JCheckBox uniqueCheckbox;
    private JComboBox<String> caseCombo;
    private JCheckBox smartSplitCheckbox;

    public TextTransformerPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Left Side: Input & Controls ---
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setPreferredSize(new Dimension(400, 0));

        inputArea = new JTextArea();
        inputArea.getDocument().addDocumentListener(new SimpleListener());
        
        // Input Toolbar (Top of Left)
        JPanel inputToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        smartSplitCheckbox = new JCheckBox("Smart Split (Lines or Commas)", true);
        smartSplitCheckbox.addActionListener(e -> updateFields());
        inputToolbar.add(smartSplitCheckbox);

        // Footer for Left Side
        JPanel inputFooter = new JPanel(new BorderLayout());
        itemCountLabel = new JLabel("Items: 0");
        JButton clearBtn = new JButton("Clear All");
        clearBtn.addActionListener(e -> {
            inputArea.setText("");
            updateFields();
        });
        inputFooter.add(itemCountLabel, BorderLayout.WEST);
        inputFooter.add(clearBtn, BorderLayout.EAST);

        leftPanel.add(new JLabel("Input Text:"), BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        leftPanel.add(inputFooter, BorderLayout.SOUTH);
        leftPanel.add(inputToolbar, BorderLayout.NORTH);

        // --- Right Side: Results & Global Toggles ---
        JPanel rightPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 5, 10, 5);

        // Global Settings Row
        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        uniqueCheckbox = new JCheckBox("Unique Only");
        uniqueCheckbox.addActionListener(e -> updateFields());
        
        caseCombo = new JComboBox<>(new String[]{"Original Case", "UPPERCASE", "lowercase"});
        caseCombo.addActionListener(e -> updateFields());

        settingsPanel.add(uniqueCheckbox);
        settingsPanel.add(new JLabel("Case:"));
        settingsPanel.add(caseCombo);
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Transformation Settings"));
        
        rightPanel.add(settingsPanel, gbc);

        // Result Components
        commaField = createReadOnlyField();
        rightPanel.add(createResultComponent(new JLabel("Standard Comma List"), commaField), gbc);

        sqlField = createReadOnlyField();
        rightPanel.add(createResultComponent(new JLabel("SQL In-Clause (Quoted)"), sqlField), gbc);

        customResultField = createReadOnlyField();
        rightPanel.add(createCustomWrapperComponent(), gbc);

        // Spacer
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

    private JPanel createResultComponent(Component header, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(5, 2));
        JButton copyBtn = new JButton("Copy");
        copyBtn.addActionListener(e -> copyToClipboard(field.getText()));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.add(header, BorderLayout.WEST);
        topRow.add(copyBtn, BorderLayout.EAST);

        panel.add(topRow, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCustomWrapperComponent() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        leftDelimiter = new JTextField("(", 3);
        rightDelimiter = new JTextField(")", 3);
        
        leftDelimiter.getDocument().addDocumentListener(new SimpleListener());
        rightDelimiter.getDocument().addDocumentListener(new SimpleListener());

        header.add(new JLabel("Custom Left:"));
        header.add(leftDelimiter);
        header.add(Box.createHorizontalStrut(10));
        header.add(new JLabel("Right:"));
        header.add(rightDelimiter);

        return createResultComponent(header, customResultField);
    }

    private void updateFields() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            itemCountLabel.setText("Items: 0");
            commaField.setText("");
            sqlField.setText("");
            customResultField.setText("");
            return;
        }

        // Logic for splitting: Newlines, and optionally commas
        String regex = smartSplitCheckbox.isSelected() ? "[,\\n\\r]+" : "[\\n\\r]+";
        Stream<String> stream = Arrays.stream(input.split(regex))
                .map(String::trim)
                .filter(s -> !s.isEmpty());

        // Apply Unique Filter
        if (uniqueCheckbox.isSelected()) {
            stream = stream.distinct();
        }

        // Apply Case Transformation
        String caseType = (String) caseCombo.getSelectedItem();
        stream = stream.map(s -> {
            if ("UPPERCASE".equals(caseType)) return s.toUpperCase();
            if ("lowercase".equals(caseType)) return s.toLowerCase();
            return s;
        });

        List<String> results = stream.collect(Collectors.toList());
        itemCountLabel.setText("Items: " + results.size());

        // Update Fields
        commaField.setText(String.join(", ", results));
        sqlField.setText(results.stream().map(s -> "'" + s + "'").collect(Collectors.joining(", ")));
        
        String L = leftDelimiter.getText();
        String R = rightDelimiter.getText();
        customResultField.setText(results.stream().map(s -> L + s + R).collect(Collectors.joining(", ")));
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