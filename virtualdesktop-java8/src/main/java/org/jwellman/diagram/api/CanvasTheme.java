package org.jwellman.diagram.api;

import java.awt.Color;

/**
 * Defines the color palette for a diagram canvas.
 *
 * Defines a complete color palette for the diagram canvas and its nodes.
 * All canvas and node colors are sourced from the active theme so that the
 * diagram looks intentional and consistent regardless of the host LAF.
 *
 * The default implementation is BlueprintCanvasTheme (in core). Custom themes are
 * injected via DiagramLayeredPane and propagate to node factories automatically.
 */
public interface CanvasTheme {

    /** Stable identifier used for the toolbar selector and persistence. */
    String getThemeName();

    /** Background color of the canvas surface. */
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

    /** Color used for the stereotype label (e.g. «interface»); often a tint of the text color. */
    Color getStereotypeTextColor();

    /** Color used to stroke edges (connections between nodes). */
    Color getEdgeColor();
}
