package org.jwellman.swing.icon;

import java.awt.Color ;
import java.awt.Component ;
import java.awt.Graphics ;
import java.awt.Graphics2D ;
import java.awt.Insets ;
import java.awt.RenderingHints ;

import javax.swing.Icon ;

/**
 * 
 * @author rwellman
 *
 */
public class ColorIcon implements Icon {
    
    private static final Insets NONE = new Insets(0,0,0,0);
    
    private int width;
    private int height;
    private Color color;
    private Color border = Color.black;
    private Insets margin = NONE;    
    private boolean roundedCorners = false;
    
    /**
     * Create a color swatch icon of the given color whose dimensions
     * are a square (size x size).
     * 
     * @param c
     * @param size
     */
    public ColorIcon (Color c, int size) {
        this(c, size, size);
    }

    /**
     * Create a color swatch icon of the given color and dimensions.
     * (w x h)
     * 
     * @param c
     * @param w
     * @param h
     */
    public ColorIcon(Color c, int w, int h) {
        this.color = c;
        this.width = w;
        this.height = h;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        final Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        
        final int w = this.width; 
        final int h = this.height;
        
        g2.setColor(this.color);
        if (this.roundedCorners)
            g2.fillRoundRect(x+margin.left, y+margin.top, w, h, h-2, h-2);
        else
            g2.fillRect(x+margin.left, y+margin.top, w, h);
        
        g2.setColor(this.border);
        if (this.roundedCorners)
            g2.drawRoundRect(x+margin.left, y+margin.top, w, h, h-2, h-2);
        else
            g2.drawRect(x+margin.left, y+margin.top, w, h);

        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return width + margin.left + margin.right; 
    }

    @Override
    public int getIconHeight() {
        return height + margin.top + margin.bottom;
    }

    /**
     * @return the border color
     */
    public Color getBorder() {
        return border;
    }

    /**
     * 
     * @param the border color
     */
    public void setBorder(Color border) {
        this.border = border;
    }
    
    /**
     * @return the margin
     */
    public Insets getMargin() {
        return margin ;
    }

    /**
     * @param margin the margin to set
     */
    public void setMargin(Insets margin) {
        this.margin = margin ;
    }

    /**
     * @return whether roundedCorners are enabled
     */
    public boolean isRoundedCorners() {
        return roundedCorners;
    }

    /**
     * @param value true to enable rounded corners; otherwise, false.
     */
    public void setRoundedCorners(boolean value) {
        this.roundedCorners = value;
    }

    /**
     * @return the color
     */
    public Color getColor() {
        return color;
    }

    /**
     * @param color the color to set
     */
    public void setColor(Color color) {
        this.color = color;
    }

}
