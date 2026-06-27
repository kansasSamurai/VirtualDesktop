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
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import org.katacode.pipeline.engine.PipelineStep;

/**
 * A custom-rendered JToggleButton that natively manages its selection state
 * while rendering our soft-palette, high-craft layout style.
 */
@SuppressWarnings("serial")
public class PipelineCardPanel extends JToggleButton {

    private final PipelineStep step;
    private final int stepIndex;
    
    // Soft-palette specs
    private Color bgLightColor;
    private Color bgDarkColor;
    private Color borderColor;

    public interface Resources {
        Color textColor();
        Color subTextColor();
        Font iconFont();
        Font outputBadgeFont();
        Border outputBadgeBorder();
        Font stepTitleFont();
        Font stepMetaFont();
        Dimension closeButtonPreferredSize();
    }

    public static class Customizer implements Resources {

        private Font outputBadgeFont = new Font("Monospaced", Font.BOLD, 12);
        private Border outputBadgeBorder = BorderFactory.createLineBorder(new Color(0,0,0,30), 1, true);
        private Dimension closeButtonPreferredSize = new Dimension(20, 20);
        private Color textColor = new Color(0x22, 0x22, 0x22);
        private Color subTextColor = new Color(0x55, 0x55, 0x55);
        private Font stepTitleFont = new Font("SansSerif", Font.BOLD, 16);
        private Font stepMetaFont = new Font("SansSerif", Font.PLAIN, 14);
                // new Font("SansSerif", Font.ITALIC, 11);
        private Font iconFont = new Font("SansSerif", Font.BOLD, 44);
        
        @Override
        public Dimension closeButtonPreferredSize() { return closeButtonPreferredSize; }

        @Override
        public Border outputBadgeBorder() { return outputBadgeBorder; }

        @Override
        public Font outputBadgeFont() { return outputBadgeFont; }

        @Override
        public Color textColor() { return textColor; }

        @Override
        public Color subTextColor() { return subTextColor; }

        @Override
        public Font stepTitleFont() { return stepTitleFont; }

        @Override
        public Font stepMetaFont() { return stepMetaFont; }

        @Override
        public Font iconFont() { return iconFont; }

    }

    public static Customizer CUSTOMIZER = new Customizer();    
    public PipelineCardPanel(PipelineStep step, int stepIndex) {
        this.step = step;
        this.stepIndex = stepIndex;

        // 1. Strip the default OS Button decorations to make it an "undecorated canvas"
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        
        setBorder(new EmptyBorder(12, 16, 12, 16));
        setLayout(new BorderLayout(14, 0));
        
        assignPaletteColors();
        initUI();
    }

    private void assignPaletteColors() {
        String type = step.getComponentName().toLowerCase();
        if (type.contains("reader") || type.contains("source")) {
            bgLightColor = new Color(0xD6, 0xE4, 0xF0);
            bgDarkColor = new Color(0xEB, 0xF4, 0xFA);
        } else if (type.contains("filter") || type.contains("router") || type.contains("evaluator")) {
            bgLightColor = new Color(0xD2, 0xE9, 0xD2);
            bgDarkColor = new Color(0xE8, 0xF5, 0xE8);
        } else if (type.contains("transformer") || type.contains("converter")) {
            bgLightColor = new Color(0xFD, 0xE2, 0xC4);
            bgDarkColor = new Color(0xFF, 0xF3, 0xE3);
        } else {
            bgLightColor = new Color(0xE2, 0xD4, 0xEB);
            bgDarkColor = new Color(0xF3, 0xEE, 0xF7);
        }
    }

    private void initUI() {
        // Left Side: Step Marker & Icon
        JPanel iconPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        iconPanel.setOpaque(false);
        
        JLabel lblStep = new JLabel(String.format("Step %02d", stepIndex));
        lblStep.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblStep.setForeground(CUSTOMIZER.subTextColor());
        
        JLabel lblIcon = new JLabel("⚙", SwingConstants.CENTER);
        lblIcon.setFont(new Font("SansSerif", Font.BOLD, 44));
        lblIcon.setForeground(CUSTOMIZER.subTextColor());
        
        // iconPanel.add(lblStep);
        iconPanel.add(lblIcon);
        add(iconPanel, BorderLayout.WEST);

        // Center Side: Labels
        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);
        
        JLabel lblTitle = new JLabel(step.getComponentName());
        lblTitle.setFont(CUSTOMIZER.stepTitleFont());
        lblTitle.setForeground(CUSTOMIZER.textColor());
        
        JLabel lblMeta = new JLabel(String.format("Step %02d", stepIndex));
                // new JLabel("ID: " + step.getId());
        lblMeta.setFont(CUSTOMIZER.stepMetaFont());
        lblMeta.setForeground(CUSTOMIZER.subTextColor());
        
        textPanel.add(lblMeta);
        textPanel.add(lblTitle);
        add(textPanel, BorderLayout.CENTER);

        // Right Side: Contract Badges
        JPanel badgePanel = new JPanel(new GridBagLayout());
        badgePanel.setOpaque(false);
        
        if (!"None".equalsIgnoreCase(step.getOutputContract())) {
            JLabel lblBadge = new JLabel(" <" + step.getOutputContract() + "> ");
            lblBadge.setOpaque(false);
            // lblBadge.setBackground(new Color(0xFF, 0xFF, 0xFF, 180));
            lblBadge.setForeground(CUSTOMIZER.subTextColor());
            lblBadge.setFont(CUSTOMIZER.outputBadgeFont());
            lblBadge.setBorder(CUSTOMIZER.outputBadgeBorder());
            badgePanel.add(lblBadge);
        }
        add(badgePanel, BorderLayout.EAST);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int arc = 16;

        // Query the native button state model directly for selection!
        boolean selected = isSelected();
        borderColor = selected ? Color.ORANGE : new Color(0xBD, 0xC3, 0xC7);

        // Render Shadow
        g2.setColor(new Color(0, 0, 0, 15));
        g2.fillRoundRect(3, 5, w - 6, h - 7, arc, arc);

        // Render Background Gradient
        // GradientPaint gradient = new GradientPaint(0, 0, bgLightColor, 0, h, bgDarkColor);
        g2.setPaint(bgLightColor); // gradient
        g2.fillRoundRect(2, 2, w - 5, h - 6, arc, arc);

        // Render Dynamic Border Stroke
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(selected ? 2.2f : 1.2f));
        g2.drawRoundRect(2, 2, w - 5, h - 6, arc, arc);

        g2.dispose();
        super.paintComponent(g); // Ensures child text components display safely
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(450, 103);
    }

    public PipelineStep getStep() {
        return step;
    }

}