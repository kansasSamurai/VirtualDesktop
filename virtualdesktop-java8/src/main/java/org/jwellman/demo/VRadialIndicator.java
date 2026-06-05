package org.jwellman.demo;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

public class VRadialIndicator extends JComponent {

    private static final long serialVersionUID = 1L;
    
    private double percentage = 0.0; // 0.0 to 100.0
    
    // Theme Colors matching your mockup
    private final Color colorTrackBg = new Color(0, 50, 100, 80);
    private final Color colorTrackActive = new Color(0, 150, 255);
    private final Color colorGlowCap = new Color(200, 240, 255);
    private final Color colorOuterAccents = new Color(0, 130, 220);

    public VRadialIndicator() {
        setPreferredSize(new Dimension(150, 150));
    }

    public void setPercentage(double val) {
        // Clamp between 0 and 100
        this.percentage = Math.max(0.0, Math.min(100.0, val));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        // CRITICAL FOR SCI-FI UI: Turn on high-quality rendering & anti-aliasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int size = Math.min(getWidth(), getHeight());
        int padding = 20; // Room for the outer ticks
        int diameter = size - (padding * 2);
        int x = (getWidth() - diameter) / 2;
        int y = (getHeight() - diameter) / 2;

        // --- Math Constants ---
        double startAngle = -90.0; // 6 o'clock (Bottom)
        double extentAngle = -(percentage * 3.6); // Negative means clockwise sweep

        // 1. Draw Background Track Ring
        g2.setStroke(new BasicStroke(16f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g2.setColor(colorTrackBg);
        g2.draw(new Arc2D.Double(x, y, diameter, diameter, 0, 360, Arc2D.OPEN));

        // 2. Draw Active Progress Arc
        g2.setColor(colorTrackActive);
        Arc2D.Double progressArc = new Arc2D.Double(x, y, diameter, diameter, startAngle, extentAngle, Arc2D.OPEN);
        g2.draw(progressArc);

        // 3. Draw the Leading Edge Glow Cap (The Bright White Tip)
        if (percentage > 0) {
            // Trigonometry to find the exact tip point of the arc
            double endAngleRadians = Math.toRadians(startAngle + extentAngle);
            double radius = diameter / 2.0;
            double centerX = x + radius;
            double centerY = y + radius;
            
            // Calculate exact (x,y) tip coordinate
            double capX = centerX + radius * Math.cos(endAngleRadians);
            double capY = centerY - radius * Math.sin(endAngleRadians); // Subtracted because Swing Y is down

            // Draw a bright glowing dot right at the tip
            int capSize = 18;
            g2.setColor(colorGlowCap);
            g2.fill(new Ellipse2D.Double(capX - (capSize/2.0), capY - (capSize/2.0), capSize, capSize));
        }

        // 4. Draw Outer Accents (The thin framing brackets)
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(colorOuterAccents);
        int accentPadding = 16;
        int outerD = diameter + (accentPadding * 2);
        int outerX = (getWidth() - outerD) / 2;
        int outerY = (getHeight() - outerD) / 2;
        
        // Left accent bracket
        g2.draw(new Arc2D.Double(outerX, outerY, outerD, outerD, 110, 140, Arc2D.OPEN));
        // Right accent bracket
        g2.draw(new Arc2D.Double(outerX, outerY, outerD, outerD, -70, 140, Arc2D.OPEN));

        // --- NEW: DYNAMICALLY SCALING CENTER TEXT READOUT ---

        // 1. Convert the current percentage to a clean integer string (e.g., "75%")
        String textDisplay = String.format("%d%%", (int) Math.round(percentage));

        // 2. Compute a proportional target font size based on current circle diameter
        // 28% of the core diameter keeps the text crisp, highly visible, and perfectly
        // safely framed
        float targetFontSize = diameter * 0.28f;
        g2.setFont(new Font("SansSerif", Font.BOLD, (int) targetFontSize));

        // 3. Measure the exact pixel geometry of the string using FontMetrics
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(textDisplay);
        int textHeight = fm.getAscent(); // Use ascent for vertical positioning rather than full height

        // 4. Center the text perfectly inside the available space
        int textX = (getWidth() - textWidth) / 2;
        int textY = (getHeight() + textHeight) / 2 - (fm.getDescent() / 2);

        // 5. Apply a high-contrast glowing cyan color matching your sci-fi motif
        g2.setColor(new Color(200, 240, 255));
        g2.drawString(textDisplay, textX, textY);
     
        g2.dispose();
    }

    // --- QUICK RUNNABLE TEST TO WATCH IT ANIMATE ---
    public static void main(String[] args) {
        JFrame frame = new JFrame("Radial Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(new Color(5, 15, 30)); // Deep space background
        frame.setLayout(new GridBagLayout());

        VRadialIndicator indicator = new VRadialIndicator();
        frame.add(indicator);
        frame.setSize(300, 300);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Animate it up and down using a standard Swing Timer
        Timer timer = new Timer(30, null);
        timer.addActionListener(e -> {
            long time = System.currentTimeMillis();
            // Create a smooth sine wave cycle between 0% and 100%
            double wave = (Math.sin(time / 1000.0) + 1.0) / 2.0; 
            indicator.setPercentage(wave * 100.0);
        });
        timer.start();
    }

}
