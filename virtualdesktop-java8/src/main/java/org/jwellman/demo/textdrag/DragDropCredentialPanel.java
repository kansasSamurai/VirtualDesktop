package org.jwellman.demo.textdrag;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

/**
 * A robust Swing utility panel allowing users to drag masked credentials/text 
 * directly into native desktop applications (e.g., browser password fields).
 * * Architecture:
 * - CENTER: Scrollable list of draggable targets.
 * - SOUTH: Utility panel for generating and copying new salted hashes.
 */
@SuppressWarnings("serial")
public class DragDropCredentialPanel extends JPanel {

    private final JPanel listContainer;
    private JTextField saltInputField;
    private JPasswordField rawTextInputField;
    private JTextField hashOutputField;

    public DragDropCredentialPanel() {
        super(new BorderLayout(0, 10));

        setBorder(new EmptyBorder(10, 10, 10, 10));

        // 1. Upper Half: Draggable Components inside a JScrollPane
        this.listContainer = new JPanel();
        this.listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        
        JScrollPane scrollPane = new JScrollPane(listContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);
        
        add(scrollPane, BorderLayout.CENTER);

        // 2. Bottom Half: Generator Utility Panel
        add(createGeneratorPanel(), BorderLayout.SOUTH);

        // 3. Initialize/Load Data
        loadDraggableItems();
    }

    /**
     * Stubs out data retrieval. In production, this wires into your 
     * external framework utilities to fetch stored salt/hash pairs.
     */
    private void loadDraggableItems() {
        // Framework integration point: Fetch external records here
        List<CredentialStub> externalRecords = fetchExternalCredentials();

        for (CredentialStub record : externalRecords) {
            listContainer.add(createDragComponent(record.getDescription(), record.getSalt(), record.getStoredHash()));
            listContainer.add(Box.createVerticalStrut(6)); // Predictable spacing
        }
        listContainer.revalidate();
        listContainer.repaint();
    }

