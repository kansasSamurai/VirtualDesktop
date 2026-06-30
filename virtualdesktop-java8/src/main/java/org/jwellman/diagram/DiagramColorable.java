package org.jwellman.diagram;

import java.awt.Color;

/**
 * Interface for diagram components that support fill and border colors
 */
public interface DiagramColorable {
    Color getFillColor();
    void setFillColor(Color color);
    Color getBorderColor();
    void setBorderColor(Color color);
}
