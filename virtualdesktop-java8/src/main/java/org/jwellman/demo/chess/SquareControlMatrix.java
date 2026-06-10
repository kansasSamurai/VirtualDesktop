package org.jwellman.demo.chess;

/**
 * An immutable snapshot of tactical tension across the board.
 * Optimized for lightning-fast UI rendering queries.
 */
public final class SquareControlMatrix {

    // An 8x8 grid storing the exact number of attackers
    private final int[][] whiteControlCount = new int[8][8];
    private final int[][] blackControlCount = new int[8][8];

    // --- Package-Private Mutation Gate (Only the builder/engine writes here) ---
    void incrementControl(int file, int rank, boolean isWhite) {
        if (isWhite) {
            whiteControlCount[file][rank]++;
        } else {
            blackControlCount[file][rank]++;
        }
    }

    // --- Public Fluent API for the UI Components ($O(1)$ Performance) ---
    public int getWhiteAttackerCount(int file, int rank) {
        return whiteControlCount[file][rank];
    }

    public int getBlackAttackerCount(int file, int rank) {
        return blackControlCount[file][rank];
    }

    public boolean isContested(int file, int rank) {
        return whiteControlCount[file][rank] > 0 && blackControlCount[file][rank] > 0;
    }

}
