package org.jwellman.demo.chess;

import java.awt.Point;

public class MoveVectors {

 // Cross-shaped vectors (Rook)
    public static final Point[] STRAIGHT = {
        new Point(0, 1), new Point(0, -1), new Point(1, 0), new Point(-1, 0)
    };

    // X-shaped vectors (Bishop)
    public static final Point[] DIAGONAL = {
        new Point(1, 1), new Point(1, -1), new Point(-1, 1), new Point(-1, -1)
    };

    // Star-shaped vectors (Queen / King)
    public static final Point[] OMNIDIRECTIONAL = {
        new Point(0, 1),  new Point(0, -1),  new Point(1, 0),  new Point(-1, 0),
        new Point(1, 1),  new Point(1, -1),  new Point(-1, 1), new Point(-1, -1)
    };

    // L-shaped vectors (Knight)
    public static final Point[] KNIGHT_LEAPS = {
        new Point(1, 2),  new Point(1, -2),  new Point(-1, 2),  new Point(-1, -2),
        new Point(2, 1),  new Point(2, -1),  new Point(-2, 1),  new Point(-2, -1)
    };

    public static final Point[] PAWN_WHITE = {
        new Point(0,1)
    };

    public static final Point[] PAWN_BLACK = {
        new Point(1,0)
    };

}
