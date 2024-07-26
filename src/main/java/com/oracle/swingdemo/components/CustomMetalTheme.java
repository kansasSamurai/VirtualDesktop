package com.oracle.swingdemo.components;

import java.awt.Color;

import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;

public class CustomMetalTheme extends DefaultMetalTheme {
    
    private static ColorUIResource white = new ColorUIResource(Color.green);

    protected ColorUIResource getWhite() { return white; }

}
