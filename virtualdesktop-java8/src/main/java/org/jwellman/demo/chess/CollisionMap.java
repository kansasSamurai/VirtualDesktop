package org.jwellman.demo.chess;

import java.awt.Point;

/**
 * A read-only spatial index contract allowing the validation engine 
 * to evaluate piece collision geometry without exposing mutation capabilities.
 */
public interface CollisionMap {

    /**
     * Returns true if no piece occupies the specified coordinate.
     */
    boolean isEmptyAt(int x, int y);

    /**
     * Helper shorthand that accepts a standard Point token.
     */
    default boolean isEmptyAt(Point p) {
        return isEmptyAt(p.x, p.y);
    }

    /**
     * Returns true if a piece exists at the target and matches the requested color.
     */
    boolean isOccupiedByColor(int x, int y, boolean isWhite);

}
