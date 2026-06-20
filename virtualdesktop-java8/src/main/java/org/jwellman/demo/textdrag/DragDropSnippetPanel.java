package org.jwellman.demo.textdrag;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Dialog;
import java.awt.FlowLayout;
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
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
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
 * A Swing utility panel for dragging labeled text snippets into other
 * applications. Each snippet's value is stored encoded and decoded at drop time.
 *
 * Architecture:
 * - CENTER: Scrollable list of draggable snippet targets.
 * - SOUTH:  Add-snippet bar (description field + button that opens encoder dialog).
 */
@SuppressWarnings("serial")
public class DragDropSnippetPanel extends JPanel {

    private JPanel listContainer;
    private JTextField descriptionInputField;

    private String configuredSeed = null;
    private List<String> configuredDescriptions = null;
    private List<String> configuredValues = null;

    public DragDropSnippetPanel() {
        super(new BorderLayout(0, 10));
        initUI();
    }

    public DragDropSnippetPanel(String seed, List<String> descriptions, List<String> values) {
        super(new BorderLayout(0, 10));
        this.configuredSeed = seed;
        this.configuredDescriptions = descriptions;
        this.configuredValues = values;
        initUI();
    }

    private void initUI() {
        setBorder(new EmptyBorder(10, 10, 10, 10));

        this.listContainer = new JPanel();
        this.listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(listContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);

        add(scrollPane, BorderLayout.CENTER);
        add(createAddSnippetBar(), BorderLayout.SOUTH);

        loadSnippets();
    }

    /**
     * Stubs out data retrieval. In production, this wires into your
     * external framework utilities to fetch stored snippet records.
     */
    private void loadSnippets() {
        List<SnippetRecord> records = fetchSnippets();

        for (SnippetRecord record : records) {
            listContainer.add(createDragComponent(record.getDescription(), record.getSeed(), record.getEncodedValue()));
            listContainer.add(Box.createVerticalStrut(6));
        }
        listContainer.revalidate();
        listContainer.repaint();
    }

