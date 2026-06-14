package org.jwellman.demo.chess;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

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

    public boolean exposesKingToCheck(ChessGame chessGame, Point from, Point to) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isKingInCheck(CollisionMap collisionMap, boolean b) {
        // TODO Auto-generated method stub
        return false;
    }

    public List<Point> getValidMoves(ChessGame game, ChessPiece piece, EvaluationContext context, RaycastObserver observer) {

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
                if (context == EvaluationContext.MOVEMENT) {
                    
                }

                // 2. Collision Matrix Check (Now sensitive to piece biology and context)
                if (occupant == null) {
                    // Pawns cannot move diagonally into empty space unless it's a valid En Passant strike
                    if (piece.isPawn() && context == EvaluationContext.MOVEMENT) {
                        // If this specific vector step has a non-zero X component, it's a diagonal sweep.
                        // In a pure MOVEMENT sweep, empty diagonals are illegal.
                        if (vector.x != 0) {
                            // Skip adding, but pawns don't "break" rays because their rays are only 1-2 steps anyway
                            continue; 
                        }
                    }
                    
                    // Standard pathing for all other pieces/vectors: empty square is a green light
                    validSquares.add(targetSquare);
                } 
                else if (occupant.isWhite() != piece.isWhite()) {
                    // ENEMY COLLISION
                    if (piece.isPawn()) {
                        if (context == EvaluationContext.MOVEMENT) {
                            // A pawn CANNOT march forward into an enemy piece. It is a blockade.
                            if (vector.x == 0) {
                                break; // Ray is blocked, square is NOT valid
                            }
                        } else if (context == EvaluationContext.CONTROL) {
                            // In a control/attack assessment, the enemy on the diagonal is a valid strike target.
                            validSquares.add(targetSquare);
                            break; 
                        }
                    } else {
                        // Standard sliding piece behavior: capture the enemy and terminate the raycast
                        validSquares.add(targetSquare);
                        break; 
                    }
                } 
                else {
                    // FRIENDLY COLLISION
                    if (piece.isPawn() && context == EvaluationContext.CONTROL) {
                        // Crucial Telemetry Nuance: Even if a friendly piece sits on the diagonal, 
                        // the pawn STILL projects protective control onto that square for defense tracking.
                        validSquares.add(targetSquare);
                    }
                    break; // Friends always structurally block the physical raycast pass
                }

                stepsTaken++;
            }
        }

        return validSquares;
    }

}
