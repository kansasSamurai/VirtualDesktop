package org.jwellman.diagram.core;

import java.awt.Point;
import java.awt.geom.Path2D;

import org.jwellman.diagram.api.EdgeRouter;

/**
 * Port-direction-aware orthogonal router.
 *
 * Chooses the bend shape based on whether each port faces horizontally (E/W)
 * or vertically (N/S):
 *
 *   both vertical   → V-H-V  (vertical stub, horizontal crossing, vertical stub)
 *   both horizontal → H-V-H  (horizontal stub, vertical crossing, horizontal stub)
 *   vertical → horizontal  → L: go vertical first, then horizontal
 *   horizontal → vertical  → L: go horizontal first, then vertical
 */
public class OrthogonalRouter implements EdgeRouter {

    @Override
    public Path2D calculatePath(Point start, String startPortId,
                                Point end, String endPortId) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(start.x, start.y);

        boolean startV = isVertical(startPortId);
        boolean endV   = isVertical(endPortId);

        if (startV && endV) {
            // V-H-V: vertical stub from start, horizontal cross, vertical stub to end
            int midY = (start.y + end.y) / 2;
            path.lineTo(start.x, midY);
            path.lineTo(end.x,   midY);
            path.lineTo(end.x,   end.y);

        } else if (!startV && !endV) {
            // H-V-H: horizontal stub from start, vertical cross, horizontal stub to end
            int midX = (start.x + end.x) / 2;
            path.lineTo(midX, start.y);
            path.lineTo(midX, end.y);
            path.lineTo(end.x, end.y);

        } else if (startV) {
            // start exits vertically, end enters horizontally → one-bend L
            path.lineTo(start.x, end.y);
            path.lineTo(end.x,   end.y);

        } else {
            // start exits horizontally, end enters vertically → one-bend L
            path.lineTo(end.x, start.y);
            path.lineTo(end.x, end.y);
        }

        return path;
    }

    @Override
    public Point getApproachPoint(Point start, String startPortId,
                                  Point end, String endPortId) {
        boolean startV = isVertical(startPortId);
        boolean endV   = isVertical(endPortId);

        if (startV && endV) {
            int midY = (start.y + end.y) / 2;
            return new Point(end.x, midY);          // last segment is vertical

        } else if (!startV && !endV) {
            int midX = (start.x + end.x) / 2;
            return new Point(midX, end.y);           // last segment is horizontal

        } else if (startV) {
            return new Point(start.x, end.y);        // last segment is horizontal

        } else {
            return new Point(end.x, start.y);        // last segment is vertical
        }
    }

    private boolean isVertical(String portId) {
        return "N".equals(portId) || "S".equals(portId);
    }
}