    /**
     * Creates an individual UI component representing a draggable item.
     * Uses an explicit DragMouseAdapter to govern the drag threshold.
     */
    private JComponent createDragComponent(String description, String salt, String hash) {
        JPanel itemPanel = new JPanel(new BorderLayout(10, 0));
        itemPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                new EmptyBorder(8, 10, 8, 10)
        ));
        itemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        JLabel descLabel = new JLabel(description);
        descLabel.setFont(descLabel.getFont().deriveFont(Font.BOLD));
        itemPanel.add(descLabel, BorderLayout.CENTER);

        // Visual handle indicating drag capability
        JLabel dragHandle = new JLabel("⋮⋮ Drag");
        dragHandle.setForeground(Color.GRAY);
        dragHandle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        itemPanel.add(dragHandle, BorderLayout.WEST);

        // Configure Drag and Drop TransferHandler
        itemPanel.setTransferHandler(new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return COPY;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                // Resolution happens dynamically at the moment of the drop operation
                String resolvedValue = resolvePayload(salt, hash);
                return new StringSelection(resolvedValue);
            }
        });

        // Attach mouse listener to initiate drag safely based on standard gestures
        DragMouseAdapter mouseAdapter = new DragMouseAdapter(itemPanel);
        itemPanel.addMouseListener(mouseAdapter);
        itemPanel.addMouseMotionListener(mouseAdapter);

        return itemPanel;
    }

    /**
     * Creates the bottom generation utility area using GridBagLayout for alignment durability.
     */
    private JPanel createGeneratorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Hash Generator Utility"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Salt Input
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        panel.add(new JLabel("Salt:"), gbc);
        
        saltInputField = new JTextField(15);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(saltInputField, gbc);

        // Row 1: Plaintext Input
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        panel.add(new JLabel("Plain Text:"), gbc);

        rawTextInputField = new JPasswordField(15);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(rawTextInputField, gbc);

        // Row 2: Actions
        JButton generateBtn = new JButton("Generate & Copy");
        gbc.gridx = 1; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(generateBtn, gbc);

        // Row 3: Output Display
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Generated Hash:"), gbc);

        hashOutputField = new JTextField();
        hashOutputField.setEditable(false);
        hashOutputField.setBackground(UIManager.getColor("Panel.background"));
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(hashOutputField, gbc);

        // Event Handling
        generateBtn.addActionListener(e -> executeGenerationPipeline());

        return panel;
    }

    /**
     * Orchestrates the generation, UI display, and clipboard injection of a new hash.
     */
    private void executeGenerationPipeline() {
        String salt = saltInputField.getText().trim();
        char[] passwordChars = rawTextInputField.getPassword();
        String plainText = new String(passwordChars);

        if (salt.isEmpty() || plainText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Both Salt and Plain Text fields are required.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String generatedHash = calculateCryptoHash(plainText, salt);
        hashOutputField.setText(generatedHash);

        // Instantly move to system clipboard
        StringSelection selection = new StringSelection(generatedHash);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);

        // Clear out raw memory reference quickly
        java.util.Arrays.fill(passwordChars, ' ');
        rawTextInputField.setText("");
    }

    /**
     * Deterministic hashing algorithm (SHA-256) acting as the underlying security mechanism.
     * Change or swap out depending on requirements.
     */
    private String calculateCryptoHash(String input, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Combine token and salt via explicit UTF-8 byte boundaries
            String combined = input + salt;
            byte[] encodedHash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            
            // Return clean alphanumeric string representation
            return Base64.getEncoder().encodeToString(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Critical Failure: SHA-256 Algorithm implementation missing from JVM.", e);
        }
    }

    /**
     * Resolves the real drag-and-drop payload at drop execution time.
     * In your architecture, if the hash *is* the actual password, this simply returns the hash.
     * If the hash must be cracked or looked up via a local vault, adapt this method.
     */
    private String resolvePayload(String salt, String hash) {
        // As specified, the app stores/utilizes the hash to avoid plain text on disk.
        // Returning the raw hash to be dropped directly into the password field.
        return hash;
    }

    /**
     * Stubs out data persistence. Replace with your structural framework database/file reads.
     */
    private List<CredentialStub> fetchExternalCredentials() {
        List<CredentialStub> list = new ArrayList<>();
        // Mocked Examples:
        list.add(new CredentialStub("Internal App Database Admin", "salt_alpha_99", "k8FmX/qW0P9oLkR1vA5zNn8M..."));
        list.add(new CredentialStub("Staging Environment API Key", "salt_beta_local", "mQ4vP9LzT7xW1bC2vR9sKz0X..."));
        return list;
    }

    /**
     * Explicit mouse adapter that monitors drag thresholds before triggering 
     * the TransferHandler, preventing jerky accidental drags during basic clicks.
     */
    private static class DragMouseAdapter extends MouseAdapter {
        private final JComponent component;
        private MouseEvent firstMouseEvent = null;

        public DragMouseAdapter(JComponent component) {
            this.component = component;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            firstMouseEvent = e;
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            firstMouseEvent = null;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (firstMouseEvent != null) {
                long dx = Math.abs(e.getX() - firstMouseEvent.getX());
                long dy = Math.abs(e.getY() - firstMouseEvent.getY());
                
                // Verify movement extends beyond the native platform's drag threshold
                if (dx > 5 || dy > 5) {
                    TransferHandler handler = component.getTransferHandler();
                    handler.exportAsDrag(component, firstMouseEvent, TransferHandler.COPY);
                    firstMouseEvent = null; // Clear to prevent multiple concurrent triggers
                }
            }
        }
    }

    /**
     * Simple domain object stub for managing local references.
     */
    private static class CredentialStub {
        private final String description;
        private final String salt;
        private final String storedHash;

        public CredentialStub(String description, String salt, String storedHash) {
            this.description = description;
            this.salt = salt;
            this.storedHash = storedHash;
        }

        public String getDescription() { return description; }
        public String getSalt() { return salt; }
        public String getStoredHash() { return storedHash; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Drag Drop Text");
            frame.add(new DragDropCredentialPanel(), BorderLayout.CENTER);

            frame.setSize(350, 450);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }

}
