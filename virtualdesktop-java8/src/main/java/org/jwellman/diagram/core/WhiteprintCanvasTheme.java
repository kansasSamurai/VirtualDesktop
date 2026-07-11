package org.jwellman.diagram.core;

import java.awt.Color;

import org.jwellman.diagram.api.CanvasTheme;

/**
 * Inverted blueprint aesthetic — technical drawing on drafting paper.
 *
 * Off-white "surface" ground with blue ink, as if the cyanotype process were
 * run in reverse: where blueprint exposes blue on blue-dark, whiteprint lays
 * blue ink on a slightly cool off-white sheet.
 *
 * Canvas background: #F0F4FA (cool off-white — drafting surface, not stark white)
 * Grid:             #D8E5F5 (pale blue-grey — subtle drafting rule lines)
 * Node headers:     #E4EEF8 (light blue tint — just enough to mark the header band)
 * Node body:        #F8FBFE (near-white — distinguished from header without contrast shock)
 * Borders / edges:  #3A6BA5 (ink blue — the pen stroke on drafting paper)
 * Text:             #1A3E70 (dark navy — readable, authoritative, clearly blueprint-family)
 * Stereotype label: #4A7ABF (steel blue — lighter than text, still clearly blue)
 */
public class WhiteprintCanvasTheme implements CanvasTheme {

    private static final Color OFF_WHITE    = new Color(0xF0, 0xF4, 0xFA); // #F0F4FA — drafting surface
    private static final Color GRID_TINT    = new Color(0xD8, 0xE5, 0xF5); // #D8E5F5 — rule lines
    private static final Color HEADER_TINT  = new Color(0xE4, 0xEE, 0xF8); // #E4EEF8 — header band
    private static final Color BODY_WHITE   = new Color(0xF8, 0xFB, 0xFE); // #F8FBFE — node body
    private static final Color INK_BLUE     = new Color(0x3A, 0x6B, 0xA5); // #3A6BA5 — borders + edges
    private static final Color DARK_NAVY    = new Color(0x1A, 0x3E, 0x70); // #1A3E70 — primary text
    private static final Color STEEL_BLUE   = new Color(0x4A, 0x7A, 0xBF); // #4A7ABF — stereotype label

    @Override
    public String getThemeName() {
        return "Whiteprint";
    }

    @Override
    public Color getCanvasBackground() {
        return OFF_WHITE;
    }

    @Override
    public Color getGridLineColor() {
        return GRID_TINT;
    }

    @Override
    public Color getNodeHeaderBackground(String stereotype) {
        return HEADER_TINT;
    }

    @Override
    public Color getNodeBodyBackground() {
        return BODY_WHITE;
    }

    @Override
    public Color getNodeBorderColor() {
        return INK_BLUE;
    }

    @Override
    public Color getTextColor() {
        return DARK_NAVY;
    }

    @Override
    public Color getStereotypeTextColor() {
        return STEEL_BLUE;
    }

    @Override
    public Color getEdgeColor() {
        return INK_BLUE;
    }
}
