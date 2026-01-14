package org.jwellman.jfreechart.editor;

import java.awt.Color;

/**
 * A simple extension to the Color class that allows us
 * to add a name to a color for use in user interface selection tools.
 * <p>
 * A potential alternate name for this class is simply 'NamedColor'.
 * 
 * @author rwellman
 *
 */
public class Shade extends Color {

    private String name;

    private static final long serialVersionUID = 1L;

    public Shade(int rgb, String name) {
        super(rgb);
        this.name = name;
    }

    public static Shade decode(String hex, String name) throws NumberFormatException {
        Color c = Color.decode(hex);
        return new Shade(c.getRGB(), name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
