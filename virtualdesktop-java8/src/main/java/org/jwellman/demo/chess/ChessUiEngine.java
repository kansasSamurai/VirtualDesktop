package org.jwellman.demo.chess;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager2;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * An architectural Proof of Concept demonstrating a domain-specific
 * LayoutManager operating directly on a JLayeredPane to manage spatial tracking,
 * zero-allocation coordinate snapping, and high-altitude z-stack drag-and-drop.
 */
public class ChessUiEngine {

    private static final int BOARD_BORDER_SIZE = 40;

    // Board Squares - they only know how to draw a square and whether they are targeted or highlighted.
    private static final BoardSquare[][] boardSquareMatrix = new BoardSquare[8][8];

    private static final ChessGame game = new ChessGame();

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
            chessBoard.setLayout(new ChessBoardLayout());



            Color lightSquare = new Color(235, 236, 208); // Modern tactical tones
            Color darkSquare  = new Color(119, 149, 86);

            // 1. Create a structural grid for the squares
            // files are vertical columns, ranks are rows
            JPanel boardBackground = new JPanel(new GridLayout(8, 8));
            for (int rank = 7; rank > -1; rank--) {
                for (int file = 0; file < 8; file++) {
                    BoardSquare square = new BoardSquare(((file + rank) % 2 == 0) ? lightSquare : darkSquare, rank, file);
                    boardBackground.add(square);
                    boardSquareMatrix[file][rank] = square;
                }
            }

            Map<Point, ChessPiece> activePieces = game.getActivePieces();
            for (Point p : activePieces.keySet()) {
                ChessPiece piece = activePieces.get(p);
                chessBoard.add(new ChessPieceToken(piece, piece.isWhite() ? Color.WHITE : Color.DARK_GRAY), piece.getPosition());
            }

            // 2. Lock the background panel to the absolute bottom of your JLayeredPane
            chessBoard.add(boardBackground, JLayeredPane.DEFAULT_LAYER);

            // Force it to fill the entire board canvas via your layout manager
            boardBackground.setBounds(0, 0, 640, 640);
            boardBackground.setBorder(new ChessGutterBorder(BOARD_BORDER_SIZE, game));

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
                int boardSize = Math.min(board.getWidth(), board.getHeight()) - (BOARD_BORDER_SIZE*2);
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
                        int x = coord.x * squareSize + BOARD_BORDER_SIZE;
                        // Invert rank coordinate because Swing (0,0) is top-left, 
                        // while chess rank 0 (1st rank) is bottom-left.
                        int y = (7 - coord.y) * squareSize;
                        y += BOARD_BORDER_SIZE;

                        comp.setBounds(x, y, squareSize, squareSize);
                    }
                }
            }
        }

        public void updateCoordinate(Component comp, Point newCoord) {
            coordMap.put(comp, newCoord);
            if (comp instanceof ChessPieceToken) {
                ChessPieceToken t = (ChessPieceToken)comp;
                t.piece.setPosition(newCoord);
            }
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

        // Only one piece can be dragged at a time so despite this being a shared
        // controller, there will be no conflicts piece to piece.
        private Point dragStartOffset = null;
        private Point logicalOriginPoint = null;
        private List<Point> validDestinations;

        private PieceFlightController() {}

        @Override
        public void mousePressed(MouseEvent e) {
            if (!(e.getSource() instanceof JComponent)) return;
            JComponent piece = (JComponent) e.getSource();
            JLayeredPane board = (JLayeredPane) piece.getParent();
            ChessBoardLayout layout = (ChessBoardLayout) board.getLayout();

            /* On Lift (`mousePressed`):** 
             * Your coordinator runs the fast vector raycasting math we discussed 
             * to get a list of valid destination `Point` coordinates. 
             * It loops through the target squares at those points and 
             * calls `square.setTargeted(true)`
             * 
             */
            // 1. Get your targeted points from the validator
            validDestinations = game.getValidator().getValidMoves(game, ((ChessPieceToken)piece).piece);

            // 2. Direct index routing—zero searching required!
            for (Point targetPoint : validDestinations) {
                BoardSquare square = boardSquareMatrix[targetPoint.x][targetPoint.y];
                square.setTargeted(true);
            }

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

            int boardSize = board.getWidth() - (BOARD_BORDER_SIZE*2);
            int squareSize = boardSize / 8;

            // Compute center point of the token to find the targeted square midpoint
            int centerX = piece.getX() + (piece.getWidth() / 2) - BOARD_BORDER_SIZE;
            int centerY = piece.getY() + (piece.getHeight() / 2) - BOARD_BORDER_SIZE;

            int targetFile = centerX / squareSize;
            int targetRank = 7 - (centerY / squareSize); // Flip the layout math back to chess orientation

            // 2. BOUNDARY DEFENSE: Verify the drop is inside the actual 8x8 matrix boundaries
            if (targetFile >= 0 && targetFile < 8 && targetRank >= 0 && targetRank < 8) {
                Point droppedAt = new Point(targetFile, targetRank);

                MoveAnalysis result = game.submitMove(logicalOriginPoint, droppedAt);
                if (result.isAccepted()) {
                    layout.updateCoordinate(piece, droppedAt);
                } else {
                    // Return safely back to where it was picked up
                    layout.updateCoordinate(piece, logicalOriginPoint);
                }
            } else {
                // Return safely back to where it was picked up
                layout.updateCoordinate(piece, logicalOriginPoint);
            }

            // 2a. undecorate destination squares
            for (Point targetPoint : validDestinations) {
                BoardSquare square = boardSquareMatrix[targetPoint.x][targetPoint.y];
                square.setTargeted(false);
                square.setHighlighted(false);
            }

            // 3. DROP TO BEDROCK: Land the piece back down and re-assert layout control
            board.setLayer(piece, 20); // JLayeredPane.DEFAULT_LAYER);
            board.revalidate(); 
            board.repaint();

            dragStartOffset = null;
            logicalOriginPoint = null;
        }
    }
    
}
