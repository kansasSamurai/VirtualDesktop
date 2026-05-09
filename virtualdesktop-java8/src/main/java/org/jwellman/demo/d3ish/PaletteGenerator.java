package org.jwellman.demo.d3ish;

import java.awt.Color;

public class PaletteGenerator {

    /**
     * Derives a 5-step scale: 0 (Washout) to 4 (Ink)
     * This ensures the "Dungeon of Doom" background still feels related to the brand.
     */
    public static Color[] generateScale(Color brand) {
        float[] hsb = Color.RGBtoHSB(brand.getRed(), brand.getGreen(), brand.getBlue(), null);
        Color[] scale = new Color[5];

        // 0: Washout (High brightness, very low saturation - for Light Mode backgrounds)
        scale[0] = Color.getHSBColor(hsb[0], 0.05f, 0.98f);
        
        // 1: Muted (Subtle version of the color)
        scale[1] = Color.getHSBColor(hsb[0], hsb[1] * 0.3f, hsb[2] * 0.9f);
        
        // 2: Anchor (The original Level 0 Brand color)
        scale[2] = brand;
        
        // 3: Deep (Higher saturation, lower brightness - for hover/active states)
        scale[3] = Color.getHSBColor(hsb[0], Math.min(1.0f, hsb[1] * 1.2f), hsb[2] * 0.7f);
        
        // 4: Ink (The "Dungeon" color - low brightness, high saturation shift)
        scale[4] = Color.getHSBColor(hsb[0], Math.min(1.0f, hsb[1] * 1.4f), 0.12f);

        return scale;
    }

}
