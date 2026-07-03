package org.jwellman.diagram.api;

import java.awt.Color;

/**
 * Defines the color palette for a diagram canvas.
 *
 * The canvas is always rendered with a light/white background regardless of the
 * host application's Look and Feel. All node and edge colors are sourced from
 * the active theme so that the diagram remains visually consistent when the
 * surrounding desktop switches between light and dark LAFs.
 *
 * The default implementation is LightCanvasTheme (in core). Custom themes are
 * injected via DiagramLayeredPane and propagate to node factories automatically.
 */
public interface CanvasTheme {

    /** Background color of the canvas itself (always light). */
    Color getCanvasBackground();

    /** Color of the grid lines painted over the canvas background. */
    Color getGridLineColor();

    /**
     * Background color for a node's header strip.
     * @param stereotype node type string, e.g. "CLASS" or "INTERFACE"
     */
    Color getNodeHeaderBackground(String stereotype);

    /** Background color of the node body (fields + methods sections). */
    Color getNodeBodyBackground();

    /** Color of the border drawn around the outside of a node. */
    Color getNodeBorderColor();

    /** Primary text color used for node labels and entry text. */
    Color getTextColor();

    /** Color used to stroke edges (connections between nodes). */
    Color getEdgeColor();
}
