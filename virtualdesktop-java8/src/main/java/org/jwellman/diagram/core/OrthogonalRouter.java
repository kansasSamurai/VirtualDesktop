package org.jwellman.diagram.core;

import java.awt.Point;
import java.awt.geom.Path2D;

import org.jwellman.diagram.api.EdgeRouter;

/**
 * Routes edges as a two-segment L-path: horizontal then vertical,
 * using the midpoint as the bend.
 */
public class OrthogonalRouter implements EdgeRouter {

    @Override
    public Path2D calculatePath(Point start, Point end) {
        Path2D.Double path = new Path2D.Double();
        int midX = (start.x + end.x) / 2;

        path.moveTo(start.x, start.y);
        path.lineTo(midX, start.y);   // horizontal segment
        path.lineTo(midX, end.y);     // vertical segment
        path.lineTo(end.x, end.y);    // horizontal to target
        return path;
    }

    @Override
    public Point getApproachPoint(Point start, Point end) {
        // Last segment goes from (midX, end.y) → end, so approach from midX
        int midX = (start.x + end.x) / 2;
        return new Point(midX, end.y);
    }
}
