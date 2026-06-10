package org.jwellman.demo.chess;

import java.awt.Point;

/**
 * A functional interceptor contract to observe the live lifecycle of a raycast sweep.
 */
@FunctionalInterface
public interface RaycastObserver {

    /**
     * Invoked every single time a ray steps onto a valid board coordinate, 
     * immediately BEFORE collision logic determines if the ray must terminate.
     */
    void onSquareVisited(ChessPiece cursorPiece, Point coordinate, boolean isCollisionObstacle);

}
