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
import java.util.Stack;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

/**
 * Demonstrate a domain-specific LayoutManager operating directly on 
 * a JLayeredPane to manage spatial tracking, zero-allocation coordinate snapping, 
 * and high-altitude z-stack drag-and-drop.
 */
public class ChessUiEngine {

    private static final int BOARD_BORDER_SIZE = 40;

    private ChessGame game; // model
    private JLayeredPane chessBoard; // board view
    private MovesScoresheetPanel scoresheetPanel; // game view / interactions
    private PieceFlightController mouseController; // controller

    // Support undo/redo
    private final Stack<MoveEvent> undoStack = new Stack<>();
    private final Stack<MoveEvent> redoStack = new Stack<>();

    // Board Squares - they only know how to draw a square and whether they are targeted or highlighted.
    private static final BoardSquare[][] boardSquareMatrix = new BoardSquare[8][8];

    // Inside your BoardView / BoardController layer, NOT the domain
    private static final Map<ChessPiece, ChessPieceToken> viewTokens = new HashMap<>();

    @SuppressWarnings("serial")
    public ChessUiEngine() {
        game = new ChessGame();
        mouseController = new PieceFlightController(this);
        scoresheetPanel = new MovesScoresheetPanel();
        chessBoard = new JLayeredPane() {
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

    }

    public static void main(String[] args) {
        
        ChessUiEngine engine = new ChessUiEngine();

        // Set up the frame environment
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Foundation ChessUiEngine Prototype");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Create the board wrapper to maintain a perfect square aspect ratio
            JPanel boardWrapper = new JPanel(new GridBagLayout());
            boardWrapper.setBackground(new Color(180, 180, 180));

            JLayeredPane chessBoard = engine.chessBoard;
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
                    Color bg = ((file + rank) % 2 == 0) ? darkSquare : lightSquare;
                    BoardSquare square = new BoardSquare(engine.game, bg, rank, file);
                    boardBackground.add(square);
                    boardSquareMatrix[file][rank] = square;
                }
            }

            Map<Point, ChessPiece> activePieces = engine.game.getActivePieces();
            for (Point p : activePieces.keySet()) {
                ChessPiece piece = activePieces.get(p);
                ChessPieceToken token = new ChessPieceToken( piece, engine.mouseController);
                chessBoard.add(token, piece.getPosition());
                ChessUiEngine.viewTokens.put(piece, token);
            }

            // 2. Lock the background panel to the absolute bottom of your JLayeredPane
            chessBoard.add(boardBackground, JLayeredPane.DEFAULT_LAYER);

            // Force it to fill the entire board canvas via your layout manager
            boardBackground.setBounds(0, 0, 640, 640);
            boardBackground.setBorder(new ChessGutterBorder(BOARD_BORDER_SIZE, engine.game));

            boardWrapper.add(chessBoard);

            frame.add(boardWrapper, BorderLayout.CENTER);
            frame.add(engine.createControlPanel(), BorderLayout.EAST);
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
                // This appears to no longer be necessary due to evolution of the app
                // ChessPieceToken t = (ChessPieceToken)comp;
                // t.getPiece().setPosition(newCoord);
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
    public class PieceFlightController extends MouseAdapter {

        // Only one piece can be dragged at a time so despite this being a shared
        // controller, there will be no conflicts piece to piece.
        private Point dragStartOffset = null;
        private Point logicalOriginPoint = null;
        private List<Point> validDestinations;
        private ChessUiEngine engine;

        private PieceFlightController(ChessUiEngine e) {
            this.engine = e;
        }

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
            List<Point> validCaptures = game.getValidator().getValidMoves(game, ((ChessPieceToken)piece).getPiece(),
                    ChessMoveValidator.EvaluationContext.CONTROL, null);
            // System.out.println("Valid Captures: " + validCaptures);

