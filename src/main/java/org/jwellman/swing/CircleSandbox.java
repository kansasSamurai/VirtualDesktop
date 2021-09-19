package org.jwellman.swing;

import java.awt.BasicStroke ;
import java.awt.Color ;
import java.awt.Dimension ;
import java.awt.GradientPaint ;
import java.awt.Graphics ;
import java.awt.Graphics2D ;
import java.awt.Point ;
import java.awt.Rectangle ;
import java.awt.RenderingHints ;
import java.awt.Stroke ;
import java.awt.geom.Line2D ;
import java.awt.geom.Path2D ;
import java.awt.geom.Point2D ;

import javax.swing.JComponent ;

/**
 * A sandbox component for exploring Circular UI components/drawing.
 * Note that this class supports a "proxy" for use with beanshell scripting.
 * 
 * @author rwellman
 *
 */
public class CircleSandbox extends JComponent {

    private int height, width, type;
    private PaintProxy paintProxy;

    private static final Color myblue = new Color(0x004892);
    private static final Color darkblue = new Color(0x003770);
    private static final Color lightblue = new Color(0x69E2FF);
    private static final Color midnightblue = new Color(0x012D5C);
    private static final Color graphlineblue = new Color(0x0075C2);
    private static final Color axisgray = new Color(0xB9D7F1);
    
    private static final Color ledgreen = new Color(0x12FF00);
    private static final Color ledwarning = new Color(0xFFB901);
    private static final Color capshadow = new Color(255,255,0,64);
    private static final Color capshadowred = new Color(255,0,0,64);
    private static final Color tgray100 = new Color(0,0,0,0);
    private static final Color tgray75 = new Color(0,0,0,255);
    
    // This is not intended to be made public; a flyweight to simply avoid the overhead of a new object each time.
    private Dimension dimension = new Dimension();

    private boolean debug;
    
    public CircleSandbox(int type) {
        this.type = type;
        this.width = 225;
        this.height = 225;
        this.dimension.setSize(this.width, this.height);
        this.setBackground(myblue);
    }
    
    @Override
    public Dimension getPreferredSize() {
        return this.dimension;
    }
    
    @Override
    public void paintComponent(Graphics g) {

        // If a proxy is defined, then use it and return.
        if (this.getPaintProxy() != null) {
            this.getPaintProxy().paintComponent(g, this);
            return;
        }
        
        switch(type) {
            case 1: this.drawCircles(g); break;
            case 2: this.drawGraph(g); break;
        }

    }
    
    private void drawGraph(Graphics g) {

        final Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Calculate commonly used values:
        final int left = 0, right = width-1, top = 0, bottom = height-1;
        final int midx = right/2, midy = bottom/2;

        // old background
        //g2.setPaint(new GradientPaint(0, 50, darkblue, 112, 50, myblue, true));
        g2.setColor(myblue); // myblue is almost the same as: 004E90
        g2.fillRect(0, 0, this.width, this.height);
        
        Rectangle bounds = new Rectangle(0,0,this.width,this.height);

        g2.setColor(graphlineblue);
        Line2D line = new Line2D.Double(0, 0, 0, height);
        // Vertical Lines   
        for (int x=0; x < width; x+=5) {
            line.setLine(x, 0, x, height);
            g2.draw(line);
        }
        // Horizontal Lines   
        for (int y=0; y < height; y+=5) {
            line.setLine(0, y, width, y);
            g2.draw(line);
        }

        // Gradient Highlight/Overlay
        g2.setPaint(new GradientPaint(0, 0, tgray100, width, height, tgray75, false));
        g2.fillRect(0, 0, this.width, this.height);

        // Axes
        int barsLeftStart = 50, barsBottom = bottom-3, barsWidth = 25;
        this.drawMajorAxis(g2, barsLeftStart, barsBottom);
        this.drawMinorAxis(g2, barsLeftStart, barsBottom);
        
        // Bars
        Point cursor = new Point(barsLeftStart, 0);
        int[] values = {20,30,50,80,130};
        for (int n=0; n<5; n++) {

            // Main Bar
            Rectangle bar = new Rectangle(cursor);
            bar.setSize(barsWidth, 20+values[n]);
            bar.translate(0, barsBottom - bar.height);            
            g2.setColor(midnightblue);
            g2.fill(bar);

            // Bar Cap
            Rectangle cap = new Rectangle(bar);
            cap.setSize(barsWidth, 10);
            g2.setColor(n < 4 ? Color.yellow : Color.red);
            g2.fill(cap);            

            // Bar Cap Shadow
            cap.translate(0, 10);
            g2.setColor(n < 4 ? capshadow : capshadowred);
            g2.fill(cap);
        
            cursor.translate(barsWidth, 0);            
        }
        
    }
    
