package org.jwellman.diagram.api;

import java.awt.Color;

/**
 * Visual attributes for a graph edge.
 */
public class EdgeAttributes {

    public enum LineStyle { SOLID, DASHED }

    public enum ArrowType { OPEN, FILLED, NONE }

    private LineStyle lineStyle = LineStyle.SOLID;
    private ArrowType arrowType = ArrowType.FILLED;
    private Color color = Color.DARK_GRAY;
    private float strokeWidth = 1.5f;

    public LineStyle getLineStyle() {
        return lineStyle;
    }

    public void setLineStyle(LineStyle lineStyle) {
        this.lineStyle = lineStyle;
    }

    public ArrowType getArrowType() {
        return arrowType;
    }

    public void setArrowType(ArrowType arrowType) {
        this.arrowType = arrowType;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
    }
}
