package org.jwellman.diagram;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JComponent;

/**
 * Text component data
 */
public class TextData extends ComponentData {

    private String fillColor;
    private String borderColor = "#707070FF"; // default

    private int fontSize = 16;
    private int fontStyle = Font.BOLD;
    private String fontName = "Segoe UI";

    private String text;
    private String textColor;

    // Default constructor required for Jackson
    public TextData() {
    }

    public TextData(DiagramText textComp) {
        setBounds(textComp.getBounds());
        this.text = textComp.getText();
        this.fillColor = colorToHex(textComp.getFillColor());
        this.borderColor = colorToHex(textComp.getBorderColor());
        this.fontName = textComp.getFontName();
        this.fontSize = textComp.getFontSize();
        this.fontStyle = textComp.getFontStyle();
        this.textColor = colorToHex(textComp.getTextColor());
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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

    public String getFontName() {
        return fontName;
    }

    public void setFontName(String fontName) {
        this.fontName = fontName;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public int getFontStyle() {
        return fontStyle;
    }

    public void setFontStyle(int fontStyle) {
        this.fontStyle = fontStyle;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    @Override
    public JComponent createComponent() {
        DiagramText textComp = new DiagramText(text);
        textComp.setBounds(getBounds());
        textComp.setFillColor(hexToColor(fillColor));
        textComp.setBorderColor(hexToColor(borderColor));
        textComp.setFontName(fontName);
        textComp.setFontSize(fontSize);
        textComp.setFontStyle(fontStyle);
        textComp.setTextColor(Color.black); // (hexToColor(textColor));
        return textComp;
    }

}
