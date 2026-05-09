package org.jwellman.demo.d3ish;

import java.awt.Color;

public class ColorScaleGenerator {
    /**
     * Generates a 5-step scale from a single color.
     * 0: Washout, 1: Muted, 2: Anchor (Original), 3: Deep, 4: Ink
     */
    public static Color[] generateScale(Color brand) {
        float[] hsb = Color.RGBtoHSB(brand.getRed(), brand.getGreen(), brand.getBlue(), null);
        Color[] scale = new Color[5];

        // 0: Washout - Very light, low saturation (Backgrounds)
        scale[0] = Color.getHSBColor(hsb[0], 0.05f, 0.98f);
        
        // 1: Muted - Soft version of the color
        scale[1] = Color.getHSBColor(hsb[0], hsb[1] * 0.4f, hsb[2] * 0.9f);
        
        // 2: The Anchor - The actual Brand color
        scale[2] = brand;
        
        // 3: Deep - More saturated, slightly darker (Hover states)
        scale[3] = Color.getHSBColor(hsb[0], Math.min(1.0f, hsb[1] * 1.2f), hsb[2] * 0.8f);
        
        // 4: Ink - Very dark, high saturation shift (The "Dungeon" background)
        scale[4] = Color.getHSBColor(hsb[0], Math.min(1.0f, hsb[1] * 1.5f), 0.12f);

        return scale;
    }

}