            validDestinations = game.getValidator().getValidMoves(game, ((ChessPieceToken)piece).getPiece(),
                    ChessMoveValidator.EvaluationContext.MOVEMENT, null);
            validDestinations.addAll(validCaptures);

            // 2. Direct index routing—zero searching required!
            for (Point targetPoint : validDestinations) {
                BoardSquare square = boardSquareMatrix[targetPoint.x][targetPoint.y];
                square.setTargeted(true);
            }

            // Cache the original anchor coordinate in case the move is canceled
            dragStartOffset = e.getPoint();
            logicalOriginPoint = layout.getCoordinate(piece);

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
            ChessPieceToken piece = (ChessPieceToken) e.getSource();
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
            Point droppedAt = new Point(targetFile, targetRank);
            boolean onboard = targetFile >= 0 && targetFile < 8 && targetRank >= 0 && targetRank < 8;
            if (onboard && validDestinations.contains(droppedAt)) {
                // cache this value because submitMove will alter it
                boolean hasNotMoved = piece.getPiece().hasNotMoved();
                MoveAnalysis result = game.submitMove(logicalOriginPoint, droppedAt);
                if (result.isAccepted()) {
                    layout.updateCoordinate(piece, droppedAt);
                    if (result.getCapturedPiece().isPresent())
                        board.remove(viewTokens.get(result.getCapturedPiece().get()));

                    boolean wasInitialPawnMove = ChessPiece.Type.PAWN == piece.getPiece().getType() && hasNotMoved;
                    MoveEvent move = new MoveEvent(logicalOriginPoint, droppedAt, 
                            piece.getPiece(), result.getCapturedPiece().orElse(null), 
                            null, wasInitialPawnMove );
                    recordMove(move);
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

            engine.scoresheetPanel.synchronizeHistory(engine.undoStack);

            dragStartOffset = null;
            logicalOriginPoint = null;
        }
    }
    
    public void recordMove(MoveEvent event) {
        undoStack.push(event);
        redoStack.clear(); // A fresh player move ALWAYS wipes out the redo timeline
    }

    public void undoLastMove() {
        if (undoStack.isEmpty()) return;

        final MoveEvent event = undoStack.pop();
        ChessBoardLayout layout = (ChessBoardLayout) chessBoard.getLayout();

        // 1. Teleport the primary piece straight back to its home coordinate
        game.restoreMovedPiece(event);
        ChessPieceToken movedToken = viewTokens.get(event.getMovedPiece());
        layout.updateCoordinate(movedToken, event.getOrigin());

        // 2. Resurrection: If a piece was captured, pop it back into existence!
        game.restoreCapturedPiece(event);

        // 3. Restore the temporal En Passant target window
        game.setEnPassantVulnerableSquare(event.getEnPassantSquareBeforeMove());

        // 4. Push to redo stack so the user can change their mind
        redoStack.push(event);

        // Update the visual scoreboard
        this.scoresheetPanel.synchronizeHistory(this.undoStack);
        
        // 5. Broadcast to the UI layer to clean house
        // --- THE SWING CACHE SMASHER ---
        // This turned out to be un-necessary but leaving in case it becomes necessary later
        //chessBoard.invalidate(); // Forcefully declare the entire container hierarchy invalid
        //chessBoard.doLayout();   // Tell the layout manager to immediately reposition children based on the new coords
        chessBoard.revalidate();
        chessBoard.repaint();
    }

    public JPanel createControlPanel() {
        Border b = new CompoundBorder(
                new EmptyBorder(0, 4, 4, 4),
                new TitledBorder("Control Panel")
                );
        
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(b);

        JPanel south = new JPanel();
        controlPanel.add(south, BorderLayout.SOUTH);

        JButton btn = new JButton("Undo");
        south.add(btn);
        btn.addActionListener(e -> {
            this.undoLastMove();
        });
        btn = new JButton("Redo");
        south.add(btn);

        controlPanel.add(scoresheetPanel, BorderLayout.CENTER);

        return controlPanel;
    }

}
