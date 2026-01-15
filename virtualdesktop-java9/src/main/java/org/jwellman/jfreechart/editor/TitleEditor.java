package org.jwellman.jfreechart.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Paint;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.JFreeChart;
import org.jwellman.swing.colorchooser.VPaletteChooser;

/**
 * 
 * @author rwellman
 *
 */
public class TitleEditor extends BaseEditor {

    public /**/ JFreeChart jfreechart;
    public /**/ JLabel statusLabel = new JLabel();
    public /**/ JSlider fontsize = new JSlider(10, 100);
    public /**/ JButton saveButton = new JButton("Save Changes");
    public /**/ JButton resetButton = new JButton("Reset");
    public /**/ JTextField txtTitle = new JTextField();
    public /**/ JTextField txtTitleFont = new JTextField();
    public /**/ VPaletteChooser pnlPaletteChooser = new VPaletteChooser(2);

    protected static class STATUS {
        public static final String READY = "Ready to edit chart properties";
        public static final String SAVED = "Chart properties saved";
        public static final String RESET = "Chart properties reset";
    }

    private static final long serialVersionUID = 1L;

    public TitleEditor() {
        this.setLayout(new BorderLayout());
        this.setBorder(BORDERS.PANEL);

        // Set different background colors for different node types
        Color backgroundColor = this.getBackground();

        // Create title
        JLabel title = new JLabel("Edit Title", SwingConstants.CENTER);
        title.setFont(FONTS.TITLE);
        title.setBorder(BORDERS.TITLE);

        // Editor Panel
        JPanel editorPanel = this.createEditorPanel();

        // Status panel
        statusLabel.setText(STATUS.READY);
        statusLabel.setFont(FONTS.STATUS);
        statusLabel.setForeground(Color.GRAY);

        Box statusPanel = Box.createHorizontalBox();
        statusPanel.setBackground(backgroundColor);
        statusPanel.add(statusLabel);
        statusPanel.add(Box.createHorizontalGlue());
        statusPanel.add(saveButton);
        statusPanel.add(resetButton);

        this.add(title, BorderLayout.NORTH);
        this.add(editorPanel, BorderLayout.CENTER);
        this.add(statusPanel, BorderLayout.SOUTH);
    }

    private JPanel createEditorPanel() {
        JPanel container = new JPanel(new BorderLayout());

        // Set different background colors for different node types
        Color backgroundColor = this.getBackground();

        // Lightweight [reusable] constraint object used for panel layout
        final GridBagConstraints gbc = new GridBagConstraints();
        int row = 0; // use a variable so we don't have to code row by hand every time the page changes

        // Create editor form
        JPanel editorPanel = new JPanel(new GridBagLayout());
        editorPanel.setBackground(backgroundColor);
        gbc.insets = new Insets(2, 10, 2, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Title field
        gbc.gridx = 0; gbc.gridy = row;
        editorPanel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        editorPanel.add(txtTitle, gbc);

        // Title, Font Name
        row++; // new row so increment
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        editorPanel.add(createLabel("Title Font:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        editorPanel.add(txtTitleFont, gbc);

        // Title, Font Size
        row++; // new row so increment
        JLabel lblTitleSize = createLabel("Title Size (pt):");
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        editorPanel.add(lblTitleSize, gbc);

        fontsize.setMajorTickSpacing(10);
        fontsize.setMinorTickSpacing(2);
        fontsize.setPaintTicks(true);
        fontsize.setSnapToTicks(true);
        fontsize.setPaintLabels(true);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; // gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        editorPanel.add(fontsize, gbc);

        // Title, Font Color
        row++; // new row so increment
        JLabel lblTitleColor = createLabel("Title Color:");
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        editorPanel.add(lblTitleColor, gbc);

        // not a portable hack but since I'm using a dark theme, set background to match
        pnlPaletteChooser.pnlButtons.setBackground(this.getBackground());
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; // gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        editorPanel.add(pnlPaletteChooser, gbc);

        // Buttons panel
//        row++; // new row so increment
//
//        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE; 
//        gbc.weightx = 0; gbc.weighty = 0; gbc.anchor = GridBagConstraints.CENTER;
//        JPanel buttonPanel = new JPanel(new FlowLayout());
//        buttonPanel.setBackground(backgroundColor);
//        buttonPanel.add(saveButton);
//        // buttonPanel.add(resetButton);
//
//        editorPanel.add(buttonPanel, gbc);

        // Add action listeners
        saveButton.addActionListener(e -> {
            this.updateChart();
        });

        resetButton.addActionListener(e -> {
            txtTitle.setText(jfreechart.getTitle().getText());
            txtTitleFont.setText(jfreechart.getTitle().getFont().getFontName());
            fontsize.setValue(jfreechart.getTitle().getFont().getSize());

            Paint paintObject = jfreechart.getTitle().getPaint();
            if (paintObject != null) {
                if (paintObject instanceof Color) {
                    pnlPaletteChooser.setSelectedColor((Color) paintObject);
                }
            }

            statusLabel.setText(STATUS.RESET);
            this.showResetMessage();
            statusLabel.setText(STATUS.READY);
        });

        container.add(editorPanel, BorderLayout.NORTH);
        return container;
    }

    /**
     * Action taken when "Save Changes" is clicked.
     */
    public void updateChart() {
        jfreechart.setTitle(txtTitle.getText());

        Font c = jfreechart.getTitle().getFont();
        String name = StringUtils.isEmpty(txtTitleFont.getText())
            ?  c.getFontName() : txtTitleFont.getText() ;
        Font f = new Font(name, c.getStyle(), fontsize.getValue());
        jfreechart.getTitle().setFont(f);

        Color pc = pnlPaletteChooser.getSelectedColor();
        if (pc != null)
            jfreechart.getTitle().setPaint(pc);

        statusLabel.setText(STATUS.SAVED);
        this.showSavedMessage();
        statusLabel.setText(STATUS.READY);
    }

    public JFreeChart getJfreechart() {
        return jfreechart;
    }

    public void setJfreechart(JFreeChart jfreechart) {
        this.jfreechart = jfreechart;
    }

}
