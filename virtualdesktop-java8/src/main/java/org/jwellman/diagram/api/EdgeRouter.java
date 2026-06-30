package org.jwellman.diagram.api;

import java.awt.Point;
import java.awt.geom.Path2D;

/**
 * Strategy for computing the path between two port locations.
 */
public interface EdgeRouter {

    Path2D calculatePath(Point start, Point end);

    /**
     * Returns the point immediately before {@code end} on the computed path.
     * Used by the edge renderer to orient the arrowhead along the final segment
     * rather than along the diagonal from start to end.
     */
    Point getApproachPoint(Point start, Point end);
}
