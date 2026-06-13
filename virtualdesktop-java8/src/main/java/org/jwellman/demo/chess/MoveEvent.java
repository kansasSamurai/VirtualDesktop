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
    
    // Package-private constructor...
    MoveEvent(Point origin, Point destination, 
            ChessPiece movedPiece, ChessPiece capturedPiece,
            Point enPassantSquareBeforeMove, boolean wasInitialPawnMove) {
        super();
        this.origin = origin;
        this.destination = destination;
        this.movedPiece = movedPiece;
        this.capturedPiece = capturedPiece;
        this.enPassantSquareBeforeMove = enPassantSquareBeforeMove;
        this.wasInitialPawnMove = wasInitialPawnMove;
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

    public String toAlgebraicNotation() {
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
