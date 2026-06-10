package org.jwellman.demo.chess;

import java.util.Optional;

/**
 * An immutable telemetry record detailing the complete side-effects 
 * of a move transaction.
 */
public final class MoveAnalysis {

    private final boolean givesCheck;
    private final String algebraicNotation; // e.g., "Nf3" or "exd5"
    private final ResultType resultType;
    private final ChessPiece capturedPiece;

    /**
     * Categorizes the precise structural outcome of a submitted move.
     */
    public enum ResultType {
        REJECTED_ILLEGAL_GEOMETRY, // Piece cannot physically move that way
        REJECTED_OUT_OF_TURN,      // White moved on Black's clock
        REJECTED_SELF_CHECK,       // Move leaves or puts own King in check
        
        SUCCESS_STANDARD,          // Clean slide or jump into empty space
        SUCCESS_CAPTURE,           // Enemy piece permanently removed from play
        SUCCESS_PROMOTION_REQUIRED,// Pawn hit the back rank (trigger UI modal!)
        
        GAME_OVER_CHECKMATE,       // No legal moves remaining while in check
        GAME_OVER_STALEMATE        // No legal moves remaining, but not in check
    }
    
    public MoveAnalysis(ResultType resultType, boolean givesCheck, 
                        ChessPiece capturedPiece, String algebraicNotation) {
        this.resultType = resultType;
        this.givesCheck = givesCheck;
        this.capturedPiece = capturedPiece;
        this.algebraicNotation = algebraicNotation;
    }

    // --- High-Craft Fluent Inquiries ---

    public boolean isAccepted() {
        return resultType == ResultType.SUCCESS_STANDARD || resultType == ResultType.SUCCESS_CAPTURE
                || resultType == ResultType.SUCCESS_PROMOTION_REQUIRED || resultType == ResultType.GAME_OVER_CHECKMATE;
    }

    public ResultType getResultType() {
        return resultType;
    }

    public boolean isCheck() {
        return givesCheck;
    }

    public String getNotation() {
        return algebraicNotation;
    }

    public Optional<ChessPiece> getCapturedPiece() {
        return Optional.ofNullable(capturedPiece);
    }

}
