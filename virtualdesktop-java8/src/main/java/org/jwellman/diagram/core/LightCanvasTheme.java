package org.jwellman.diagram.core;

import java.awt.Color;

import org.jwellman.diagram.api.CanvasTheme;

/**
 * Default canvas theme: white background, light grid, blue/green node headers.
 * Matches the original hardcoded colors so existing diagrams are unaffected.
 */
public class LightCanvasTheme implements CanvasTheme {

    @Override
    public Color getCanvasBackground() {
        return Color.WHITE;
    }

    @Override
    public Color getGridLineColor() {
        return new Color(220, 220, 220);
    }

    @Override
    public Color getNodeHeaderBackground(String stereotype) {
        if ("INTERFACE".equalsIgnoreCase(stereotype)) {
            return new Color(200, 240, 200);
        }
        return new Color(180, 210, 255);
    }

    @Override
    public Color getNodeBodyBackground() {
        return new Color(248, 248, 248);
    }

    @Override
    public Color getNodeBorderColor() {
        return new Color(120, 140, 170);
    }

    @Override
    public Color getTextColor() {
        return Color.BLACK;
    }

    @Override
    public Color getEdgeColor() {
        return new Color(60, 60, 60);
    }
}
