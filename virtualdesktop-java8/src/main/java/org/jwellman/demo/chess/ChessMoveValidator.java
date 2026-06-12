package org.jwellman.demo.chess;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jwellman.demo.chess.ChessMoveValidator.EvaluationContext;

/**
 * 
 * @author rwellman
 *
 */
public class ChessMoveValidator {

    public enum EvaluationContext {
        MOVEMENT, // Looking for legal squares to slide/step into
        CONTROL // Looking for squares the piece actively attacks/defends
    }

    public boolean moveIsLegal(CollisionMap collisionMap, ChessPiece piece, Point to) {
        // TODO Auto-generated method stub
        return true;
    }

    public boolean moveIsNotLegal(CollisionMap collisionMap, ChessPiece piece, Point to) {
        return ! moveIsLegal(collisionMap, piece, to);
    }

    public List<Point> getValidMoves(ChessGame game, ChessPiece piece, EvaluationContext context, RaycastObserver observer) {

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

        final List<Point> validSquares = new ArrayList<>();

        final String side = piece.isWhite() ? "White" : "Black";
        final String pieceName = piece.getType().name().toLowerCase();

        final Point start = piece.getPosition();
        final Point[] directions = piece.getMovementVectors(context);
        final int maxSteps = piece.getStepCount(context);

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
