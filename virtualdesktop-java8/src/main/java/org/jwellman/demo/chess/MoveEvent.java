package org.jwellman.demo.chess;

import java.awt.Point;

import org.jwellman.demo.chess.ChessPiece.Type;

public final class MoveEvent {

    private final Point origin;
    private final Point destination;
    private final ChessPiece movedPiece;
    private final ChessPiece capturedPiece; // Nullable
    private final Point enPassantSquareBeforeMove; // For restoring previous EP state
    private final boolean wasInitialPawnMove;
    private final ChessPiece rookMoved; // Nullable - set only for castling
    private final Point rookOrigin;
    private final Point rookDestination;

    // Package-private constructor...
    MoveEvent(Point origin, Point destination,
            ChessPiece movedPiece, ChessPiece capturedPiece,
            Point enPassantSquareBeforeMove, boolean wasInitialPawnMove) {
        this(origin, destination, movedPiece, capturedPiece, enPassantSquareBeforeMove,
                wasInitialPawnMove, null, null, null);
    }

    // Package-private constructor for castling, carrying the rook's half of the move...
    MoveEvent(Point origin, Point destination,
            ChessPiece movedPiece, ChessPiece capturedPiece,
            Point enPassantSquareBeforeMove, boolean wasInitialPawnMove,
            ChessPiece rookMoved, Point rookOrigin, Point rookDestination) {
        super();
        this.origin = origin;
        this.destination = destination;
        this.movedPiece = movedPiece;
        this.capturedPiece = capturedPiece;
        this.enPassantSquareBeforeMove = enPassantSquareBeforeMove;
        this.wasInitialPawnMove = wasInitialPawnMove;
        this.rookMoved = rookMoved;
        this.rookOrigin = rookOrigin;
        this.rookDestination = rookDestination;
    }

    protected Point getOrigin() {
        return origin;
    }

    protected Point getDestination() {
        return destination;
    }

    protected ChessPiece getMovedPiece() {
        return movedPiece;
    }

    protected ChessPiece getCapturedPiece() {
        return capturedPiece;
    }

    protected Point getEnPassantSquareBeforeMove() {
        return enPassantSquareBeforeMove;
    }

    protected boolean wasInitialPawnMove() {
        return wasInitialPawnMove;
    }

    protected ChessPiece getRookMoved() {
        return rookMoved;
    }

    protected Point getRookOrigin() {
        return rookOrigin;
    }

    protected Point getRookDestination() {
        return rookDestination;
    }

    public String toAlgebraicNotation() {
        if (rookMoved != null) {
            // Castling has its own notation, independent of origin/destination/capture
            return destination.x == 6 ? "O-O" : "O-O-O";
        }

        // If it's a capture, use 'x' (e.g., "e4xd5"), otherwise a hyphen (e.g., "e2-e4")
        String separator = (capturedPiece != null) ? "x" : "-";

        // Map your coordinate Points to chess board letters (0 -> 'a', 7 -> 'h')
        char fileOrigin = (char) ('a' + origin.x);
        char fileDest = (char) ('a' + destination.x);

        // Chess ranks are 1-indexed (0 -> '1', 7 -> '8')
        int rankOrigin = origin.y + 1;
        int rankDest = destination.y + 1;

        // Optional: Add piece prefixes (e.g., "N", "B", "R", "Q", "K" - pawns are blank)
        String piecePrefix = movedPiece.getType() == Type.PAWN ? " " : movedPiece.getGlyph();

        return piecePrefix + fileOrigin + rankOrigin + separator + fileDest + rankDest;
    }
    
}
