package org.jwellman.demo.chess;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jwellman.demo.chess.ChessPiece.Type;
import org.jwellman.demo.chess.MoveAnalysis.ResultType;

/**
 * High-Craft Domain State Machine managing the lifecycle, turn rules, 
 * and piece inventories for a live chess match.
 * 
 * <pre>
    The `private final Map<Point, ChessPiece> activePieces` collection is the absolute **heart and center of gravity** for your game’s data model. It acts as a lightweight, lightning-fast **spatial database** representing the physical reality of the board at any given millisecond.
    Instead of tracking pieces using a heavy, traditional 2D array matrix that you have to constantly loop through, this map flips the script: **The spatial coordinate *is* the lookup key.**
    Here is the precise mental model for why this map structure is so powerful and how it streamlines your entire engine:
    
    ---
    
    ### 1. It acts as an Instant Collision Map
    
    When your vector raycaster is shooting out lines to see where a Rook or Bishop can travel, it doesn't need to scan the board. It simply asks the map:
    
    * *"Is there anything at coordinate `e4`?"* $\rightarrow$ `activePieces.containsKey(new Point(4, 3))`
    * Because hash map lookups run in constant $O(1)$ time, checking for spatial collisions is instantaneous.
    
    ### 2. It unifies Piece Identity and Location
    
    In a traditional design, a piece might store its location internally, and a board array might store the piece pointer externally, creating a risky dual-source-of-truth problem where they can drift out of sync.
    
    * With this map, **the map key *is* the location, and the map value *is* the identity.** * Moving a piece is reduced to a clean atomic transaction: `activePieces.remove(fromPoint)` followed by `activePieces.put(toPoint, piece)`.
    
    ### 3. It dramatically reduces the Search Space
    
    When your inverse raycaster needs to calculate if a King is in check, it needs to find where that King is sitting.
    
    * Instead of searching through an entire 8x8 layout grid or inspecting 32 visual component tokens, you can stream the map entries to immediately find the coordinate key associated with the King piece type.
    
    ### 4. It naturally adapts to Game Progression
    
    As pieces are captured and removed from play, they are simply deleted from this map via `.remove()`.
    
    * As the game enters the endgame phase with only 4 or 5 pieces left on the board, the map shrinks down to just 4 or 5 entries. Your validation routines only evaluate the coordinates that actually contain data, completely bypassing the need to iterate through empty squares.

 * </pre>
 */
public class ChessGame {

    // Authoritative lookup: Where is every piece right now?
    private final Map<Point, ChessPiece> activePieces = new HashMap<>();

    // Captures inventories for the gutter banks
    private final List<ChessPiece> whiteCaptured = new ArrayList<>();
    private final List<ChessPiece> blackCaptured = new ArrayList<>();

    // Who's turn is it?
    private boolean isWhiteTurn = true;

    // Encapsulated validation object
    private final ChessMoveValidator validator = new ChessMoveValidator();

    // Encapsulated square control matrix - initialize empty matrix for start of game
    private SquareControlMatrix currentControlMatrix = new SquareControlMatrix();

    public ChessGame() {
        initializeStandardBoard();
    }

    // --- High-Craft Accessor Methods for the Views ---

    public Map<Point, ChessPiece> getActivePieces() {
        // Return unmodifiable view to prevent external views from mutating state sideways
        return Collections.unmodifiableMap(activePieces);
    }

    public List<ChessPiece> getCapturedPieces(boolean isWhiteBank) {
        return Collections.unmodifiableList(isWhiteBank ? whiteCaptured : blackCaptured);
    }

    public boolean isWhiteTurn() {
        return isWhiteTurn;
    }

    /**
     * The Definitive State Mutation Gatekeeper.
     * 
     * The UI requests a move; the Game determines if it's allowed.
     */
    public MoveAnalysis submitMove(Point from, Point to) {
        ChessPiece piece = activePieces.get(from);

        // 1. Guard Defense
        if (piece == null) {
            return new MoveAnalysis(ResultType.REJECTED_ILLEGAL_GEOMETRY, false, null, "");
        }
        if (piece.isWhite() != isWhiteTurn) {
            return new MoveAnalysis(ResultType.REJECTED_OUT_OF_TURN, false, null, "");
        }
        // Make sure the piece moved
        if (to.equals(from)) {
            return new MoveAnalysis(ResultType.REJECTED_DID_NOT_MOVE, false, null, "");
        }

        // 2. Validate Geometry (Hand off to your vector map)
        if (validator.moveIsNotLegal(this.getCollisionMap(), piece, to)) {
            return new MoveAnalysis(ResultType.REJECTED_ILLEGAL_GEOMETRY, false, null, "");
        }

        // 3. Dry-run Check Validation (Ensure this move doesn't expose our King)
        if (validator.exposesKingToCheck(this, from, to)) {
            return new MoveAnalysis(ResultType.REJECTED_SELF_CHECK, false, null, "");
        }

        // 4. ATOMIC MUTATION: Commit the change to the spatial index map
        ChessPiece target = activePieces.remove(to); // Captures null if empty
        activePieces.remove(from);
        piece.setPosition(to);
        activePieces.put(to, piece);

        {
            // 1. Commit the move to your layout map...
            activePieces.put(to, piece);

            // 2. REGENERATE THE HEATMAP (Runs once per move, taking less than a millisecond)
            this.generateMatrix(this.activePieces);

            // 3. Broadcast to UI - ChessUIEngine/mouseRelease will automatically repaint
            // this.boardBackground.repaint();
        }

        // 5. Compute Side-Effects
        boolean opponentInCheck = validator.isKingInCheck(this.getCollisionMap(), !isWhiteTurn);
        ResultType outcome = (target != null) ? ResultType.SUCCESS_CAPTURE : ResultType.SUCCESS_STANDARD;

        // Check for future promotion hook
        if (piece.getType() == ChessPiece.Type.PAWN && (to.y == 0 || to.y == 7)) {
            outcome = ResultType.SUCCESS_PROMOTION_REQUIRED;
        }

        // 6. Flip internal turn clock
        isWhiteTurn = !isWhiteTurn;

        // Generate clean console logging string
        String logText = "logtext tbd"; // formatAlgebraicNotation(piece, to, target != null);

        return new MoveAnalysis(outcome, opponentInCheck, target, logText);
    }

