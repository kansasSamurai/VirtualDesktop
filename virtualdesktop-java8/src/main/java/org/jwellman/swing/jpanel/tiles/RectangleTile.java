package org.jwellman.swing.jpanel.tiles;

import java.awt.Graphics2D;

/**
 * Draws a rectangle.
 * <p>
 * If paint is null, the rectangle will not be filled.
 * If borderPaint is null, no outline will be drawn.
 * 
 * @author rwellman
 *
 */
public class RectangleTile extends AbstractTile {

    public RectangleTile(int w, int h) {
        super(w, h);
    }

    @Override
    public void draw(Graphics2D g2) {

        this.calcPosition();

        if (this.getPaint() != null) {
            g2.setPaint(this.getPaint());
            g2.fillRect(x, y, width, height);
        }

        if (this.getBorderPaint() != null) {
            g2.setPaint(this.getBorderPaint());
            g2.drawRect(x, y, width, height);
        }
    }

}
