package org.jwellman.demo.chess;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChessMoveValidator {

    public boolean isMoveLegal(ChessGame chessGame, ChessPiece piece, Point to) {
        // TODO Auto-generated method stub
        return true;
    }

    public List<Point> getValidNonCaptureMoves(Map<Point, ChessPiece> activePieces, ChessPiece selectedPiece) {
        // 1. Create a lightweight, high-speed virtual matrix projection
        // An empty square is represented by null.
        ChessPiece[][] virtualMatrix = new ChessPiece[8][8];

        // Project the current game state into the matrix bounds
        for (Map.Entry<Point, ChessPiece> entry : activePieces.entrySet()) {
            Point p = entry.getKey();
            virtualMatrix[p.x][p.y] = entry.getValue();
        }
        
        // 2. Locate the current coordinates of our target component
        Point start = selectedPiece.getPosition();
        if (start == null) return new ArrayList<>();

        // 3. Feed the virtual matrix directly into our single vector-raycasting loop!
        return executeRaycastValidation(virtualMatrix, selectedPiece );
    }
    
    /**
     * Generates all valid non-capture destinations for a piece at a given square.
     * 
     * @param board The 8x8 tracking array (null indicates an empty square)
     * @param start The current logical Point(file, rank) of the piece
     * @param pieceType The character representation ('R', 'B', 'Q', 'N', 'K')
     */
    public List<Point> executeRaycastValidation(ChessPiece[][] board, ChessPiece piece) {
        List<Point> validSquares = new ArrayList<>();
        
        // 1. Resolve the piece's identity down to vectors and step boundaries
        Point start = piece.getPosition();
        System.out.println("Raycasting from center: " + start);
        // If it prints the OLD position instead of the new destination, your state map has drifted!
        Point[] directions = piece.getMovementVectors();
        int maxSteps = piece.isStepLimited() ? 1 : 8;

        // 2. Raycast out along each vector direction
        for (Point dir : directions) {
            int nextX = start.x;
            int nextY = start.y;
            int stepsLeft = maxSteps;

            while (stepsLeft > 0) {
                nextX += dir.x;
                nextY += dir.y;

                // Boundary Defense: If we fly off the 8x8 matrix, kill the ray
                if (nextX < 0 || nextX >= 8 || nextY < 0 || nextY >= 8) {
                    break;
                }

                // High-Craft Matrix Check
                if (board[nextX][nextY] == null) {
                    // The square is completely empty—this is a valid non-capture destination!
                    validSquares.add(new Point(nextX, nextY));
                } else {
                    // We collided with a piece! Since this is non-capture validation,
                    // the ray is blocked, so we immediately terminate this direction.
                    break;
                }

                stepsLeft--;
            }
        }

        System.out.println("Found this many valid moves: " + validSquares.size());
        return validSquares;
    }

}
