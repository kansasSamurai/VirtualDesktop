package org.jwellman.diagram.core;

import java.awt.Point;
import java.awt.geom.Path2D;

import org.jwellman.diagram.api.EdgeRouter;

/**
 * Draws a direct straight line between two port locations.
 */
public class StraightLineRouter implements EdgeRouter {

    @Override
    public Path2D calculatePath(Point start, Point end) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(start.x, start.y);
        path.lineTo(end.x, end.y);
        return path;
    }

    @Override
    public Point getApproachPoint(Point start, Point end) {
        return start;
    }
}
