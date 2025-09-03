package org.jwellman.swing.jpanel;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPanel;

import org.jfree.chart.ui.Drawable;

/**
 * A generic JPanel that serves as a drawing canvas.
 * 
 * Although destined to become part of swing utils, this
 * is fully intended to support prototyping and experimentation
 * using beanshell.  Thus, it supports a proxy ...
 * 
 * I am going to use JFreeChart Drawable interface for convenience.
 * I may have to duplicate that later so as to remove the dependency
 * but for jvd right now it is fine.
 * 
 * @author rwellman
 *
 */
public class DrawingSurface extends JPanel implements Drawable {

    // Expose a proxy for beanshell.
    public Object proxy;

    public static RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    static {
        rh.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        rh.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
    }

    // Not typically overridden but public just in case.
    public void _doDrawing(Graphics g, Rectangle2D area) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHints(rh);

        if (proxy != null) {
            ((Drawable)proxy).draw(g2d, area);
        } else {
            this.draw(g2d, area);
        }
    }

    @Override
    public void draw(Graphics2D g2d, Rectangle2D area) {
        // just some example drawing primitives to serve
        // as an example; this method is meant to be overridden.
        g2d.setPaint(new Color(150, 150, 150));

        g2d.fillRect(30, 20, 50, 50);
        g2d.fillRect(120, 20, 90, 60);
        g2d.fillRoundRect(250, 20, 70, 60, 25, 25);

        g2d.fill(new Ellipse2D.Double(10, 100, 80, 100));
        g2d.fillArc(120, 130, 110, 100, 5, 150);
        g2d.fillOval(270, 130, 50, 50);

        g2d.fill( new RoundRectangle2D.Double(10, 100, 0,60, 25,25));
    }

    @Override
    public void paintComponent(Graphics g) {

        // calls the panels paint method
        super.paintComponent(g);

        // calls this class to do custom drawing
        _doDrawing(g, this.getBounds());

    }

}
