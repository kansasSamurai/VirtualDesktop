package org.jwellman.diagram.api;

import java.awt.Color;

/**
 * Visual attributes for a graph edge.
 */
public class EdgeAttributes {

    public enum LineStyle { SOLID, DASHED }

    public enum ArrowType { OPEN, FILLED, NONE, OPEN_DIAMOND, FILLED_DIAMOND }

    private LineStyle lineStyle       = LineStyle.SOLID;
    private ArrowType arrowType       = ArrowType.FILLED;
    private ArrowType sourceArrowType = ArrowType.NONE;
    private Color color               = null; // null = use theme default; non-null overrides
    private float strokeWidth         = 1.5f;

    public EdgeAttributes() {}

    /** Copy constructor — produces an independent instance with the same values. */
    public EdgeAttributes(EdgeAttributes other) {
        this.lineStyle       = other.lineStyle;
        this.arrowType       = other.arrowType;
        this.sourceArrowType = other.sourceArrowType;
        this.color           = other.color;       // Color is immutable; reference copy is safe
        this.strokeWidth     = other.strokeWidth;
    }

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

    public ArrowType getSourceArrowType() {
        return sourceArrowType;
    }

    public void setSourceArrowType(ArrowType sourceArrowType) {
        this.sourceArrowType = sourceArrowType;
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
    }
}
