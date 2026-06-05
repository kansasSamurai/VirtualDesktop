package org.jwellman.demo.gauge;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

public class VRadialIndicator2 extends JComponent {

    private static final long serialVersionUID = 1L;

    private static final Stroke TRACK_STROKE = new BasicStroke(14f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);

    private static final Stroke BRACKET_STROKE = new BasicStroke(1.5f);
    
    private boolean indeterminate = false;
    private double percentage = 0.0;    // Used in determinate mode (0 to 100)
    private double animationAngle = -90; // Used in indeterminate mode (tracks rotation)
    
    // Theme Colors
    private final Color colorTrackBg = new Color(0, 50, 100, 80);
    private final Color colorTrackActive = new Color(0, 150, 255);
    private final Color colorOuterAccents = new Color(0, 130, 220);
    private final Color colorGlowCap = new Color(200, 240, 255);

    // --- The Cached Cap Image ---
    private final int capSize = 28; // 24; // Slightly larger to allow the gradient bloom to blend nicely
    private final BufferedImage cachedCapImage;

    public VRadialIndicator2() {
        setPreferredSize(new Dimension(150, 150));
        this.cachedCapImage = createCachedCap(capSize);
    }

    /**
     * Pre-computes the RadialGradientPaint cap onto a transparent image once at startup.
     */
    private BufferedImage createCachedCap(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float radius = size / 2f;
        float[] fractions = {0.0f, 0.3f, 1.0f};
            // {0.0f, 0.99f, 1.0f};
            // original {0.0f, 0.3f, 1.0f};
        Color[] colors = {
            Color.WHITE,                     // Hot white core
            new Color(0, 180, 255, 255),     // Electric neon blue inner bloom
            new Color(0, 150, 255, 0)        // Completely transparent edge for smooth feathering
        };

        RadialGradientPaint gradient = new RadialGradientPaint(
            radius, radius, radius, fractions, colors
        );
        g2.setPaint(gradient);
        g2.fillRect(0, 0, size, size);

        int coreSize = 14; double offset = (size-coreSize)/2.0;
        g2.setColor(colorGlowCap);
        g2.fill(new Ellipse2D.Double(offset, offset, coreSize, coreSize));

        g2.dispose();
        return img;
    }

    public void setPercentage(double val) {
        this.indeterminate = false;
        this.percentage = Math.max(0.0, Math.min(100.0, val));
        repaint();
    }

    public void setIndeterminate(boolean indeterminate) {
        this.indeterminate = indeterminate;
        repaint();
    }

    /**
     * Increments the spinner angle. Call this inside your animation timer when indeterminate.
     */
    public void advanceIndeterminateLoop(double degreeDelta) {
        if (!indeterminate) return;
        // Keep the angle negative to maintain the clockwise sweeping math
        this.animationAngle -= degreeDelta;
        if (this.animationAngle < -450) { // Wrap around seamlessly past 360 degrees
            this.animationAngle += 360;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int size = Math.min(getWidth(), getHeight());
        int padding = 24; 
        int diameter = size - (padding * 2);
        int x = (getWidth() - diameter) / 2;
        int y = (getHeight() - diameter) / 2;

        // --- Calculate Track Layout Parameters based on mode ---
        double startAngle;
        double extentAngle;

        if (indeterminate) {
            startAngle = animationAngle;
            extentAngle = -90.0; // A fixed-width 90-degree crescent that chases its tail
        } else {
            startAngle = -90.0; // Anchored at 6 o'clock
            extentAngle = -(percentage * 3.6);
        }

        // 1. Draw Background Track Ring
        g2.setStroke(TRACK_STROKE);
        g2.setColor(colorTrackBg);
        g2.draw(new Arc2D.Double(x, y, diameter, diameter, 0, 360, Arc2D.OPEN));

        // 2. Draw Active Track Arc
        g2.setColor(colorTrackActive);
        g2.draw(new Arc2D.Double(x, y, diameter, diameter, startAngle, extentAngle, Arc2D.OPEN));

        // 3. Draw the Cached Glow Cap at the leading edge
        if (indeterminate || percentage > 0) {
            double endAngleRadians = Math.toRadians(startAngle + extentAngle);
            double radius = diameter / 2.0;
            double centerX = x + radius;
            double centerY = y + radius;
            
            // Map arc endpoint to exact component coordinates
            double capX = centerX + radius * Math.cos(endAngleRadians);
            double capY = centerY - radius * Math.sin(endAngleRadians);

            // Stamp the cached image down, centered directly over the coordinates
            g2.drawImage(cachedCapImage, 
                (int)(capX - (capSize / 2.0)), 
                (int)(capY - (capSize / 2.0)), 
                null
            );
        }

        // 4. Draw Outer Brackets
        g2.setStroke(BRACKET_STROKE);
        g2.setColor(colorOuterAccents);
        int accentPadding = 16; // 8
        int outerD = diameter + (accentPadding * 2);
        int outerX = (getWidth() - outerD) / 2;
        int outerY = (getHeight() - outerD) / 2;
        
        g2.draw(new Arc2D.Double(outerX, outerY, outerD, outerD, 110, 140, Arc2D.OPEN));
        g2.draw(new Arc2D.Double(outerX, outerY, outerD, outerD, -70, 140, Arc2D.OPEN));

        g2.dispose();
    }

    // --- RUNNABLE DEMO SHOWCASING INDETERMINATE MODE ---
    public static void main(String[] args) {
        JFrame frame = new JFrame("Sci-Fi Progress Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(new Color(5, 15, 30));
        frame.setLayout(new GridBagLayout());

        VRadialIndicator2 indicator = new VRadialIndicator2();
        
        // Turn on our brand-new indeterminate tracking mode
        indicator.setIndeterminate(true);
        
        frame.add(indicator);
        frame.setSize(300, 300);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Drive the sweeping animation loop
        Timer timer = new Timer(16, e -> {
            // Advance the spinner 4 degrees clockwise every frame (~60 FPS feel)
            indicator.advanceIndeterminateLoop(4.0);
        });
        timer.start();
    }

}