    public void drawMajorAxis(Graphics2D g2, int barsLeftStart, int barsBottom) {
        int axesLeft = barsLeftStart - 26;
        int axesWidth = 21;
        g2.setColor(axisgray);

        // Axes Bottom
        Point axes = new Point(axesLeft, barsBottom-1);
        Point p1 = new Point(axes); p1.translate(axesWidth, 0);
        Line2D line = new Line2D.Double(axes, p1);
        g2.draw(line);
        
        // Axes Top
        Point p2 = new Point(axes); 
            p2.translate(0, -2*100);
            p1.translate(0, -2*100);
        line.setLine(p2, p1);
        g2.draw(line);

        // Axes Left
        line.setLine(axes,p2);
        g2.draw(line);
            p2.translate(1,0);
            axes.translate(1,0);
        line.setLine(axes,p2);
        g2.draw(line);    
    }
  
    public void drawMinorAxis(Graphics2D g2, int barsLeftStart, int barsBottom) {
        int axesWidth = 6;
        int axesLeft = barsLeftStart - 23;
        int axesBottom = barsBottom - 3;
        int axesTop = axesBottom - (2*99);
        g2.setColor(lightblue);

        Point origin = new Point(axesLeft, barsBottom-3);

        // Horizontal Lines   
        Point p1 = new Point(origin); p1.translate(1,0);
        Point p2 = new Point(p1); p2.translate(axesWidth-2,0);
        Line2D line = new Line2D.Double(p1, p2); int n = 0;
        for (int y=axesBottom; y > axesTop; y -= 2) {
            n++;
            if (n%5 == 0) { p2.translate(4,0); line.setLine(p1,p2); }
                g2.draw(line);
            if (n%5 == 0) p2.translate(-4,0);
                
            p1.translate(0,-2);
            p2.translate(0,-2);
            line.setLine(p1, p2);
        }

  }

    private void drawCircles(Graphics g) {
        final Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Stroke defaultstroke = g2.getStroke(); // store the default stroke
        Stroke doublestroke = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

        // Calculate commonly used values:
        final int left = 0, right = width-1, top = 0, bottom = height-1;
        final int midx = right/2, midy = bottom/2;
        
        g2.setPaint(new GradientPaint(0, 50, darkblue, 112, 50, myblue, true));
        g2.fillRect(0, 0, this.width, this.height);
        
        // this is "debug" artifacts; turn off for production
        if (this.debug) {
            g2.setColor(Color.red);
            g2.drawRect(left, top, right, bottom);
            g2.drawLine(midx, top, midx, bottom);
            g2.drawLine(left, midy, right, midy);            
        }

        g2.setColor(ledgreen);
        g2.drawLine(midx-3, top+2, midx-3, top+2+6);
        g2.drawLine(midx+3, top+2, midx+3, top+2+6);
        g2.drawLine(midx-3, bottom-3, midx-3, bottom-3-6);
        g2.drawLine(midx+3, bottom-3, midx+3, bottom-3-6);
        
        g2.setStroke(doublestroke);
            g2.drawArc(5, 5, right-10, bottom-10,  60, 28);
            g2.drawArc(5, 5, right-10, bottom-10,  92, 28);
            g2.drawArc(5, 5, right-10, bottom-10, 270-30, 28);
            g2.drawArc(5, 5, right-10, bottom-10, 272, 28);
        g2.setStroke(defaultstroke);

        g2.setColor(Color.white);
        g2.drawArc(5, 5, right-10, bottom-10,  90+30, 120);
        g2.drawArc(5, 5, right-10, bottom-10, 270+30, 120);
        // This draws an overlay:  // 270, 180); 
        // This draws "normal":  // 300, 120);
    }
    
    /**
     * @return the paintProxy
     */
    public PaintProxy getPaintProxy() {
        return paintProxy ;
    }

    /**
     * @param paintProxy the paintProxy to set
     */
    public void setPaintProxy(PaintProxy paintProxy) {
        this.paintProxy = paintProxy ;
    }
    
    private static final long serialVersionUID = 1L ;

}
