package org.jwellman.demo.swing;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import org.kordamp.jsilhouette.geom.Donut;

/**
 *
 * @author Rick
 */
public class DonutPanelDemo extends JPanel {

    @Override
    public void paint( Graphics g ) {
            Donut donut = new Donut( 50, 50, 40, 10, 2);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(
               RenderingHints.KEY_ANTIALIASING,
               RenderingHints.VALUE_ANTIALIAS_ON
            );
            Rectangle bounds = getBounds();
            g2d.setBackground(Color.WHITE);
            g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            g2d.setColor(Color.BLUE);
            g2d.fill(donut);
            g2d.setColor(Color.BLACK);
            g2d.draw(donut);
            g2d.drawRect(bounds.x, bounds.y, bounds.width-1, bounds.height-1);
         }

}