    /**
     * Slim bar at the bottom of the panel. If a description is entered before
     * clicking the button, a new drag item is appended to the list on completion.
     */
    private JPanel createAddSnippetBar() {
        JPanel bar = new JPanel(new BorderLayout(6, 0));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                new EmptyBorder(6, 0, 0, 0)));

        bar.add(new JLabel("Description:"), BorderLayout.WEST);

        descriptionInputField = new JTextField();
        bar.add(descriptionInputField, BorderLayout.CENTER);

        JButton addBtn = new JButton("Add Snippet...");
        addBtn.addActionListener(e -> onAddSnippet());
        bar.add(addBtn, BorderLayout.EAST);

        return bar;
    }

    /**
     * Opens the encoder dialog. If the user completes encoding and a description
     * is present, appends a new drag component to the live list.
     */
    private void onAddSnippet() {
        SnippetEncoderDialog dialog = new SnippetEncoderDialog();
        dialog.setVisible(true); // blocks — modal

        String encodedValue = dialog.getEncodedValue();
        if (encodedValue == null) {
            return; // cancelled or nothing encoded
        }

        String description = descriptionInputField.getText().trim();
        if (!description.isEmpty()) {
            listContainer.add(createDragComponent(description, dialog.getSeed(), encodedValue));
            listContainer.add(Box.createVerticalStrut(6));
            listContainer.revalidate();
            listContainer.repaint();
            descriptionInputField.setText("");
        }
    }

    /**
     * Creates an individual UI component representing a draggable snippet.
     * Uses an explicit DragMouseAdapter to govern the drag threshold.
     */
    private JComponent createDragComponent(String description, String seed, String encodedValue) {
        JPanel itemPanel = new JPanel(new BorderLayout(10, 0));
        itemPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                new EmptyBorder(8, 10, 8, 10)
        ));
        itemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        JLabel descLabel = new JLabel(description);
        descLabel.setFont(descLabel.getFont().deriveFont(Font.BOLD));
        itemPanel.add(descLabel, BorderLayout.CENTER);

        JLabel dragHandle = new JLabel("⋮⋮ Drag");
        dragHandle.setForeground(Color.GRAY);
        dragHandle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        itemPanel.add(dragHandle, BorderLayout.WEST);

        itemPanel.setTransferHandler(new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return COPY;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                return new StringSelection(decodeValue(encodedValue, seed));
            }
        });

        DragMouseAdapter mouseAdapter = new DragMouseAdapter(itemPanel);
        itemPanel.addMouseListener(mouseAdapter);
        itemPanel.addMouseMotionListener(mouseAdapter);

        return itemPanel;
    }

    // -------------------------------------------------------------------------
    // Crypto helpers — shared by drag resolution and the encoder dialog
    // -------------------------------------------------------------------------

    private String encodeValue(String plainText, String seed) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(seed));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encoding failed", e);
        }
    }

    private String decodeValue(String encodedValue, String seed) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(seed));
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encodedValue));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decoding failed", e);
        }
    }

    private SecretKeySpec deriveKey(String seed) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(seed.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, "AES");
    }

    // -------------------------------------------------------------------------
    // Data loading
    // -------------------------------------------------------------------------

    private List<SnippetRecord> fetchSnippets() {
        if (configuredDescriptions != null && configuredValues != null) {
            List<SnippetRecord> list = new ArrayList<>();
            String seed = configuredSeed != null ? configuredSeed : "";
            int count = Math.min(configuredDescriptions.size(), configuredValues.size());
            for (int i = 0; i < count; i++) {
                list.add(new SnippetRecord(configuredDescriptions.get(i), seed, configuredValues.get(i)));
            }
            return list;
        }
        List<SnippetRecord> list = new ArrayList<>();
        list.add(new SnippetRecord("Internal App Database Admin", "seed_alpha_99", "k8FmX/qW0P9oLkR1vA5zNn8M..."));
        list.add(new SnippetRecord("Staging Environment API Key", "seed_beta_local", "mQ4vP9LzT7xW1bC2vR9sKz0X..."));
        return list;
    }

    // -------------------------------------------------------------------------
    // Inner class: encoder dialog
    // -------------------------------------------------------------------------

    /**
     * Modal dialog containing the transform utility. Seed is pre-filled from
     * the panel's configuredSeed. Call getEncodedValue() after dispose() — a
     * null return means the user cancelled or closed without encoding.
     */
    private class SnippetEncoderDialog extends JDialog {

        private JTextField seedField;
        private JPasswordField plainTextField;
        private JTextField encodedOutputField;

        private String resultEncodedValue = null;
        private String resultSeed = null;

        SnippetEncoderDialog() {
            super(SwingUtilities.getWindowAncestor(DragDropSnippetPanel.this),
                    "Add Snippet", Dialog.ModalityType.APPLICATION_MODAL);
            initDialogUI();
            pack();
            setResizable(false);
            setLocationRelativeTo(DragDropSnippetPanel.this);
        }

        private void initDialogUI() {
            JPanel content = new JPanel(new GridBagLayout());
            content.setBorder(new EmptyBorder(10, 10, 6, 10));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 6, 4, 6);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            // Row 0: Seed
            gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
            content.add(new JLabel("Seed:"), gbc);

            seedField = new JTextField(20);
            if (configuredSeed != null) {
                seedField.setText(configuredSeed);
            }
            gbc.gridx = 1; gbc.weightx = 1.0;
            content.add(seedField, gbc);

            // Row 1: Plain text
            gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
            content.add(new JLabel("Plain Text:"), gbc);

            plainTextField = new JPasswordField(20);
            gbc.gridx = 1; gbc.weightx = 1.0;
            content.add(plainTextField, gbc);

            // Row 2: Encode button
            JButton encodeBtn = new JButton("Encode & Copy");
            gbc.gridx = 1; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.EAST;
            content.add(encodeBtn, gbc);

            // Row 3: Encoded output
            gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.WEST;
            content.add(new JLabel("Encoded Value:"), gbc);

            encodedOutputField = new JTextField(20);
            encodedOutputField.setEditable(false);
            encodedOutputField.setBackground(UIManager.getColor("Panel.background"));
            gbc.gridx = 1; gbc.weightx = 1.0;
            content.add(encodedOutputField, gbc);

            // Button row
            JButton okBtn = new JButton("OK");
            JButton cancelBtn = new JButton("Cancel");
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
            buttons.add(cancelBtn);
            buttons.add(okBtn);

            encodeBtn.addActionListener(e -> onEncode());
            okBtn.addActionListener(e -> onOk());
            cancelBtn.addActionListener(e -> dispose());

            getRootPane().setDefaultButton(okBtn);

            setLayout(new BorderLayout());
            add(content, BorderLayout.CENTER);
            add(buttons, BorderLayout.SOUTH);
        }

        private void onEncode() {
            String seed = seedField.getText().trim();
            char[] chars = plainTextField.getPassword();
            String plainText = new String(chars);

            if (seed.isEmpty() || plainText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Both Seed and Plain Text are required.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String encoded = encodeValue(plainText, seed); // outer class method
            encodedOutputField.setText(encoded);

            StringSelection selection = new StringSelection(encoded);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);

            java.util.Arrays.fill(chars, ' ');
            plainTextField.setText("");
        }

        private void onOk() {
            String encoded = encodedOutputField.getText().trim();
            if (encoded.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Encode a value first.", "Nothing Encoded", JOptionPane.WARNING_MESSAGE);
                return;
            }
            resultEncodedValue = encoded;
            resultSeed = seedField.getText().trim();
            dispose();
        }

        /** Returns the encoded value, or null if the dialog was cancelled. */
        String getEncodedValue() {
            return resultEncodedValue;
        }

        /** Returns the seed that was used to produce the encoded value. */
        String getSeed() {
            return resultSeed;
        }
    }

    // -------------------------------------------------------------------------
    // Inner class: drag threshold adapter
    // -------------------------------------------------------------------------

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
                if (dx > 5 || dy > 5) {
                    TransferHandler handler = component.getTransferHandler();
                    handler.exportAsDrag(component, firstMouseEvent, TransferHandler.COPY);
                    firstMouseEvent = null;
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Inner class: snippet data record
    // -------------------------------------------------------------------------

    /**
     * Simple domain object for managing snippet records.
     */
    private static class SnippetRecord {
        private final String description;
        private final String seed;
        private final String encodedValue;

        public SnippetRecord(String description, String seed, String encodedValue) {
            this.description = description;
            this.seed = seed;
            this.encodedValue = encodedValue;
        }

        public String getDescription() { return description; }
        public String getSeed() { return seed; }
        public String getEncodedValue() { return encodedValue; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Drag Drop Snippets");
            frame.add(new DragDropSnippetPanel(), BorderLayout.CENTER);
            frame.setSize(350, 450);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }

}
