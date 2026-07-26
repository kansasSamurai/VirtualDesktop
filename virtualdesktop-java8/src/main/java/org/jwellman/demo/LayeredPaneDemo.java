package org.jwellman.demo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LayeredPaneDemo extends JFrame {

    public LayeredPaneDemo() {
        setTitle("JLayeredPane Auto-Resize Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        // 1. Create the JLayeredPane
        JLayeredPane layeredPane = new JLayeredPane();

        // ---------------------------------------------------------------------
        // TOGGLE THIS LINE TO SEE THE DIFFERENCE:
        // ---------------------------------------------------------------------
        // WITH OverlayLayout: Base panel & overlay automatically stretch on window resize.
        // WITHOUT (commented out): Base panel & overlay stay stuck at 600x400.
        layeredPane.setLayout(new OverlayLayout(layeredPane));
        // ---------------------------------------------------------------------

        // 2. Create the Base Layer (Standard UI)
        JPanel baseLayer = createBaseLayer();
        baseLayer.setBounds(0, 0, 600, 400); // Initial size if null layout is used

        // 3. Create the Overlay Layer (Drag / Selection Ghost)
        SelectionOverlayPanel overlayLayer = new SelectionOverlayPanel();
        overlayLayer.setBounds(0, 0, 600, 400); // Initial size if null layout is used

        // 4. Add layers to JLayeredPane with appropriate Z-depths
        layeredPane.add(baseLayer, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(overlayLayer, JLayeredPane.DRAG_LAYER);

        // 5. Add JLayeredPane to Frame's ContentPane (uses BorderLayout by default)
        add(layeredPane, BorderLayout.CENTER);
    }

    private JPanel createBaseLayer() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(230, 230, 235));

        panel.add(new JButton("Base Layer Button 1"));
        panel.add(new JButton("Base Layer Button 2"));
        panel.add(new JTextField("Type here..."));
        panel.add(new JLabel("Drag on window to draw marquee overlay!", SwingConstants.CENTER));

        return panel;
    }

    /**
     * Transparent panel sitting on DRAG_LAYER that draws a selection box on drag.
     */
    private static class SelectionOverlayPanel extends JPanel {
        private Point startPoint;
        private Point currentPoint;

        public SelectionOverlayPanel() {
            setOpaque(false); // Crucial! Lets lower layers show through

            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    startPoint = e.getPoint();
                    currentPoint = startPoint;
                    repaint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    currentPoint = e.getPoint();
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    startPoint = null;
                    currentPoint = null;
                    repaint();
                }
            };

            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (startPoint != null && currentPoint != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                
                int x = Math.min(startPoint.x, currentPoint.x);
                int y = Math.min(startPoint.y, currentPoint.y);
                int width = Math.abs(startPoint.x - currentPoint.x);
                int height = Math.abs(startPoint.y - currentPoint.y);

                // Draw translucent blue drag box
                g2.setColor(new Color(0, 120, 215, 60));
                g2.fillRect(x, y, width, height);

                // Draw dark blue outline
                g2.setColor(new Color(0, 120, 215, 200));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRect(x, y, width, height);

                g2.dispose();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LayeredPaneDemo().setVisible(true));
    }

}
