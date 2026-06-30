package org.jwellman.diagram.api;

import java.awt.Point;
import java.awt.geom.Path2D;

/**
 * Strategy for computing the path between two port locations.
 * Port IDs (e.g. "N","S","E","W") are provided so the router can choose the
 * correct shape — H-V-H for horizontal ports, V-H-V for vertical ports, etc.
 */
public interface EdgeRouter {

    Path2D calculatePath(Point start, String startPortId, Point end, String endPortId);

    /**
     * Returns the point immediately before {@code end} on the computed path,
     * used to orient the arrowhead along the final segment.
     */
    Point getApproachPoint(Point start, String startPortId, Point end, String endPortId);
}
