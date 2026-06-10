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

    public List<Point> getValidMoves(ChessGame game, ChessPiece selectedPiece) {

        // 1. Create a lightweight, high-speed virtual matrix projection
        // An empty square is represented by null.
        ChessPiece[][] virtualMatrix = new ChessPiece[8][8];

        // Project the current game state into the matrix bounds
        for (Map.Entry<Point, ChessPiece> entry : game.getActivePieces().entrySet()) {
            Point p = entry.getKey();
            virtualMatrix[p.x][p.y] = entry.getValue();
        }
        
        // 2. Locate the current coordinates of our target component
        Point start = selectedPiece.getPosition();
        if (start == null) return new ArrayList<>();

        // 3. Feed the virtual matrix directly into our single vector-raycasting loop!
        return executeRaycastValidation(game, selectedPiece );
    }

    /**
     * Generates all valid non-capture destinations for a piece at a given square.
     * 
     * @param board The 8x8 tracking array (null indicates an empty square)
     * @param start The current logical Point(file, rank) of the piece
     * @param pieceType The character representation ('R', 'B', 'Q', 'N', 'K')
     */
    private List<Point> executeRaycastValidation(ChessGame game, ChessPiece piece) {
        List<Point> validSquares = new ArrayList<>();

        // 5. Get a readable label for the piece identity
        String side = piece.isWhite() ? "White" : "Black";
        String pieceName = piece.getType().name().toLowerCase();

        // 1. Resolve the piece's identity down to vectors and step boundaries
        Point start = piece.getPosition();
        System.out.println("Raycasting from center: " + start);
        // If it prints the OLD position instead of the new destination, your state map has drifted!

        Point[] directions = piece.getMovementVectors();
        int maxSteps = piece.getStepCount();
        System.out.printf("%s %s maxSteps: %s%n", side, pieceName, maxSteps);

        // 2. Raycast out along each vector direction
        for (Point vector : directions) {
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

                Point targetSquare = new Point(nextX, nextY);
                ChessPiece occupant = game.getPieceAt(targetSquare);

                // 2. Collision Matrix Check
                if (occupant == null) {
                    // Case 1: Empty Space. Valid move, keep sliding.
                    validSquares.add(targetSquare);
                } 
                else if (occupant.isWhite() != piece.isWhite()) {
                    // Case 2: Enemy Piece! Valid capture, but the ray must stop here.
                    validSquares.add(targetSquare);
                    break; 
                } 
                else {
                    // Case 3: Friendly Piece. Blocked completely. Stop the ray.
                    break; 
                }

                stepsTaken++;
            }
        }

        System.out.println("Found this many valid moves: " + validSquares.size());
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
