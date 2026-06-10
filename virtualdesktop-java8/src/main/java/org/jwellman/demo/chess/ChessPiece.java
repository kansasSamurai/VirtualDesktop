package org.jwellman.demo.chess;

import java.awt.Point;

public class ChessPiece {

    /**
     * A pure, stateless Domain Model representing the immutable identity 
     * and movement rules of a specific chess piece.
     */
    public enum Type {
        ROOK, BISHOP, QUEEN, KING, KNIGHT, PAWN;
    }

    private final Type type;
    private Point position; // Its current logical Point(file, rank)
    private final boolean isWhite;
    private boolean hasMoved = false; // The magic tracking flag

    public ChessPiece(Type type, boolean isWhite, Point startingPosition) {
        this.type = type;
        this.isWhite = isWhite;
        this.position = new Point(startingPosition);
    }

    // --- Domain Capabilities ---
    
    public Type getType() {
        return type;
    }

    public boolean isWhite() {
        return isWhite;
    }

    protected boolean hasMoved() {
        return hasMoved;
    }

    protected boolean hasNotMoved() {
        return !hasMoved();
    }

    public Point getPosition() { 
        // 2. Format the starting coordinate before overwriting it
        String originNotation = toAlgebraic(this.position);
        System.out.printf("Piece is at %s (%s,%s)%n", originNotation, this.position.x, this.position.y );

        return position; 
    }

    public void setPosition(Point newPos) {
        // 1. Guard against null or redundant assignments
        if (newPos == null || newPos.equals(this.position)) {
            if (newPos != null) this.position = new Point(newPos);
            return;
        }

        this.hasMoved = true;

        // 2. Format the starting coordinate before overwriting it
        String originNotation = toAlgebraic(this.position);

        // 3. Update the internal state memory
        this.position = new Point(newPos);

        // 4. Format the destination coordinate
        String destinationNotation = toAlgebraic(this.position);

        // 5. Get a readable label for the piece identity
        String pieceName = this.type.name().toLowerCase();
        String side = this.isWhite ? "White" : "Black";

        // 6. Log to the console: e.g., "White knight moved from b1 to c3"
        System.out.printf("%s %s moved from %s to %s%n", 
            side, pieceName, originNotation, destinationNotation);
    }

    /**
     * High-craft helper to map 0-indexed layout coordinates into standard chess notation.
     * x=0 -> 'a', x=7 -> 'h'
     * y=0 -> '1', y=7 -> '8'
     */
    private String toAlgebraic(Point pt) {
        if (pt == null)
            return "??";

        // Calculate the file character ('a' + 0 = 'a', 'a' + 1 = 'b', etc.)
        char file = (char) ('a' + pt.x);

        // Calculate the rank numeric character ('1' + 0 = '1', '1' + 7 = '8')
        char rank = (char) ('1' + pt.y);

        return "" + file + rank;
    }

    /**
     * High-Craft Move Abstraction: The piece itself dictates its mechanical identity
     * to the validator engine without exposing internal implementation details.
     */
    public boolean isStepLimited() {
        return type == Type.KING || type == Type.KNIGHT || type == Type.PAWN;
    }

    // switch on enum must be java14+
//    public Point[] getMovementVectors() {
//        return switch (this.type) {
//            case ROOK   -> MoveVectors.STRAIGHT;
//            case BISHOP -> MoveVectors.DIAGONAL;
//            case QUEEN, KING -> MoveVectors.OMNIDIRECTIONAL;
//            case KNIGHT -> MoveVectors.KNIGHT_LEAPS;
//            case PAWN   -> isWhite ? MoveVectors.PAWN_WHITE : MoveVectors.PAWN_BLACK;
//        };
//    }

    public Point[] getMovementVectors() {
        if (Type.ROOK.equals(this.type)) 
            return ChessMoveVectors.STRAIGHT;
        if (Type.BISHOP.equals(this.type)) 
            return ChessMoveVectors.DIAGONAL;
        if (Type.KNIGHT.equals(this.type)) 
            return ChessMoveVectors.KNIGHT_LEAPS;
        if (Type.KING.equals(this.type)) 
            return ChessMoveVectors.OMNIDIRECTIONAL;
        if (Type.QUEEN.equals(this.type)) 
            return ChessMoveVectors.OMNIDIRECTIONAL;
        if (Type.PAWN.equals(this.type))
            return isWhite ? ChessMoveVectors.PAWN_WHITE : ChessMoveVectors.PAWN_BLACK;

        return null;
    }

    public String getGlyph() {
        if (Type.ROOK.equals(this.type)) 
            return "R";
        if (Type.BISHOP.equals(this.type)) 
            return "B";
        if (Type.KNIGHT.equals(this.type)) 
            return "N";
        if (Type.KING.equals(this.type)) 
            return "K";
        if (Type.QUEEN.equals(this.type)) 
            return "Q";
        if (Type.PAWN.equals(this.type))
            return "P";

        return null;
    }

    /**
     * The number of times the move vector can be applied.
     * 
     * @return
     */
    public int getStepCount() {
        if (this.getType() == Type.PAWN) {
            if (this.hasMoved()) return 1; else return 2;
        }
        if (this.getType() == Type.KING) {
            return 1;
        }
        return 8;
    }

}
