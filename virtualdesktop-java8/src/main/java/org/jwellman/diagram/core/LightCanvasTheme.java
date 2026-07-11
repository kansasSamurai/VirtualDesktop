package org.jwellman.diagram.core;

import java.awt.Color;

import org.jwellman.diagram.api.CanvasTheme;

/**
 * Default canvas theme: white background, light grid, blue/green node headers.
 * Matches the original hardcoded colors so existing diagrams are unaffected.
 */
public class LightCanvasTheme implements CanvasTheme {

    private static final Color GRID_LINE        = new Color(220, 220, 220);
    private static final Color INTERFACE_HEADER = new Color(200, 240, 200);
    private static final Color CLASS_HEADER     = new Color(180, 210, 255);
    private static final Color NODE_BODY        = new Color(248, 248, 248);
    private static final Color NODE_BORDER      = new Color(120, 140, 170);
    private static final Color STEREOTYPE_TEXT  = new Color(60, 100, 60);
    private static final Color EDGE             = new Color(60, 60, 60);

    @Override
    public String getThemeName() {
        return "Light";
    }

    @Override
    public Color getCanvasBackground() {
        return Color.WHITE;
    }

    @Override
    public Color getGridLineColor() {
        return GRID_LINE;
    }

    @Override
    public Color getNodeHeaderBackground(String stereotype) {
        if ("INTERFACE".equalsIgnoreCase(stereotype)) {
            return INTERFACE_HEADER;
        }
        return CLASS_HEADER;
    }

    @Override
    public Color getNodeBodyBackground() {
        return NODE_BODY;
    }

    @Override
    public Color getNodeBorderColor() {
        return NODE_BORDER;
    }

    @Override
    public Color getTextColor() {
        return Color.BLACK;
    }

    @Override
    public Color getStereotypeTextColor() {
        return STEREOTYPE_TEXT;
    }

    @Override
    public Color getEdgeColor() {
        return EDGE;
    }
}
