package org.jwellman.demo.gauge;

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
    private final Color VALUE_COLOR = new Color(200, 240, 255);

    private int lastCalculatedSize = -1;
    private Font cachedFont = null;

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

        // CRITICAL: Turn on high-quality rendering & anti-aliasing
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

     // --- DYNAMICALLY SCALING CENTER TEXT READOUT (OPTIMIZED) ---
        int displayInt = (int) Math.round(Math.max(0.0, Math.min(100.0, percentage)));
        String textDisplay = String.format("%d%%", displayInt);

        // 1. Use the pre-computed, cached font reference directly!
        if (cachedFont != null) {
            g2.setFont(cachedFont);
        } else {
            // Fallback security check just in case layout initializes oddly
            g2.setFont(new Font("SansSerif", Font.BOLD, 12)); 
        }

        // 2. FontMetrics measurement is incredibly lightweight now
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(textDisplay);
        int textHeight = fm.getAscent();

        int textX = (getWidth() - textWidth) / 2;
        int textY = (getHeight() + textHeight) / 2 - (fm.getDescent() / 2);

        g2.setColor(VALUE_COLOR);
        g2.drawString(textDisplay, textX, textY);
        
        g2.dispose();
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);

        // Calculate size exactly like we do in paintComponent
        int size = Math.min(width, height);

        // Only rebuild the font if the component size has actually shifted!
        if (size != lastCalculatedSize && size > 0) {
            lastCalculatedSize = size;

            int padding = 20;
            int diameter = size - (padding * 2);
            float targetFontSize = diameter * 0.28f;

            // Derive or instantiate the font once and store it in memory
            this.cachedFont = new Font("SansSerif", Font.BOLD, (int) targetFontSize);
        }
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
