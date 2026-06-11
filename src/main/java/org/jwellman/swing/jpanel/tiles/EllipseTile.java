package org.jwellman.swing.jpanel.tiles;

import java.awt.Graphics2D;

/**
 * Draws an ellipse.
 * <p>
 * If paint is null, the ellipse will not be filled.
 * If borderPaint is null, no outline will be drawn.
 * 
 * @author rwellman
 *
 */
public class EllipseTile extends AbstractTile {

    public EllipseTile(int w, int h) {
        super(w, h);
    }

    @Override
    public void draw(Graphics2D g2) {

        this.calcPosition();

        if (this.getPaint() != null) {
            g2.setPaint(this.getPaint());
            g2.fillOval(x, y, width, height);
        }

        if (this.getBorderPaint() != null) {
            g2.setPaint(this.getBorderPaint());
            g2.drawOval(x, y, width, height);
        }

    }

}
