package org.jwellman.demo.d3ish;

import java.awt.Color;

public class StyleRegistry {

    // A collection of "Foundation Palettes"
    private static final Color[][] PALETTES = {
        {new Color(74, 144, 226), new Color(80, 227, 194), new Color(155, 126, 255)}, // Blue/Teal/Purple
        {new Color(255, 95, 109), new Color(255, 195, 113), new Color(200, 80, 192)}, // Sunset/Warm
        {new Color(43, 43, 43), new Color(85, 85, 85), new Color(170, 170, 170)},     // Forensic Monochrome
        {new Color(106, 17, 203), new Color(37, 117, 252), new Color(0, 212, 255)}   // Deep Sea
    };

    private static int currentPaletteIndex = 0;
    private static CircularArrayIterator<Color> activePalette = new CircularArrayIterator<>(PALETTES[0]);

    public static CircularArrayIterator<Color> getActivePalette() {
        return activePalette;
    }

    public static void rotatePalette() {
        currentPaletteIndex = (currentPaletteIndex + 1) % PALETTES.length;
        activePalette = new CircularArrayIterator<>(PALETTES[currentPaletteIndex]);
    }

}
