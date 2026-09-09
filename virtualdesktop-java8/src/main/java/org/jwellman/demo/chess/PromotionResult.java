package org.jwellman.demo.chess;

/**
 * The outcome of a pawn promotion transaction: the new piece that replaced the pawn,
 * and whichever piece (if any) previously occupied the destination square and was
 * displaced/captured by the promoting pawn's move.
 */
public final class PromotionResult {

    private final ChessPiece promotedPiece;
    private final ChessPiece capturedPiece; // Nullable

    public PromotionResult(ChessPiece promotedPiece, ChessPiece capturedPiece) {
        this.promotedPiece = promotedPiece;
        this.capturedPiece = capturedPiece;
    }

    public ChessPiece getPromotedPiece() {
        return promotedPiece;
    }

    public ChessPiece getCapturedPiece() {
        return capturedPiece;
    }

}
