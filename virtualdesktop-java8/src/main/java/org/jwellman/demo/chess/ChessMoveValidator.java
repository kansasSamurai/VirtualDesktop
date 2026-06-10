package org.jwellman.demo.chess;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author rwellman
 *
 */
public class ChessMoveValidator {

    public boolean moveIsLegal(CollisionMap collisionMap, ChessPiece piece, Point to) {
        // TODO Auto-generated method stub
        return true;
    }

    public boolean moveIsNotLegal(CollisionMap collisionMap, ChessPiece piece, Point to) {
        return ! moveIsLegal(collisionMap, piece, to);
    }

    public List<Point> getValidMoves(ChessGame game, ChessPiece selectedPiece, RaycastObserver observer) {

// This was old original code... commenting out on 6/10; remove eventually
//        // 1. Create a lightweight, high-speed virtual matrix projection
//        // An empty square is represented by null.
//        ChessPiece[][] virtualMatrix = new ChessPiece[8][8];
//
//        // Project the current game state into the matrix bounds
//        for (Map.Entry<Point, ChessPiece> entry : game.getActivePieces().entrySet()) {
//            Point p = entry.getKey();
//            virtualMatrix[p.x][p.y] = entry.getValue();
//        }
//        
//        // 2. Locate the current coordinates of our target component
//        Point start = selectedPiece.getPosition();
//        if (start == null) return new ArrayList<>();

        // 3. Feed the virtual matrix directly into our single vector-raycasting loop!
        return executeRaycastValidation(game, selectedPiece, observer);
    }

    /**
     * Generates all valid non-capture destinations for a piece at a given square.
     * 
     * @param board The 8x8 tracking array (null indicates an empty square)
     * @param start The current logical Point(file, rank) of the piece
     * @param pieceType The character representation ('R', 'B', 'Q', 'N', 'K')
     */
    private List<Point> executeRaycastValidation(ChessGame game, ChessPiece piece, RaycastObserver observer) {
        final List<Point> validSquares = new ArrayList<>();

        final String side = piece.isWhite() ? "White" : "Black";
        final String pieceName = piece.getType().name().toLowerCase();

        final Point start = piece.getPosition();
        final Point[] directions = piece.getMovementVectors();
        final int maxSteps = piece.getStepCount();

        for (final Point vector : directions) {
            int nextX = start.x;
            int nextY = start.y;
            int stepsTaken = 0;

            while (stepsTaken < maxSteps) {
                nextX += vector.x;
                nextY += vector.y;

                // 1. Boundary Defense (Edge of the board)
                if (nextX < 0 || nextX >= 8 || nextY < 0 || nextY >= 8) {
                    break; 
                }

                final Point targetSquare = new Point(nextX, nextY);
                final ChessPiece occupant = game.getPieceAt(targetSquare);
                
                // --- HIGH CRAFT HOOK INJECTION ---
                // A piece exerts control over a square whether it is empty, occupied by an enemy, 
                // OR occupied by a friend (defending it). So we notify the observer immediately!
                if (observer != null) {
                    final boolean isCollisionObstacle = (occupant != null);
                    observer.onSquareVisited(piece, targetSquare, isCollisionObstacle);
                }
                // ----------------------------------

                // 2. Collision Matrix Check (Remains identical for movement paths)
                if (occupant == null) {
                    validSquares.add(targetSquare);
                } 
                else if (occupant.isWhite() != piece.isWhite()) {
                    validSquares.add(targetSquare);
                    break; // Enemy blocks the move ray
                } 
                else {
                    break; // Friend blocks the move ray
                }

                stepsTaken++;
            }
        }

        return validSquares;
    }

    public boolean exposesKingToCheck(ChessGame chessGame, Point from, Point to) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isKingInCheck(CollisionMap collisionMap, boolean b) {
        // TODO Auto-generated method stub
        return false;
    }

}