    public SquareControlMatrix getControlMatrix() {
        return this.currentControlMatrix;
    }

    private void generateMatrix(Map<Point, ChessPiece> activePieces2) {
        this.currentControlMatrix = new SquareControlMatrix();
        
        // TODO implement loop/logic
        for (final ChessPiece cursorPiece : this.activePieces.values() ) {
            getValidator().getValidMoves(this, cursorPiece);
        }

    }

    /**
     * Populate activePieces with starting 32 tokens at standard Point keys
     * In your final system, your interactive ChessPieceToken swing component 
     * simply holds a reference to its backend ChessPiece domain object.
     */
    private void initializeStandardBoard() {

        // Black Pieces (Top)
        ArrayList<ChessPiece> black = new ArrayList<>();
        black.add(new ChessPiece(Type.ROOK, false, new Point(0, 7)));
        black.add(new ChessPiece(Type.KNIGHT, false, new Point(1, 7)));
        black.add(new ChessPiece(Type.BISHOP, false, new Point(2, 7)));
        black.add(new ChessPiece(Type.QUEEN, false, new Point(3, 7)));
        black.add(new ChessPiece(Type.KING, false, new Point(4, 7)));
        black.add(new ChessPiece(Type.BISHOP, false, new Point(5, 7)));
        black.add(new ChessPiece(Type.KNIGHT, false, new Point(6, 7)));
        black.add(new ChessPiece(Type.ROOK, false, new Point(7, 7)));
        for (int file=0; file<8; file++) {
            black.add(new ChessPiece(Type.PAWN, false, new Point(file, 6)));
        }
        for (ChessPiece apiece : black) {
            activePieces.put(apiece.getPosition(), apiece);
        }

        // White Pieces (Bottom)
        ArrayList<ChessPiece> white = new ArrayList<>();
        white.add(new ChessPiece(Type.ROOK, true, new Point(0, 0)));
        white.add(new ChessPiece(Type.KNIGHT, true, new Point(1, 0)));
        white.add(new ChessPiece(Type.BISHOP, true, new Point(2, 0)));
        white.add(new ChessPiece(Type.QUEEN, true, new Point(3, 0)));
        white.add(new ChessPiece(Type.KING, true, new Point(4, 0)));
        white.add(new ChessPiece(Type.BISHOP, true, new Point(5, 0)));
        white.add(new ChessPiece(Type.KNIGHT, true, new Point(6, 0)));
        white.add(new ChessPiece(Type.ROOK, true, new Point(7, 0)));
        for (int file=0; file<8; file++) {
            white.add(new ChessPiece(Type.PAWN, true, new Point(file, 1)));
        }
        for (ChessPiece apiece : white) {
            activePieces.put(apiece.getPosition(), apiece);
        }


    }

    /**
     * Generates an instantaneous, immutable spatial view of the active board position.
     */
    public CollisionMap getCollisionMap() {
        return new CollisionMap() {
            
            // Note: Reusing a mutable lookupBuffer internally prevents your engine 
            // from churning out thousands of short-lived Point allocations on 
            // the heap during rapid raycasting passes!
            private final Point lookupBuffer = new Point();

            @Override
            public boolean isEmptyAt(int x, int y) {
                lookupBuffer.setLocation(x, y);
                return !activePieces.containsKey(lookupBuffer);
            }

            @Override
            public boolean isOccupiedByColor(int x, int y, boolean isWhite) {
                lookupBuffer.setLocation(x, y);
                ChessPiece piece = activePieces.get(lookupBuffer);
                return piece != null && piece.isWhite() == isWhite;
            }

        };
    }

    protected ChessMoveValidator getValidator() {
        return validator;
    }

    public ChessPiece getPieceAt(Point targetSquare) {
        return this.getActivePieces().get(targetSquare);
    }

}
