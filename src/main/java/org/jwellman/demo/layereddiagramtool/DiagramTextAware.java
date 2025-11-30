package org.jwellman.demo.layereddiagramtool;

import java.awt.Color;

/**
 * Interface for diagram components that support text properties
 */
public interface DiagramTextAware {
    String getFontName();
    void setFontName(String fontName);
    int getFontSize();
    void setFontSize(int fontSize);
    int getFontStyle();
    void setFontStyle(int fontStyle);
    Color getTextColor();
    void setTextColor(Color color);
}
