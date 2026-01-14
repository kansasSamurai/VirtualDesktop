package org.jwellman.demo.layereddiagramtool;

import java.awt.Color;
import java.awt.Rectangle;

import javax.swing.JComponent;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base class for component data with polymorphic JSON serialization
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @JsonSubTypes.Type(value = ShapeData.class, name = "shape"),
        @JsonSubTypes.Type(value = TextData.class, name = "text") })
public abstract class ComponentData {
    private int x;
    private int y;
    private int width;
    private int height;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public abstract JComponent createComponent();

    protected void setBounds(Rectangle bounds) {
        this.x = bounds.x;
        this.y = bounds.y;
        this.width = bounds.width;
        this.height = bounds.height;
    }

    protected Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    protected String colorToHex(Color color) {
        return String.format("#%02X%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    protected Color hexToColor(String hex) {
        if (hex.length() == 9) { // #RRGGBBAA
            return new Color(Integer.parseInt(hex.substring(1, 3), 16), Integer.parseInt(hex.substring(3, 5), 16),
                    Integer.parseInt(hex.substring(5, 7), 16), Integer.parseInt(hex.substring(7, 9), 16));
        } else { // #RRGGBB
            return Color.decode(hex);
        }
    }

}
