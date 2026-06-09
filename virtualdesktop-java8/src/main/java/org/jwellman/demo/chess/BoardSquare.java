package org.jwellman.demo.chess;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

@SuppressWarnings("serial")
public class BoardSquare extends JPanel {

    private Color baseColor;
    private boolean targeted = false;
    private boolean highlighted = false;
    private int rank;
    private int file;

    private static final Color HIGHLIGHT = new Color(59, 130, 246, 60);

    public BoardSquare(Color baseColor, int rank, int file) {
        this.baseColor = baseColor;
        this.setOpaque(true); // Let Swing optimize background clearing
        this.setBackground(this.baseColor);
        this.rank = rank;
        this.file = file;
    }

    // --- Reactive Setters ---

    public void setTargeted(boolean targeted) {
        if (this.targeted != targeted) {
            this.targeted = targeted;
            this.repaint(); // <-- Instantly schedules an isolated pixel update!
        }
    }

    public void setHighlighted(boolean highlighted) {
        if (this.highlighted != highlighted) {
            this.highlighted = highlighted;
            this.repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        // 1. Let the native pipeline handle the default flat background square fill
        super.paintComponent(g); 
        
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 2. Render web-style target overlays based on active state properties
            if (targeted) {
                // Draw a sleek, modern, semi-transparent center dot for a valid move
                g2.setColor(new Color(34, 197, 94, 180)); // Velvet Emerald Green tint
                int radius = getWidth() / 4;
                int center = getWidth() / 2;
                g2.fillOval(center - (radius / 2), center - (radius / 2), radius, radius);
            } 
                
            if (highlighted) {
                // If it's the last moved square or a special check context, wash it in a soft glow
                g2.setColor(HIGHLIGHT); // Sleek translucent blue
                g2.fillRect(0, 0, getWidth(), getHeight());
            }

            // 3. High-Craft Debugging Overlay: Render Rank & File
            boolean debug = false;
            if (debug) {
                // Using a muted, semi-transparent gray so it's legible on both light and dark squares
                g2.setColor(Color.MAGENTA); //(new Color(128, 128, 128, 140)); 
                g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
                
                // Format as a clean matrix coordinate, e.g., "(2,3)"
                String debugText = String.format("(%d,%d)", this.file, this.rank);
                
                // Draw in the upper-left corner with a small 6px padding cushion
                g2.drawString(debugText, 6, 16);
            }
            
        } finally {
            g2.dispose();
        }
    }
//    protected void paintComponent(Graphics g) {
//        // 1. Let the native pipeline handle the default flat background square fill
//        super.paintComponent(g); 
//        
//        Graphics2D g2 = (Graphics2D) g.create();
//        try {
//            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//            // 2. Render web-style target overlays based on active state properties
//            if (targeted) {
//                // Draw a sleek, modern, semi-transparent center dot for a valid move
//                g2.setColor(new Color(34, 197, 94, 180)); // Velvet Emerald Green tint
//                int radius = getWidth() / 4;
//                int center = getWidth() / 2;
//                g2.fillOval(center - (radius / 2), center - (radius / 2), radius, radius);
//                
//            } // else 
//                
//            if (highlighted) {
//                // If it's the last moved square or a special check context, wash it in a soft glow
//                g2.setColor(new Color(59, 130, 246, 60)); // Sleek translucent blue
//                g2.fillRect(0, 0, getWidth(), getHeight());
//            }
//            
//        } finally {
//            g2.dispose();
//        }
//    }

}
