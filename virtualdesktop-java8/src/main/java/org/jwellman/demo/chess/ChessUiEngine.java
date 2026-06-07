package org.jwellman.demo.chess;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.LayoutManager2;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

/**
 * A High-Craft Architectural Proof of Concept demonstrating a domain-specific
 * LayoutManager operating directly on a JLayeredPane to manage spatial tracking,
 * zero-allocation coordinate snapping, and high-altitude z-stack drag-and-drop.
 */
public class ChessUiEngine {

    public static void main(String[] args) {

        // Set up the frame environment
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Foundation ChessUiEngine Prototype");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Create the board wrapper to maintain a perfect square aspect ratio
            JPanel boardWrapper = new JPanel(new GridBagLayout());
            boardWrapper.setBackground(new Color(180, 180, 180));

            @SuppressWarnings("serial")
            JLayeredPane chessBoard = new JLayeredPane() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    try {
                        int squareSize = getWidth() / 8;
                        Color lightSquare = new Color(240, 217, 181); // Classic wood tones
                        Color darkSquare  = new Color(181, 136, 99);

                        for (int file = 0; file < 8; file++) {
                            for (int rank = 0; rank < 8; rank++) {
                                // Alternating checkerboard math
                                g2.setColor(((file + rank) % 2 == 0) ? darkSquare : lightSquare);
                                g2.fillRect(file * squareSize, rank * squareSize, squareSize, squareSize);
                            }
                        }
                    } finally {
                        g2.dispose();
                    }
                }
            };
            chessBoard.setPreferredSize(new Dimension(640, 640));
            chessBoard.setBackground(new Color(220, 220, 220));

            // Apply the domain-specific layout manager straight to the layered pane!
            ChessBoardLayout layout = new ChessBoardLayout();
            chessBoard.setLayout(layout);

            // Populate some mock pieces using algebraic Points (x=File, y=Rank)
            // 0=A, 1=B, 2=C, 3=D, 4=E, 5=F, 6=G, 7=H
            // 0=1st Rank, 7=8th Rank
            
            // White Pieces (Bottom)
            chessBoard.add(new ChessPieceToken("R", Color.WHITE), new Point(0, 0));
            chessBoard.add(new ChessPieceToken("N", Color.WHITE), new Point(1, 0));
            chessBoard.add(new ChessPieceToken("B", Color.WHITE), new Point(2, 0));
            chessBoard.add(new ChessPieceToken("K", Color.WHITE), new Point(4, 0));
            chessBoard.add(new ChessPieceToken("P", Color.WHITE), new Point(4, 1));
            
            // Black Pieces (Top)
            chessBoard.add(new ChessPieceToken("k", Color.DARK_GRAY), new Point(4, 7));
            chessBoard.add(new ChessPieceToken("p", Color.DARK_GRAY), new Point(4, 6));
            chessBoard.add(new ChessPieceToken("n", Color.DARK_GRAY), new Point(6, 7));

            boardWrapper.add(chessBoard);
            frame.add(boardWrapper, BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // --- Domain Custom Layout Manager ---
    public static class ChessBoardLayout implements LayoutManager2 {
        private final Map<Component, Point> coordMap = new HashMap<>();

        @Override
        public void addLayoutComponent(Component comp, Object constraints) {
            if (constraints instanceof Point) {
                coordMap.put(comp, new Point((Point) constraints));
            }
        }

        @Override
        public void layoutContainer(Container parent) {
            synchronized (parent.getTreeLock()) {
                JLayeredPane board = (JLayeredPane) parent;
                int boardSize = Math.min(board.getWidth(), board.getHeight());
                int squareSize = boardSize / 8;

                // First Pass: Paint the standard alternating checkerboard background canvas layout
                // Using a background panel or direct component layout could go here, 
                // but we will keep this manager purely focused on the piece layout geometry.

                for (Component comp : board.getComponents()) {
                    // CRITICAL RULE: If the piece has taken off into the flying drag layer,
                    // instantly bail out and let the absolute mouse system govern its pixels!
                    if (board.getLayer(comp) == JLayeredPane.DRAG_LAYER) {
                        continue;
                    }

                    Point coord = coordMap.get(comp);
                    if (coord != null) {
                        int x = coord.x * squareSize;
                        // Invert rank coordinate because Swing (0,0) is top-left, 
                        // while chess rank 0 (1st rank) is bottom-left.
                        int y = (7 - coord.y) * squareSize;

                        comp.setBounds(x, y, squareSize, squareSize);
                    }
                }
            }
        }

        public void updateCoordinate(Component comp, Point newCoord) {
            coordMap.put(comp, new Point(newCoord));
        }

        public Point getCoordinate(Component comp) {
            return coordMap.get(comp);
        }

        @Override public void removeLayoutComponent(Component comp) { coordMap.remove(comp); }
        @Override public Dimension preferredLayoutSize(Container p) { return new Dimension(400, 400); }
        @Override public Dimension minimumLayoutSize(Container p) { return new Dimension(200, 200); }
        @Override public Dimension maximumLayoutSize(Container p) { return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE); }
        @Override public void addLayoutComponent(String name, Component comp) {}
        @Override public void invalidateLayout(Container target) {}
        @Override public float getLayoutAlignmentX(Container target) { return 0.5f; }
        @Override public float getLayoutAlignmentY(Container target) { return 0.5f; }
    }

    // --- Interactive Shared Flight Controller (Zero-Allocation Mouse Handler) ---
    public static class PieceFlightController extends MouseAdapter {
        public static final PieceFlightController INSTANCE = new PieceFlightController();

        private Point dragStartOffset = null;
        private Point logicalOriginPoint = null;

        private PieceFlightController() {}

        @Override
        public void mousePressed(MouseEvent e) {
            if (!(e.getSource() instanceof JComponent)) return;
            JComponent piece = (JComponent) e.getSource();
            JLayeredPane board = (JLayeredPane) piece.getParent();
            ChessBoardLayout layout = (ChessBoardLayout) board.getLayout();

            // Cache the original anchor coordinate in case the move is canceled
            logicalOriginPoint = layout.getCoordinate(piece);
            dragStartOffset = e.getPoint();

            // 1. POP INTO ATMOSPHERE: Lift the piece to the absolute highest layer
            board.setLayer(piece, JLayeredPane.DRAG_LAYER);
            board.moveToFront(piece);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (dragStartOffset == null || !(e.getSource() instanceof JComponent)) return;
            JComponent piece = (JComponent) e.getSource();

            // Translate native cursor deltas straight to raw layout pixels
            int newX = piece.getX() + e.getX() - dragStartOffset.x;
            int newY = piece.getY() + e.getY() - dragStartOffset.y;

            piece.setLocation(newX, newY);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (!(e.getSource() instanceof JComponent)) return;
            JComponent piece = (JComponent) e.getSource();
            JLayeredPane board = (JLayeredPane) piece.getParent();
            ChessBoardLayout layout = (ChessBoardLayout) board.getLayout();

            int squareSize = board.getWidth() / 8;

            // Compute center point of the token to find the targeted square midpoint
            int centerX = piece.getX() + (piece.getWidth() / 2);
            int centerY = piece.getY() + (piece.getHeight() / 2);

            int targetFile = centerX / squareSize;
            int targetRank = 7 - (centerY / squareSize); // Flip the layout math back to chess orientation

            // 2. BOUNDARY DEFENSE: Verify the drop is inside the actual 8x8 matrix boundaries
            if (targetFile >= 0 && targetFile < 8 && targetRank >= 0 && targetRank < 8) {
                layout.updateCoordinate(piece, new Point(targetFile, targetRank));
            } else {
                // Return safely back to where it was picked up
                layout.updateCoordinate(piece, logicalOriginPoint);
            }

            // 3. DROP TO BEDROCK: Land the piece back down and re-assert layout control
            board.setLayer(piece, JLayeredPane.DEFAULT_LAYER);
            board.revalidate(); 
            board.repaint();

            dragStartOffset = null;
            logicalOriginPoint = null;
        }
    }

    // --- The Visual Token Primitive ---
    // Inherits JToggleButton to capture focus, rollover hooks, and interaction states for free!
    @SuppressWarnings("serial")
    public static class ChessPieceToken extends JToggleButton {
        private final String glyph;
        private final Color pieceColor;

        public ChessPieceToken(String glyph, Color pieceColor) {
            this.glyph = glyph;
            this.pieceColor = pieceColor;

            // Strip default look-and-feel painting pipelines
            this.setContentAreaFilled(false);
            this.setBorderPainted(false);
            this.setFocusPainted(false);
            this.setRolloverEnabled(true);

            // Bind to the zero-allocation global flight singleton
            this.addMouseListener(PieceFlightController.INSTANCE);
            this.addMouseMotionListener(PieceFlightController.INSTANCE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int pad = 6;
                int size = getWidth() - (pad * 2);

                // Check z-stack positioning via the LayeredPane hierarchy state
                JLayeredPane lp = (JLayeredPane) getParent();
                boolean isFlying = (lp != null && lp.getLayer((Component)this) == JLayeredPane.DRAG_LAYER);

                // Render dynamic vector dropshadow if flying in upper atmosphere!
                if (isFlying) {
                    g2.setColor(new Color(0, 0, 0, 60));
                    g2.fillOval(pad + 4, pad + 8, size, size);
                }

                // Smoothly color transition based on hover/selection interaction states
                if (getModel().isRollover()) {
                    g2.setColor(new Color(240, 240, 240, 40));
                    g2.fillOval(pad - 2, pad - 2, size + 4, size + 4);
                }

                // Render Base Body
                g2.setColor(pieceColor == Color.WHITE ? new Color(245, 245, 240) : new Color(50, 50, 50));
                g2.fillOval(pad, pad, size, size);

                // Render Rim Trim
                g2.setStroke(new BasicStroke(2.0f));
                g2.setColor(pieceColor == Color.WHITE ? new Color(160, 160, 150) : new Color(20, 20, 20));
                g2.drawOval(pad, pad, size, size);

                // Draw Text Glyph Character Symbol centered inside token
                g2.setFont(new Font("SansSerif", Font.BOLD, getWidth() / 3));
                g2.setColor(pieceColor == Color.WHITE ? Color.BLACK : Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(glyph)) / 2;
                int ty = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(glyph, tx, ty);

            } finally {
                g2.dispose();
            }
        }
    }

}
