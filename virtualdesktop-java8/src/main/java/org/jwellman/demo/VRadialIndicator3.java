package org.jwellman.demo;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

public class VRadialIndicator3 extends JComponent {

    private static final long serialVersionUID = 1L;

    private static final float TRACK_STROKE_WIDTH = 14f;
    
    private static final Stroke TRACK_STROKE = new BasicStroke(TRACK_STROKE_WIDTH, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);

    private static final Stroke BRACKET_STROKE = new BasicStroke(1.5f);

    private float currentCoreHue = 0.0f; // Shifts from 0.0 to 1.0
    private float rainbowColorAngle = 0.0f;
    private double percentage = 0.0;    // Used in determinate mode (0 to 100)
    private double animationAngle = -90; // Used in indeterminate mode (tracks rotation)
    private boolean indeterminate = false;

    // Theme Colors
    private final Color colorTrackBg = new Color(0, 50, 100, 80);
    private final Color colorTrackActive = new Color(0, 150, 255);
    private final Color colorOuterAccents = new Color(0, 130, 220);
    private final Color colorGlowCap = new Color(200, 240, 255);

    // --- The Cached Cap Image ---
    private final int capSize = 28; // 24; // Slightly larger to allow the gradient bloom to blend nicely
    private final BufferedImage cachedCapImage;

    public VRadialIndicator3() {
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

        // start
     // --- FIXED UNIFORM RAINBOW CORE DRAWING LOGIC ---

     // 1. Calculate the exact bounding circle for the blank inner core
     int corePadding = 0; // Slight spacing so it doesn't bleed into the progress ring
     int track = (int)(TRACK_STROKE_WIDTH / 2) + 7; // TODO need to figure out this formula so no manual adjustment is necessary
     int coreDiameter = diameter - track - (corePadding * 2); // subtract track stroke width
     int coreX = (getWidth() - coreDiameter) / 2;
     int coreY = (getHeight() - coreDiameter) / 2;

     // 2. Generate the pure spectral color based on our current animated hue
     // Saturation = 0.9 (vibrant), Brightness = 0.8 (bright but not blinding)
     Color spectralColor = Color.getHSBColor(currentCoreHue, 0.9f, 0.8f);

     // 3. Inject an elegant transparency alpha channel (e.g., 90 out of 255) 
     // so it matches your dark cyberpunk theme backdrop
     Color fadingCoreColor = new Color(
         spectralColor.getRed(), 
         spectralColor.getGreen(), 
         spectralColor.getBlue(), 
         210 // 90
     );

     // 4. Fill the entire inner core uniformly with our solid shifting color
     g2.setColor(fadingCoreColor);
     g2.fill(new java.awt.geom.Ellipse2D.Double(coreX, coreY, coreDiameter, coreDiameter));
     // end

     // 4. Draw Outer Brackets
     g2.setStroke(BRACKET_STROKE);
     g2.setColor(colorOuterAccents);
     int accentPadding = 16; // 8
     int outerD = diameter + (accentPadding * 2);
     int outerX = (getWidth() - outerD) / 2;
     int outerY = (getHeight() - outerD) / 2;

     g2.draw(new Arc2D.Double(outerX, outerY, outerD, outerD, 110, 140, Arc2D.OPEN));
     g2.draw(new Arc2D.Double(outerX, outerY, outerD, outerD, -70, 140, Arc2D.OPEN));

     if (true) {
         g2.dispose();
         return;
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

        g2.dispose();
    }

    @SuppressWarnings("unused")
    private void saturnCore(Graphics2D g2, int diameter) {

        // --- NEW RAINBOW CORE DRAWING LOGIC ---

        // 1. Calculate the exact bounding circle for the blank inner core
        int corePadding = 2; // Slight spacing so it doesn't bleed into the progress ring
        int coreDiameter = diameter - (int)(TRACK_STROKE_WIDTH * 2) - (corePadding * 2); // subtract track stroke width
        int coreX = (getWidth() - coreDiameter) / 2;
        int coreY = (getHeight() - coreDiameter) / 2;

        // 2. Find the exact center point of the component
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        // 3. Use trigonometry to calculate a moving line across the center based on our angle
        double rads = Math.toRadians(rainbowColorAngle);
        float lineLength = coreDiameter / 2f; // The width of the gradient transition
        float p1x = centerX - (float)Math.cos(rads) * lineLength;
        float p1y = centerY - (float)Math.sin(rads) * lineLength;
        float p2x = centerX + (float)Math.cos(rads) * lineLength;
        float p2y = centerY + (float)Math.sin(rads) * lineLength;

        // 4. Define the pure rainbow spectrum fractions (0.0 to 1.0)
        float[] fractions = {0.0f, 0.2f, 0.4f, 0.6f, 0.8f, 1.0f};

        // 5. Define the matching colors of the rainbow (with an elegant 40% transparency so it fits the theme)
        Color[] rainbowColors = {
            new Color(255, 0, 0, 100),     // Red
            new Color(255, 200, 0, 100),   // Yellow / Orange
            new Color(0, 255, 0, 100),     // Green
            new Color(0, 255, 255, 100),   // Cyan / Blue
            new Color(200, 0, 255, 100),   // Purple
            new Color(255, 0, 0, 100)      // Wrap back to Red for a perfect, seamless loop
        };

        // 6. Create and apply the moving linear gradient
        LinearGradientPaint rainbowGradient = new LinearGradientPaint(
            p1x, p1y, p2x, p2y, fractions, rainbowColors
        );
        g2.setPaint(rainbowGradient);

        // 7. Fill the inner circle with the shifting rainbow spectrum
        g2.fill(new java.awt.geom.Ellipse2D.Double(coreX, coreY, coreDiameter, coreDiameter));
        
    }

    // --- RUNNABLE DEMO SHOWCASING INDETERMINATE MODE ---
    public static void main(String[] args) {
        JFrame frame = new JFrame("Sci-Fi Progress Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(new Color(5, 15, 30));
        frame.setLayout(new GridBagLayout());

        VRadialIndicator3 indicator = new VRadialIndicator3();
        
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
            
            // Advance the rainbow shift 2 degrees every frame for a smooth, active swirl
            indicator.rainbowColorAngle += 2.0f;
            if (indicator.rainbowColorAngle > 360.0f) {
                indicator.rainbowColorAngle -= 360.0f;
            }

            // Advance the color hue by 0.005 every frame for a mesmerizing, slow morph
            indicator.currentCoreHue += 0.005f;
            if (indicator.currentCoreHue > 1.0f) {
                indicator.currentCoreHue -= 1.0f; // Wrap around seamlessly
            }
        });
        timer.start();
    }

}
