package org.jwellman.demo.spinners;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jwellman.demo.animation.Engine;

/**
 * 
 * @author rwellman
 *
 */
@SuppressWarnings("serial")
public class SpinnerPanel extends JPanel {

    private Color color = new Color(0, 120, 215); // Windows Blue;

    private boolean upperClip;

    private boolean lowerClip;

    private double globalAngle = 90; // Driven by your animation/tween framework

    public void setAnimationProgress(double angle) {
        this.globalAngle = angle;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int radius = 40;
        int numDots = 5;
        int dotWidth = 8;
        int clipRadius = radius + (2*dotWidth);

        // 1. Define the visible boundary (e.g., top-left 270 degrees only)
        // Adjust the clip shape to define where dots appear/disappear
        if (upperClip) { g2d.setClip(centerX - clipRadius, centerY - clipRadius, clipRadius*2, clipRadius); };
        if (lowerClip) { g2d.setClip(centerX - clipRadius, centerY, clipRadius*2, clipRadius); };

        // 2. Draw dots along the circle
        g2d.setColor(color ); // Windows Blue
        for (int i = 0; i < numDots; i++) {

            // Stagger each dot slightly behind the leader
            double dotAngle = Math.toRadians(globalAngle - (i * 15)); 

            int x = (int) (centerX + radius * Math.cos(dotAngle));
            int y = (int) (centerY + radius * Math.sin(dotAngle));

            // Draw dot
            g2d.fill(new Ellipse2D.Double(x - 4, y - 4, dotWidth, dotWidth));
        }

        g2d.dispose();
    }

    // --- Usage Example ---
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            SpinnerPanel panel = new SpinnerPanel();
            panel.setBorder(BorderFactory.createLineBorder(Color.black));

            final AtomicInteger atomicLoops = new AtomicInteger(0);
            Engine.getInstance().register(3000, Engine.Easing.LINEAR, Engine.LoopMode.SAWTOOTH, c -> {
                int loops = atomicLoops.get();
                if (loops%4 == 0) { panel.setUpperClip(true); panel.setLowerClip(false); }
                if (loops%4 == 1) { panel.setUpperClip(false); panel.setLowerClip(false); }
                if (loops%4 == 2) { panel.setUpperClip(false); panel.setLowerClip(true); }
                if (loops%4 == 3) { panel.setUpperClip(false); panel.setLowerClip(false); }
                panel.setAnimationProgress(c.value * 360.0);

                if (c.value > 0.989) {
                    atomicLoops.incrementAndGet();
                    System.out.print("> ");
                    System.out.print(c.value);
                    System.out.println(" : " + loops%4);
                }

                return true;
            });

            JFrame frame = new JFrame("SpinnerPanel Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            // frame.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));

            frame.add(panel, BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public boolean isUpperClip() {
        return upperClip;
    }

    public void setUpperClip(boolean upperClip) {
        this.upperClip = upperClip;
    }

    public boolean isLowerClip() {
        return lowerClip;
    }

    public void setLowerClip(boolean lowerClip) {
        this.lowerClip = lowerClip;
    }

}
