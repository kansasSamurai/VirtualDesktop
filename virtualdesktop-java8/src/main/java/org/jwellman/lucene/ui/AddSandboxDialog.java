package org.jwellman.lucene.ui;

import org.jwellman.lucene.model.AnalyzerType;
import org.jwellman.lucene.model.DirectorySandboxConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Modal dialog for adding a new Lucene index sandbox.
 *
 * <p>Collects display name, source path (with Browse button), file inclusion
 * filter, and analyzer type. The sandbox ID is auto-derived from the display
 * name (lowercase, non-alphanumeric chars replaced with underscores).</p>
 *
 * <p>Call {@link #showDialog(Component)} — returns a configured
 * {@link DirectorySandboxConfig} if the user confirmed, or {@code null} if
 * they cancelled.</p>
 */
public class AddSandboxDialog extends JDialog {

    private final JTextField nameField   = new JTextField(30);
    private final JTextField sourceField = new JTextField(30);
    private final JTextField filterField = new JTextField("*.txt,*.md", 30);
    private final JComboBox<AnalyzerType> analyzerCombo = new JComboBox<AnalyzerType>(AnalyzerType.values());

    private DirectorySandboxConfig result = null;

    private AddSandboxDialog(Frame owner) {
        super(owner, "Add Index Sandbox", true);
        buildUI();
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Show the dialog and return the configured sandbox, or null on cancel.
     *
     * @param parent any component in the hierarchy (used to find the owner Frame)
     */
    public static DirectorySandboxConfig showDialog(Component parent) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(parent);
        AddSandboxDialog dialog = new AddSandboxDialog(owner);
        dialog.setVisible(true);
        return dialog.result;
    }

    private void buildUI() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));

        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(4, 0, 4, 8);
        lc.gridx  = 0;

        GridBagConstraints fc = new GridBagConstraints();
        fc.fill    = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1.0;
        fc.insets  = new Insets(4, 0, 4, 0);
        fc.gridx   = 1;

        // Display Name
        lc.gridy = 0; fc.gridy = 0;
        form.add(new JLabel("Display Name:"), lc);
        form.add(nameField, fc);

        // Source Path + Browse
        lc.gridy = 1; fc.gridy = 1;
        form.add(new JLabel("Source Path:"), lc);
        JPanel sourceRow = new JPanel(new BorderLayout(4, 0));
        sourceRow.add(sourceField, BorderLayout.CENTER);
        JButton browseBtn = new JButton("Browse...");
        browseBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onBrowse();
            }
        });
        sourceRow.add(browseBtn, BorderLayout.EAST);
        form.add(sourceRow, fc);

        // File Filter
        lc.gridy = 2; fc.gridy = 2;
        form.add(new JLabel("File Filter:"), lc);
        form.add(filterField, fc);

        // Analyzer
        lc.gridy = 3; fc.gridy = 3;
        form.add(new JLabel("Analyzer:"), lc);
        form.add(analyzerCombo, fc);

        // ID note
        GridBagConstraints nc = new GridBagConstraints();
        nc.gridx = 0; nc.gridy = 4; nc.gridwidth = 2;
        nc.anchor = GridBagConstraints.WEST;
        nc.insets = new Insets(6, 0, 0, 0);
        JLabel note = new JLabel("The sandbox ID is auto-derived from the display name.");
        note.setFont(note.getFont().deriveFont(Font.ITALIC, 10f));
        note.setForeground(Color.GRAY);
        form.add(note, nc);

        // Buttons
        JButton okBtn     = new JButton("Add");
        JButton cancelBtn = new JButton("Cancel");

        okBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onConfirm();
            }
        });
        cancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttonRow.setBorder(BorderFactory.createEmptyBorder(4, 10, 8, 10));
        buttonRow.add(okBtn);
        buttonRow.add(cancelBtn);

        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttonRow, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okBtn);
    }

    private void onBrowse() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Source Directory");
        String current = sourceField.getText().trim();
        if (!current.isEmpty()) {
            chooser.setCurrentDirectory(new File(current));
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            sourceField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void onConfirm() {
        String name = nameField.getText().trim();
        String src  = sourceField.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Display name is required.", "Validation",
                JOptionPane.WARNING_MESSAGE);
            nameField.requestFocusInWindow();
            return;
        }
        if (src.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Source path is required.", "Validation",
                JOptionPane.WARNING_MESSAGE);
            sourceField.requestFocusInWindow();
            return;
        }

        String id = deriveId(name);
        DirectorySandboxConfig cfg = new DirectorySandboxConfig();
        cfg.setId(id);
        cfg.setDisplayName(name);
        cfg.setSourcePath(src);
        cfg.setFileInclusionFilter(filterField.getText().trim());
        cfg.setAnalyzerType((AnalyzerType) analyzerCombo.getSelectedItem());

        result = cfg;
        dispose();
    }

    /**
     * Derives a filesystem-safe sandbox ID from the display name.
     * e.g. "Script Repository" → "script_repository"
     */
    static String deriveId(String displayName) {
        return displayName.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }
}
