package org.jwellman.demo.d3ish;

import java.awt.Color;

public class ColorInterpolator {

    public static Color interpolate(Color start, Color end, double fraction) {
        int r = (int) (start.getRed() + (end.getRed() - start.getRed()) * fraction);
        int g = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * fraction);
        int b = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * fraction);
        int a = (int) (start.getAlpha() + (end.getAlpha() - start.getAlpha()) * fraction);
        return new Color(r, g, b, a);
    }

}
