package org.jwellman.demo;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class ShadowRendererDemo extends JFrame {

    public ShadowRendererDemo() {
        setTitle("High-Craft Shadow Comparison");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 450);
        setLocationRelativeTo(null);

        // A mock canvas holding two shadow panels side-by-side
        JPanel canvas = new JPanel(new GridLayout(1, 2, 20, 0));
        canvas.setBackground(new Color(0xF4, 0xF5, 0xF7)); // Soft off-white canvas
        canvas.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        // Choice 1: Mathematical/Algorithmic Multi-Pass Shadow
        canvas.add(new ShadowDisplayPanel("Multi-Pass Procedural Shadow", false));

        // Choice 2: Per-Pixel Gaussian Blur Buffered Image
        canvas.add(new ShadowDisplayPanel("True Gaussian Image Shadow", true));

        add(canvas);
    }

    /**
     * A panel that acts as the delegated shadow layer, rendering a mock node on top.
     */
    private static class ShadowDisplayPanel extends JPanel {
        private final String title;
        private final boolean useGaussian;
        private BufferedImage cachedGaussianShadow = null;

        public ShadowDisplayPanel(String title, boolean useGaussian) {
            this.title = title;
            this.useGaussian = useGaussian;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Define the size and position of our actual visual "Class Box"
            int nodeW = 240;
            int nodeH = 180;
            int nodeX = (getWidth() - nodeW) / 2;
            int nodeY = (getHeight() - nodeH) / 2 - 10; 
            int cornerRadius = 8;

            // --- PAINT SHADOW ---
            if (useGaussian) {
                paintGaussianShadow(g2d, nodeX, nodeY, nodeW, nodeH, cornerRadius);
            } else {
                paintMultiPassShadow(g2d, nodeX, nodeY, nodeW, nodeH, cornerRadius);
            }

            // --- PAINT ACTUAL NODE (Pure White Card) ---
            g2d.setColor(Color.WHITE);
            g2d.fillRoundRect(nodeX, nodeY, nodeW, nodeH, cornerRadius, cornerRadius);

            // Thin, crisp, elegant border accent (subtle desaturated gray)
            g2d.setColor(new Color(0, 0, 0, 20));
            g2d.drawRoundRect(nodeX, nodeY, nodeW, nodeH, cornerRadius, cornerRadius);

            // Text Typography Label
            g2d.setColor(new Color(0x2C, 0x3E, 0x50));
            g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(title, nodeX + (nodeW - fm.stringWidth(title)) / 2, nodeY + (nodeH / 2) + 5);

            g2d.dispose();
        }

        /**
         * Choice 1: Multi-Pass Algorithmic Shadow.
         * Simulates a blur by recursively painting the rounded rectangle, expanding outward 
         * while fading the opacity exponentially. High performance, zero image memory overhead.
         */
        private void paintMultiPassShadow(Graphics2D g2d, int x, int y, int w, int h, int radius) {
            int shadowSize = 12;      // Total blur radius expansion
            int offsetY = 6;          // Directional drop downward
            float maxOpacity = 0.04f; // Very soft base opacity

            // We loop from the outside edge inward to build an exponential falloff density profile
            for (int i = shadowSize; i >= 0; i--) {
                // Exponential decay for opacity approximation
                float progress = (float) i / shadowSize;
                float opacity = maxOpacity * (1.0f - (progress * progress)); 
                
                g2d.setColor(new Color(0, 0, 0, opacity));
                
                // Expand boundaries slightly per pass
                int sizeCompensation = i * 2;
                g2d.fillRoundRect(
                        x - i, 
                        y - i + offsetY, 
                        w + sizeCompensation, 
                        h + sizeCompensation, 
                        radius + sizeCompensation, 
                        radius + sizeCompensation
                );
            }
        }

        /**
         * Choice 2: True Per-Pixel Gaussian Blur Shadow.
         * Generates a solid mask on a buffered image and applies a raster convolution matrix blur.
         * Yields the absolute highest visual fidelity.
         */
        private void paintGaussianShadow(Graphics2D g2d, int x, int y, int w, int h, int radius) {
            int padding = 24; // Extra image padding room for blur bleed
            int offsetY = 6;

            if (cachedGaussianShadow == null) {
                // Create a clear canvas larger than the node to handle the bleed
                int imgW = w + (padding * 2);
                int imgH = h + (padding * 2);
                BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D imgG = img.createGraphics();
                imgG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw the initial solid mask (very faint gray to establish base shadow tint)
                imgG.setColor(new Color(0, 0, 0, 110));
                imgG.fillRoundRect(padding, padding, w, h, radius, radius);
                imgG.dispose();

                // Apply a standard 5x5 Gaussian Kernel blur matrix
                float[] matrix = {
                    1f/273f,  4f/273f,  7f/273f,  4f/273f, 1f/273f,
                    4f/273f, 16f/273f, 26f/273f, 16f/273f, 4f/273f,
                    7f/273f, 26f/273f, 41f/273f, 26f/273f, 7f/273f,
                    4f/273f, 16f/273f, 26f/273f, 16f/273f, 4f/273f,
                    1f/273f,  4f/273f,  7f/273f,  4f/273f, 1f/273f
                };
                
                // Convolve twice to deepen the soft dispersion radius
                ConvolveOp blurOp = new ConvolveOp(new Kernel(5, 5, matrix), ConvolveOp.EDGE_NO_OP, null);
                BufferedImage midBlur = blurOp.filter(img, null);
                cachedGaussianShadow = blurOp.filter(midBlur, null);
            }

            // Render the cached raster shadow perfectly positioned behind the element
            g2d.drawImage(cachedGaussianShadow, x - padding, y - padding + offsetY, null);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ShadowRendererDemo().setVisible(true));
    }

}
