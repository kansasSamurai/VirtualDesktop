package org.katacode.pipeline.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import org.katacode.pipeline.engine.PipelineStep;

/**
 * A custom-rendered Java Swing component that displays a PipelineStep card
 * matching our soft-palette, rounded, visual mockup style.
 */
@SuppressWarnings("serial")
public class PipelineCardPanel extends JPanel {

    private final PipelineStep step;
    private final int stepIndex;
    private final boolean isSelected;
    
    // Theme Colors based on our soft-palette mockup specs
    private Color bgLightColor;
    private Color bgDarkColor;
    private Color borderColor;
    private final Color textColor = new Color(0x22, 0x22, 0x22);
    private final Color subTextColor = new Color(0x55, 0x55, 0x55);

    public PipelineCardPanel(PipelineStep step, int stepIndex, boolean isSelected) {
        this.step = step;
        this.stepIndex = stepIndex;
        this.isSelected = isSelected;

        setOpaque(false); // Allows us to render our own rounded corners beautifully
        setBorder(new EmptyBorder(12, 16, 12, 16));
        setLayout(new BorderLayout(14, 0));
        
        assignPaletteColors();
        initUI();
    }

    /**
     * Maps component type strings directly to our soft-palette color states.
     */
    private void assignPaletteColors() {
        String type = step.getComponentName().toLowerCase();
        
        if (type.contains("reader") || type.contains("source")) {
            bgLightColor = new Color(0xD6, 0xE4, 0xF0); // Soft Blue
            bgDarkColor = new Color(0xEB, 0xF4, 0xFA);
            borderColor = isSelected ? Color.ORANGE : new Color(0xA3, 0xC1, 0xAD);
        } else if (type.contains("filter") || type.contains("router") || type.contains("evaluator")) {
            bgLightColor = new Color(0xD2, 0xE9, 0xD2); // Soft Green
            bgDarkColor = new Color(0xE8, 0xF5, 0xE8);
            borderColor = isSelected ? Color.ORANGE : new Color(0xB2, 0xC4, 0xB2);
        } else if (type.contains("transformer") || type.contains("converter")) {
            bgLightColor = new Color(0xFD, 0xE2, 0xC4); // Soft Amber/Orange
            bgDarkColor = new Color(0xFF, 0xF3, 0xE3);
            borderColor = isSelected ? Color.ORANGE : new Color(0xE3, 0xC5, 0xA1);
        } else { // Sinks / Dispatchers / Default
            bgLightColor = new Color(0xE2, 0xD4, 0xEB); // Soft Purple
            bgDarkColor = new Color(0xF3, 0xEE, 0xF7);
            borderColor = isSelected ? Color.ORANGE : new Color(0xC6, 0xB4, 0xD4);
        }
    }

    private void initUI() {
        // Left Column: Step Indicator & Placeholder Icon Area
        JPanel iconPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        iconPanel.setOpaque(false);
        
        JLabel lblStep = new JLabel(String.format("Step %02d", stepIndex));
        lblStep.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblStep.setForeground(subTextColor);
        
        // Simulating the rich text layout headings
        JLabel lblIcon = new JLabel("⚙", SwingConstants.CENTER); // Fallback glyph icon
        lblIcon.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblIcon.setForeground(subTextColor);
        
        iconPanel.add(lblStep);
        iconPanel.add(lblIcon);
        add(iconPanel, BorderLayout.WEST);

        // Center Column: Core Descriptive Text Fields
        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);
        
        JLabel lblTitle = new JLabel(step.getComponentName());
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblTitle.setForeground(textColor);
        
        JLabel lblMeta = new JLabel("ID: " + step.getId());
        lblMeta.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblMeta.setForeground(subTextColor);
        
        textPanel.add(lblTitle);
        textPanel.add(lblMeta);
        add(textPanel, BorderLayout.CENTER);

        // Right Column: Type Contract Badges
        JPanel badgePanel = new JPanel(new GridBagLayout());
        badgePanel.setOpaque(false);
        
        if (!"None".equalsIgnoreCase(step.getOutputContract())) {
            JLabel lblBadge = new JLabel(" <" + step.getOutputContract() + "> ");
            lblBadge.setOpaque(true);
            lblBadge.setBackground(new Color(0xFF, 0xFF, 0xFF, 180));
            lblBadge.setForeground(textColor);
            lblBadge.setFont(new Font("Monospaced", Font.BOLD, 10));
            lblBadge.setBorder(BorderFactory.createLineBorder(new Color(0,0,0,30), 1, true));
            badgePanel.add(lblBadge);
        }
        
        add(badgePanel, BorderLayout.EAST);
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Native Java2D anti-aliasing initialization for ultra-crisp edges
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int arc = 16; // Edge roundness match from visual prototype

        // 1. Render the Subtle Drop Shadow
        g2.setColor(new Color(0, 0, 0, 15));
        g2.fillRoundRect(3, 5, w - 6, h - 7, arc, arc); // Offset shadow layer
        g2.fillRoundRect(4, 6, w - 8, h - 8, arc, arc);

        // 2. Render Gradient Card Background Body
        GradientPaint gradient = new GradientPaint(0, 0, bgLightColor, 0, h, bgDarkColor);
        g2.setPaint(gradient);
        g2.fillRoundRect(2, 2, w - 5, h - 6, arc, arc);

        // 3. Render Card Frame Border (Changes color if item is selected)
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(isSelected ? 2.0f : 1.2f));
        g2.drawRoundRect(2, 2, w - 5, h - 6, arc, arc);

        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(450, 72); // Fixed canvas card bounding boxes
    }

}
