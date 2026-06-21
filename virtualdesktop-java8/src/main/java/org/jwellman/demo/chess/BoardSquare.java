package org.jwellman.demo.chess;

import javax.swing.JPanel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;

@SuppressWarnings("serial")
public class BoardSquare extends JPanel {

    private ChessGame game;
    private Color baseColor;
    private boolean targeted = false;
    private boolean highlighted = false;
    private int rank;
    private int file;

    // Modern tactical tones TODO make themeable
    private static Color lightSquare = new Color(235, 236, 208); 
    private static Color darkSquare  = new Color(119, 149, 86);

    // Options
    public static boolean drawControlBadges = false;
    public static boolean drawControlIndicators = true;

    private static final Color HIGHLIGHT = new Color(59, 130, 246, 60);
    private static final Stroke STROKE_TARGETTED = new BasicStroke(4.0f);

    public BoardSquare(ChessGame g, Color baseColor, int rank, int file) {
        this.baseColor = baseColor;
        this.setOpaque(true); // Let Swing optimize background clearing
        this.setBackground(this.baseColor);
        this.rank = rank;
        this.file = file;
        this.game = g;
    }

    /**
     * Returns true if this square represents a light (white) tile,
     * or false if it represents a dark (black) tile.
     */
    public boolean isLightSquare() {
        // If the sum of file and rank is odd, it's a light square
        return (this.file + this.rank) % 2 != 0;
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

        // Paints the underlying tile background
        super.paintComponent(g); 

        final Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 2. Render web-style target overlays based on active state properties
            if (targeted) {
                // Peek ahead to see if a piece component is currently sitting on top of us
                boolean holdsPiece = (this.getComponentCount() > 0) || game.hasPieceAt(this.file, this.rank);

                if (holdsPiece) {
                    // TARGET CAPTURE: Draw a thick, gorgeous outer ring framing the square
                    // g2.setColor(new Color(34, 197, 94, 180)); // Velvet Emerald Green tint
                    g2.setColor(new Color(180, 180, 180, 180)); 
                    g2.setStroke(STROKE_TARGETTED); // Thick vector ring

                    // Inset it by 3px so it sits cleanly inside the square margins
                    g2.drawOval(3, 3, getWidth() - 6, getHeight() - 6);
                } else {
                    // STANDARD MOVE: Draw the clean, modern center dot
                    // g2.setColor(new Color(34, 197, 94, 180)); // Velvet Emerald Green tint
                    g2.setColor(new Color(90, 90, 90, 180)); 
                    int radius = getWidth() / 4;
                    int center = getWidth() / 2;
                    g2.fillOval(center - (radius / 2), center - (radius / 2), radius, radius);
                }
            }

            if (highlighted) {
                // If it's the last moved square or a special check context, wash it in a soft glow
                g2.setColor(HIGHLIGHT); // Sleek translucent blue
                g2.fillRect(0, 0, getWidth(), getHeight());
            }

            if (drawControlIndicators) {
                // Reach into the fast, pre-calculated engine cache
                final SquareControlMatrix matrix = game.getControlMatrix();
                final int whiteAttackers = matrix.getWhiteAttackerCount(this.file, this.rank);
                final int blackAttackers = matrix.getBlackAttackerCount(this.file, this.rank);

                // --- Control Indicator ---
                int barHeight = 12;
                int yOffset = getHeight() - barHeight;

                // Determine semantic color based on majority rules
                if (whiteAttackers > 0 || blackAttackers > 0) {

                    // "normalize" the background between light and dark for visual consistency
//                    if (isLightSquare()) {
//                    }
                      g2.setColor(darkSquare);
//                    if (whiteAttackers > blackAttackers) {
//                        g2.setColor(lightSquare);
//                    } else if (blackAttackers > whiteAttackers) {
//                        g2.setColor(darkSquare);
//                    } else {
//                        g2.setColor(Color.white);  // Blue for contested ties
//                    }
                    // Paint the clean control anchor strip across the bottom floorboard
                    g2.fillRect(0, yOffset, getWidth(), barHeight);

                    // paint the translucent color - old
//                    if (whiteAttackers > blackAttackers) {
//                        if (isLightSquare()) {
//                            g2.setColor(new Color(240, 255, 240, 200)); // Clean, luminous white-blue
////                            g2.setColor(new Color(240, 240, 245, 128)); // Clean, luminous white-blue
//                        } else {
//                            g2.setColor(new Color(240, 240, 245, 128)); // Clean, luminous white-blue
//                        }
//                    } else if (blackAttackers > whiteAttackers) {
//                        if (isLightSquare()) {
//                            g2.setColor(new Color(30, 35, 30, 80));   // Deep, tactical dark charcoal
//                        } else {
//                            g2.setColor(new Color(30, 30, 30, 16));   // Deep, tactical dark charcoal
//                        }
//                    } else {
//                        g2.setColor(new Color(0, 0, 128, 128));  // Mid-tone slate grey for contested ties
//                    }
//                    // Paint the clean control anchor strip across the bottom floorboard
//                    g2.fillRect(0, yOffset, getWidth(), barHeight);

                    
                    // paint the translucent color - new
                    if (whiteAttackers > blackAttackers) {
                        g2.setColor(new Color(240, 240, 245, 128)); // Clean, luminous white-blue
                    } else if (blackAttackers > whiteAttackers) {
                        g2.setColor(new Color(30, 30, 30, 64));   // Deep, tactical dark charcoal
                    } else {
                        g2.setColor(new Color(0, 0, 128, 128));  // Blue for contested ties
                    }
                    // Paint the clean control anchor strip across the bottom floorboard
                    g2.fillRect(0, yOffset, getWidth(), barHeight);
                }

                if (drawControlBadges) {
                    final int badgeRadius = 14; // Diameter of the control pill
                    final int margin = 4;       // Inset distance from the square's edges

                    // Upper positions become lower positions:
                    final int lowerY = getHeight() - badgeRadius - margin; 

                    // White lower left: (margin, lowerY)
                    // Black lower right: (getWidth() - badgeRadius - margin, lowerY)

                    // 1. Draw White Control Badge (Upper Left)
                    if (whiteAttackers > 0) {
                        final int x = margin;
                        final int y = lowerY; // margin;
                        
                        g2.setColor(Color.WHITE);
                        g2.fillOval(x, y, badgeRadius, badgeRadius);
                        
                        g2.setColor(Color.BLACK);
                        drawCenteredString(g2, String.valueOf(whiteAttackers), x, y, badgeRadius, badgeRadius);
                    }

                    // 2. Draw Black Control Badge (Upper Right)
                    if (blackAttackers > 0) {
                        final int x = getWidth() - badgeRadius - margin;
                        final int y = lowerY; // margin;

                        g2.setColor(Color.BLACK);
                        g2.fillOval(x, y, badgeRadius, badgeRadius);
                        
                        g2.setColor(Color.WHITE);
                        drawCenteredString(g2, String.valueOf(blackAttackers), x, y, badgeRadius, badgeRadius);
                    }
                }

            }

        } finally {
            g2.dispose(); // Guarantee disposal to prevent graphic context memory leaks
        }
    }

    /**
     * Aligns and renders a text string directly into the center of a defined geometric bounding box.
     */
    private void drawCenteredString(Graphics2D g2, String text, int x, int y, int width, int height) {
        final FontMetrics metrics = g2.getFontMetrics(g2.getFont());

        // Determine the x coordinate for the text
        final int textX = x + (width - metrics.stringWidth(text)) / 2;

        // Determine the y coordinate for the text (utilizing the font's ascent for
        // perfect baseline leveling)
        final int textY = y + ((height - metrics.getHeight()) / 2) + metrics.getAscent();

        g2.drawString(text, textX, textY);
    }

    protected void paintComponent_old(Graphics g) {
        // 1. Let the native pipeline handle the default flat background square fill
        super.paintComponent(g); 
        
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 2. Render web-style target overlays based on active state properties
            if (targeted) {
                // Peek ahead to see if a piece component is currently sitting on top of us
                boolean holdsPiece = (this.getComponentCount() > 0) || game.hasPieceAt(this.file, this.rank);

                if (holdsPiece) {
                    // TARGET CAPTURE: Draw a thick, gorgeous outer ring framing the square
                    // g2.setColor(new Color(34, 197, 94, 180)); // Velvet Emerald Green tint
                    g2.setColor(new Color(180, 180, 180, 180)); 
                    g2.setStroke(STROKE_TARGETTED); // Thick vector ring

                    // Inset it by 3px so it sits cleanly inside the square margins
                    g2.drawOval(3, 3, getWidth() - 6, getHeight() - 6);
                } else {
                    // STANDARD MOVE: Draw the clean, modern center dot
                    // g2.setColor(new Color(34, 197, 94, 180)); // Velvet Emerald Green tint
                    g2.setColor(new Color(180, 180, 180, 180)); 
                    int radius = getWidth() / 4;
                    int center = getWidth() / 2;
                    g2.fillOval(center - (radius / 2), center - (radius / 2), radius, radius);
                }
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

}
