package org.jwellman.diagram;

import javax.swing.JComponent;

/**
 * Shape component data
 */
public class ShapeData extends ComponentData {

    private String shapeType;
    private String fillColor;
    private String borderColor;
    private int cornerRadius = 10;

    // Default constructor required for Jackson
    public ShapeData() {
    }

    public ShapeData(DiagramShape shape) {
        setBounds(shape.getBounds());
        this.shapeType = shape.getShapeType().name();
        this.fillColor = colorToHex(shape.getFillColor());
        this.borderColor = colorToHex(shape.getBorderColor());
        this.cornerRadius = shape.getCornerRadius();
    }

    public String getShapeType() {
        return shapeType;
    }

    public void setShapeType(String shapeType) {
        this.shapeType = shapeType;
    }

    public String getFillColor() {
        return fillColor;
    }

    public void setFillColor(String fillColor) {
        this.fillColor = fillColor;
    }

    public String getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(String borderColor) {
        this.borderColor = borderColor;
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    @Override
    public JComponent createComponent() {
        DiagramShape shape = new DiagramShape(ShapeType.valueOf(shapeType));
        shape.setBounds(getBounds());
        shape.setFillColor(hexToColor(fillColor));
        shape.setBorderColor(hexToColor(borderColor));
        shape.setCornerRadius(cornerRadius);
        return shape;
    }

}
