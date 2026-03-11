package org.jwellman.demo.textutility;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class TextUtilityApp extends JFrame {

    public TextUtilityApp() {
        setTitle("SQL & Text Formatter Utility");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
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
    private JTextField commaField;
    private JTextField sqlField;
    private JTextField customResultField;
    
    // Custom Delimiter Inputs
    private JTextField leftDelimiter;
    private JTextField rightDelimiter;

    public TextTransformerPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Left Side: Input ---
        inputArea = new JTextArea();
        inputArea.getDocument().addDocumentListener(new SimpleListener());
        
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel("Paste Line-Delimited Text:"), BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        leftPanel.setPreferredSize(new Dimension(350, 0));

        // --- Right Side: Results ---
        JPanel rightPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 5, 15, 5);

        // Component 1: Comma Delimited
        commaField = createReadOnlyField();
        rightPanel.add(createResultComponent(new JLabel("Comma Delimited"), commaField), gbc);

        // Component 2: SQL Formatted
        sqlField = createReadOnlyField();
        rightPanel.add(createResultComponent(new JLabel("SQL Quoted (Single Apostrophe)"), sqlField), gbc);

        // Component 3: Custom Wrapper
        customResultField = createReadOnlyField();
        rightPanel.add(createCustomWrapperComponent(), gbc);

        // Spacer to push components to the top
        gbc.weighty = 1.0;
        rightPanel.add(new JPanel(), gbc);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
    }

    private JTextField createReadOnlyField() {
        JTextField field = new JTextField();
        field.setEditable(false);
        field.setBackground(new Color(245, 245, 245));
        return field;
    }

    private JPanel createResultComponent(Component labelOrHeader, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(5, 2));
        JButton copyBtn = new JButton("Copy");
        copyBtn.setMargin(new Insets(2, 5, 2, 5));
        copyBtn.addActionListener(e -> copyToClipboard(field.getText()));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.add(labelOrHeader, BorderLayout.WEST);
        topRow.add(copyBtn, BorderLayout.EAST);

        panel.add(topRow, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCustomWrapperComponent() {
        // Create the "Header" with the two input boxes
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        
        leftDelimiter = new JTextField("(", 3);
        rightDelimiter = new JTextField(")", 3);
        
        // Add listeners so the result updates when delimiters change
        leftDelimiter.getDocument().addDocumentListener(new SimpleListener());
        rightDelimiter.getDocument().addDocumentListener(new SimpleListener());

        headerPanel.add(new JLabel("Left:"));
        headerPanel.add(leftDelimiter);
        headerPanel.add(Box.createHorizontalStrut(10));
        headerPanel.add(new JLabel("Right:"));
        headerPanel.add(rightDelimiter);

        return createResultComponent(headerPanel, customResultField);
    }

    private void updateFields() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            commaField.setText("");
            sqlField.setText("");
            customResultField.setText("");
            return;
        }

        List<String> cleanedLines = Arrays.stream(input.split("\\r?\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        // Standard Comma
        commaField.setText(String.join(", ", cleanedLines));

        // SQL (Apostrophes)
        sqlField.setText(cleanedLines.stream()
                .map(s -> "'" + s + "'")
                .collect(Collectors.joining(", ")));

        // Custom Wrapper
        String left = leftDelimiter.getText();
        String right = rightDelimiter.getText();
        customResultField.setText(cleanedLines.stream()
                .map(s -> left + s + right)
                .collect(Collectors.joining(", ")));
    }

    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(text), null);
    }

    private class SimpleListener implements DocumentListener {
        public void insertUpdate(DocumentEvent e) { updateFields(); }
        public void removeUpdate(DocumentEvent e) { updateFields(); }
        public void changedUpdate(DocumentEvent e) { updateFields(); }
    }

}
