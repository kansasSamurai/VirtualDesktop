package org.jwellman.diagram.core;

import java.awt.Color;

import org.jwellman.diagram.api.CanvasTheme;

/**
 * Classic 19th-century cyanotype blueprint aesthetic.
 *
 * Prussian blue canvas with crisp white lines and lettering — evoking the
 * look of a structural draft on the drawing board. The palette signals
 * "architecture in progress" to stakeholders rather than a finished commitment.
 *
 * Canvas background: #003366 (Prussian Blue)
 * Grid:             #004080 (subtle brighter wash — drafting-paper texture)
 * Node headers:     #002b54 (deep shadow inset — structural hierarchy)
 * Node body:        #003a75 (slightly lighter wash — distinguishes the card)
 * Borders / edges:  #C8DCFF (pale blueprint blue — matches text and edges)
 * Text:             #C8DCFF (pale blueprint blue — drafted lettering with a blue tint)
 * Stereotype label: #E6F2FF (ice blue — lighter tint to distinguish from body text)
 */
public class BlueprintCanvasTheme implements CanvasTheme {

    private static final Color PRUSSIAN_BLUE  = new Color(0x00, 0x33, 0x66); // #003366
    private static final Color GRID_BLUE      = new Color(0x00, 0x40, 0x80); // #004080
    private static final Color HEADER_BLUE    = new Color(0x00, 0x2b, 0x54); // #002b54
    private static final Color BODY_BLUE      = new Color(0x00, 0x3a, 0x75); // #003a75
    private static final Color PALE_BLUE      = new Color(0xC8, 0xDC, 0xFF); // #C8DCFF — primary drafted text
    private static final Color ICE_BLUE       = new Color(0xE6, 0xF2, 0xFF); // #E6F2FF — stereotype label (lighter tint)

    @Override
    public Color getCanvasBackground() {
        return PRUSSIAN_BLUE;
    }

    @Override
    public Color getGridLineColor() {
        return GRID_BLUE;
    }

    @Override
    public Color getNodeHeaderBackground(String stereotype) {
        return HEADER_BLUE;
    }

    @Override
    public Color getNodeBodyBackground() {
        return BODY_BLUE;
    }

    @Override
    public Color getNodeBorderColor() {
        return PALE_BLUE;
    }

    @Override
    public Color getTextColor() {
        return PALE_BLUE;
    }

    @Override
    public Color getStereotypeTextColor() {
        return ICE_BLUE;
    }

    @Override
    public Color getEdgeColor() {
        return PALE_BLUE;
    }
}
