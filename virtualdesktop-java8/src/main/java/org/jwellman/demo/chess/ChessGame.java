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

    // Authoritative lookup: Where is every active piece right now?
    private final Map<Point, ChessPiece> activePieces = new HashMap<>();

    // Captures inventories for the gutter banks
    private final List<ChessPiece> whiteCaptured = new ArrayList<>();
    private final List<ChessPiece> blackCaptured = new ArrayList<>();

    // The Core King Pointers
    private ChessPiece whiteKing;
    private ChessPiece blackKing;

    // The Four Fixed Rook Anchors for O(1) Castling Audits
    private ChessPiece whiteKingsideRook;
    private ChessPiece whiteQueensideRook;
    private ChessPiece blackKingsideRook;
    private ChessPiece blackQueensideRook;

    // Who's turn is it?
    private boolean isWhiteTurn = true;

    // Support en passant captures
    private Point enPassantVulnerableSquare;

    // Encapsulated validation object
    private final ChessMoveValidator validator = new ChessMoveValidator();

    // Encapsulated square control matrix - initialize empty matrix for start of game
    private SquareControlMatrix currentControlMatrix = new SquareControlMatrix();

    public ChessGame() {
        initializeStandardBoard();
    }

    // --- High-Craft Accessor Methods for the Views ---

    public boolean isWhiteTurn() {
        return isWhiteTurn;
    }

    public Map<Point, ChessPiece> getActivePieces() {
        // Return unmodifiable view to prevent external views from mutating state sideways
        return Collections.unmodifiableMap(activePieces);
    }

    public List<ChessPiece> getCapturedPieces(boolean isWhiteBank) {
        return Collections.unmodifiableList(isWhiteBank ? whiteCaptured : blackCaptured);
    }

    protected ChessMoveValidator getValidator() {
        return validator;
    }

    public ChessPiece getPieceAt(Point targetSquare) {
        return activePieces.get(targetSquare);
    }

    public boolean hasPieceAt(int file, int rank) {
        return this.getPieceAt(new Point(file, rank)) != null;
    }

    public int getMaterialDelta() {
        int whiteValue = 0;
        int blackValue = 0;
        for (ChessPiece piece : activePieces.values()) {
            if (piece.isWhite()) {
                whiteValue += piece.getValue();
            } else {
                blackValue += piece.getValue();
            }
        }
        return whiteValue - blackValue;
    }

    public ChessPiece getKing(boolean white) {
        return white ? whiteKing : blackKing;
    }

    public ChessPiece getRook(boolean white, boolean kingside) {
        if (white) {
            return kingside ? whiteKingsideRook : whiteQueensideRook;
        }
        return kingside ? blackKingsideRook : blackQueensideRook;
    }

    public SquareControlMatrix getControlMatrix() {
        return this.currentControlMatrix;
    }

    /**
     * Retrieves all geometrically valid movement coordinates for a given piece.
     */
    public List<Point> getValidMovementSquares(ChessPiece piece) {
        return this.validator.getValidMoves(this, piece, ChessMoveValidator.EvaluationContext.MOVEMENT, null);
    }

    /**
     * Retrieves all coordinates actively controlled or targeted for attack by a given piece.
     */
    public List<Point> getValidControlSquares(ChessPiece piece) {
        return this.validator.getValidMoves(this, piece, ChessMoveValidator.EvaluationContext.CONTROL, null);
    }

    /** */
    public boolean isPromotionPending(ChessPiece piece, Point destination) {
        if (!piece.isPawn()) {
            return false;
        }
        // White pawn reaches rank 8 (index 7) or Black pawn reaches rank 1 (index 0)
        int promotionRank = piece.isWhite() ? 7 : 0;
        return destination.y == promotionRank;
    }
    
    public ChessPiece executePromotion(ChessPiece piece, Point droppedAt, Type chosenType) {

        // remove the piece/pawn
        activePieces.remove(piece.getPosition());

        // add the promoted piece according to type
        ChessPiece promoted = new ChessPiece(chosenType, piece.isWhite(), droppedAt);
        activePieces.put(droppedAt, promoted);

        return promoted;
    }

    /**
     * The Definitive State Mutation Gatekeeper.
     * 
     * The UI requests a move; the Game determines if it's allowed.
     */
    public MoveAnalysis submitMove(Point from, Point to) {
        ChessPiece piece = activePieces.get(from);
        System.out.printf("Submit move: from %s to %s%n", from, to);

        // 1. Guard Defense
        if (piece == null) {
            return new MoveAnalysis(ResultType.REJECTED_NULL_PIECE, false, null, "");
        }
        System.out.printf("...        : Piece %s %n", piece.getGlyph() );

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

        // 4. Move the piece...
        // ... remove/capture the piece at the target square (can be null if empty)
        ChessPiece capturedPiece = activePieces.remove(to); // Captures null if empty
        // ... remove the moving piece from its current square
        activePieces.remove(from);
        if (capturedPiece != null) {
             // 4b. Execute Capture if a piece occupies the destination
            if (capturedPiece.isWhite()) 
                whiteCaptured.add(capturedPiece);
            else 
                blackCaptured.add(capturedPiece);
        }

        {
            // 1. Commit the move to your layout map...
            activePieces.put(to, piece);
            piece.setPosition(to);

            // 2. REGENERATE THE HEATMAP (Runs once per move, taking less than a millisecond)
            this.generateMatrix(this.activePieces);

            // 3. Broadcast to UI - ChessUIEngine/mouseRelease will automatically repaint
            // this.boardBackground.repaint();
        }

        // 5. Compute Side-Effects
        boolean opponentInCheck = validator.isKingInCheck(this.getCollisionMap(), !isWhiteTurn);
        ResultType outcome = (capturedPiece != null) ? ResultType.SUCCESS_CAPTURE : ResultType.SUCCESS_STANDARD;

        // Check for future promotion hook
        if (piece.getType() == ChessPiece.Type.PAWN && (to.y == 0 || to.y == 7)) {
            outcome = ResultType.SUCCESS_PROMOTION_REQUIRED;
        }

        // 6. Flip internal turn clock
        advanceTurn();

        // Fire and forget the background worker thread
        dumpBoardToConsole(activePieces);
//        new Thread(() -> {
//            // Run the console output loop inside the worker thread context
//            dumpBoardToConsole(activePieces);
//        }, "Chess-Diagnostic-Dumper").start();

        // Generate clean console logging string
        String logText = "logtext tbd"; // formatAlgebraicNotation(piece, to, target != null);

        return new MoveAnalysis(outcome, opponentInCheck, capturedPiece, logText);
    }

    /**
     * The Definitive State Mutation Gatekeeper for castling. Skips geometry/self-check
     * validation for now (see ChessMoveValidator.castlingExposesKingToCheck) - the caller
     * is trusted to only invoke this for a legal castle. Still guards against a stale
     * king/rook anchor (e.g. a rook that was actually captured earlier) corrupting the board.
     */
    public MoveAnalysis submitCastle(boolean white, boolean kingside) {
        if (white != isWhiteTurn) {
            return new MoveAnalysis(ResultType.REJECTED_OUT_OF_TURN, false, null, "");
        }

        ChessPiece king = getKing(white);
        ChessPiece rook = getRook(white, kingside);
        if (king == null || rook == null
                || activePieces.get(king.getPosition()) != king
                || activePieces.get(rook.getPosition()) != rook) {
            return new MoveAnalysis(ResultType.REJECTED_CASTLING_UNAVAILABLE, false, null, "");
        }

        // TODO: real castling legality once ChessMoveValidator's check-detection exists
        // (king/rook must be unmoved, path clear, king not moving through/into check).
        if (validator.castlingExposesKingToCheck(this, white, kingside)) {
            return new MoveAnalysis(ResultType.REJECTED_SELF_CHECK, false, null, "");
        }

        int rank = white ? 0 : 7;
        Point kingTo = new Point(kingside ? 6 : 2, rank);
        Point rookTo = new Point(kingside ? 5 : 3, rank);

        activePieces.remove(king.getPosition());
        activePieces.remove(rook.getPosition());
        activePieces.put(kingTo, king);
        activePieces.put(rookTo, rook);
        king.setPosition(kingTo);
        rook.setPosition(rookTo);

        generateMatrix(activePieces);
        advanceTurn();
        dumpBoardToConsole(activePieces);

        return new MoveAnalysis(ResultType.SUCCESS_STANDARD, false, null, kingside ? "O-O" : "O-O-O");
    }

    public void setEnPassantVulnerableSquare(Point enPassantSquareBeforeMove) {
        enPassantVulnerableSquare = enPassantSquareBeforeMove;
    }

    public Point getEnPassantVulnerableSquare() {
        return enPassantVulnerableSquare;
    }

    private void generateMatrix(Map<Point, ChessPiece> activePieces2) {
        this.currentControlMatrix = new SquareControlMatrix();

        for (final ChessPiece cursorPiece : this.activePieces.values() ) {
            getValidator().getValidMoves(this, cursorPiece, ChessMoveValidator.EvaluationContext.CONTROL,
                    (piece, coordinate, isCollisionObstacle) -> {
                // This lambda block matches the RaycastObserver signature perfectly!
                currentControlMatrix.incrementControl(coordinate.x, coordinate.y, piece.isWhite());
            });
        }

    }

    /**
     * Populate activePieces with starting 32 tokens at standard Point keys
     * In your final system, your interactive ChessPieceToken swing component 
     * simply holds a reference to its backend ChessPiece domain object.
     */
    private void initializeStandardBoard() {
        activePieces.clear(); // Clear any existing residues

        // 1. The Back Rank Blueprint (Left-to-Right layout mapping)
        Type[] backRankBlueprint = {
            Type.ROOK, Type.KNIGHT, Type.BISHOP, Type.QUEEN, 
            Type.KING, Type.BISHOP, Type.KNIGHT, Type.ROOK
        };

        // 2. Loop cleanly through both armies
        boolean[] teams = { true, false }; // true = White, false = Black
        
        for (boolean isWhite : teams) {
            int backRank = isWhite ? 0 : 7;
            int pawnRank = isWhite ? 1 : 6;

            // Populate the Back Rank using the Blueprint array indices
            for (int file = 0; file < 8; file++) {
                Point pos = new Point(file, backRank);
                Type type = backRankBlueprint[file];
                
                ChessPiece piece = new ChessPiece(type, isWhite, pos);
                activePieces.put(pos, piece);

                // =========================================================
                // CAPTURE GAME-LEVEL POINTERS AUTONOMOUSLY VIA COORDINATES
                // =========================================================
                if (type == Type.KING) {
                    if (isWhite) this.whiteKing = piece;
                    else this.blackKing = piece;
                } 
                else if (type == Type.ROOK) {
                    if (file == 0) { // Queenside (File A)
                        if (isWhite) this.whiteQueensideRook = piece;
                        else this.blackQueensideRook = piece;
                    } else if (file == 7) { // Kingside (File H)
                        if (isWhite) this.whiteKingsideRook = piece;
                        else this.blackKingsideRook = piece;
                    }
                }
            }

            // Populate the Pawn Rank sequentially
            for (int file = 0; file < 8; file++) {
                Point pos = new Point(file, pawnRank);
                ChessPiece pawn = new ChessPiece(Type.PAWN, isWhite, pos);
                activePieces.put(pos, pawn);
            }
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

    /**
     * Diagnostic utility to dump the active pieces map to the console as an 8x8 grid view.
     */
    public void dumpBoardToConsole(Map<Point, ChessPiece> activePieces) {
        System.out.println("\n--- ENGINE BOARD DIAGNOSTIC MATRIX ---");
        
        // Chess ranks traditional layout goes from 8 down to 1 (y = 7 down to 0)
        for (int rank = 7; rank >= 0; rank--) {
            // Print the rank index prefix for quick coordinate tracking
            System.out.print(rank + " "); 
            
            for (int file = 0; file < 8; file++) {
                final Point currentCoordinate = new Point(file, rank);
                final ChessPiece piece = activePieces.get(currentCoordinate);

                if (piece == null) {
                    System.out.print("[ ]");
                } else {
                    // Snatch the first letter of the type enum name (KING -> "K")
                    String symbol = piece.getGlyph();
//                            piece.getType().name().substring(0, 1);
//                    
//                    // Special case for Knight since both King and Knight start with 'K'
//                    if (piece.getType() == Type.KNIGHT) {
//                        symbol = "N"; // The standard chess notation symbol for Knight
//                    }
                    
                    System.out.print("[" + symbol + "]");
                }
            }
            // Advance to the next rank line
            System.out.println();
        }
        
        // Print the file index footer
        System.out.println("   0  1  2  3  4  5  6  7\n");
    }

    public void advanceTurn() {
        this.isWhiteTurn = !this.isWhiteTurn;
    }

    /**
     * Restore all game state based on the MoveEvent.
     * 
     * @param event
     */
    public void restoreMovedPiece(MoveEvent event) {
        // update the map : since the map key is the point, it has to be removed and then re-added
        activePieces.remove(event.getDestination());
        activePieces.put(event.getOrigin(), event.getMovedPiece());

        // update the piece itself
        event.getMovedPiece().setPosition(event.getOrigin());

        // Reset the moved flag if this transaction was its maiden voyage
        if (event.getMovedPiece().getType() == Type.PAWN) {
            if (event.wasInitialPawnMove()) {
                System.out.println("restoreMovedPiece: " );
                event.getMovedPiece().setHasMoved(false);
            }
        }

        // restore whose turn it is
        isWhiteTurn = event.getMovedPiece().isWhite();

    }

    public void restoreCapturedPiece(MoveEvent event) {
        ChessPiece piece = event.getCapturedPiece();
        if (piece != null) {
            // Handle standard vs en passant placement
            Point spawnPoint = event.getDestination(); // TODO test this, the original AI code is below
                    // .getCapturedLocation(); 
            activePieces.put(spawnPoint, event.getCapturedPiece());
            if (piece.isWhite())
                whiteCaptured.remove(piece);
            else
                blackCaptured.remove(piece);
        }
    }

    public void restoreCastleRook(MoveEvent event) {
        ChessPiece rook = event.getRookMoved();
        activePieces.remove(event.getRookDestination());
        activePieces.put(event.getRookOrigin(), rook);
        rook.setPosition(event.getRookOrigin());
    }

}
