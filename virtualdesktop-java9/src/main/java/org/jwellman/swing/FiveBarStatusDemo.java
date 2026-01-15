package org.jwellman.swing;

import java.awt.Color ;
import java.awt.Dimension ;
import java.awt.GradientPaint ;
import java.awt.Graphics ;
import java.awt.Graphics2D ;
import java.awt.Point ;
import java.awt.Rectangle ;
import java.awt.RenderingHints ;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Line2D ;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import javax.swing.JComponent ;
import javax.swing.Timer;

import org.apache.commons.lang3.RandomUtils;
import org.jwellman.swing.shapes.TriangleShape;

/**
 * A demonstration of a five bar status component.
 * 
 * Note that this class supports a "proxy" for use with beanshell scripting.
 * 
 * @author rwellman
 *
 */
public class FiveBarStatusDemo extends JComponent implements ActionListener, MouseListener {

    private int height, width;
    private int barsWidth = 25;
    private int[] values = {20,30,50,80,130};
    private int[] oldvalues = {20,30,50,80,130};
    private int[] newvalues = {20,30,50,80,130};
    private float[] stepvalues = {20,30,50,80,130};
    private PaintProxy paintProxy;
    private BufferedImage background;

    // Variables to support animation
    private int framecount;
    private Timer timer;
    private boolean animActive = false;
    private boolean timerEnabled = true;

    protected static final Color myblue = new Color(0x004892);
    protected static final Color darkblue = new Color(0x003770);
    protected static final Color lightblue = new Color(0x69E2FF);
    protected static final Color midnightblue = new Color(0x88012D5C, true);
    protected static final Color graphlineblue = new Color(0x0075C2);
    protected static final Color axisgray = new Color(0xB9D7F1);
    
    protected static final Color ledgreen = new Color(0x12FF00);
    protected static final Color ledwarning = new Color(0xFFB901);
    protected static final Color capshadow = new Color(255,255,0,32);
    protected static final Color capshadowred = new Color(255,0,0,64);
    protected static final Color tgray100 = new Color(0,0,0,0);
    protected static final Color tgray75 = new Color(0,0,0,255);
    
    // This is not intended to be made public; a flyweight to simply avoid the overhead of a new object each time.
    private Dimension dimension = new Dimension();

    private boolean debug = false;
    
    public FiveBarStatusDemo() {
        this.width = 240 + 1; // multiple of 5 + 1
        this.height = 275 + 1; // multiple of 5 + 1
        this.dimension.setSize(this.width, this.height);

        this.setBackground(myblue);
        
        this.addMouseListener(this);

        // 1000 / 20 is "twenty steps/frame per second (1000 ms)"
        this.timer = new Timer(1000 / 20, this);
        timer.setInitialDelay(0);
        timer.start();
    }
    
    @Override
    public Dimension getPreferredSize() {
        return this.dimension;
    }
    
    public void actionPerformed(ActionEvent e) {
        if (animActive) { // update oldvalues
            for (int i=0; i < 5; i++) {
                newvalues[i] = (int) ( oldvalues[i] + (stepvalues[i] * framecount) );
                if ( stepvalues[i] < 0) {
                    if (newvalues[i] < values[i]) newvalues[i] = values[i];
                } else {
                    if (newvalues[i] > values[i]) newvalues[i] = values[i];                    
                }
            }
            
            framecount++;
            if (framecount > 21) {
                animActive = false;
                timer.restart();

                for (int i=0; i < 5; i++) {
                    print("  newvalues[" + i + "] " + newvalues[i]);                    
                } println("\n-----");
            } 
            
            this.repaint();

        } else { // create new values
            
            // print the current values (which will be replaced)
            if (debug) {
                for (int i=0; i < 5; i++) {
                    print("    avalues[" + i + "] " + values[i]);
                } println("");                
            }
            
            for (int i=0; i < 5; i++) {
                // store current values before generating new values
                oldvalues[i] = values[i]; // << oldvalues is a copy of values that does not change during animation.
                newvalues[i] = values[i]; // << newvalues is updated incrementally during each frame of animation.
                
                // generate new values
                boolean old = false;
                if (old) {
                    // This just uses a random value
                    values[i] = RandomUtils.nextInt(0, 161);                    
                } else {
                    // This is random like the case above, except the random max is smaller and ...
                    if (i < 4) values[i] = RandomUtils.nextInt(0, 61);
                    // The last bar is always greater than the other four bars
                    else values[i] = values[0] + values[1] + values[2] + values[3]; 
                }
                print("    bvalues[" + i + "] " + values[i]);

                // generate the animation "step" values
                stepvalues[i] = (values[i] - oldvalues[i]) / 20f;
                // if the step value is less than 1(pixel), then set it to either 1 or -1
                // ... this makes the animation smoother
                if (Math.abs(stepvalues[i]) < 1) {
                    // determine which "direction" the new value is
                    int stepdir = (values[i] > oldvalues[i]) ? 1 : -1;                    
                    stepvalues[i] = (float)stepdir;
                }

            } println("");

            // print the calculated step values 
            if (debug) {
                for (int i=0; i < 5; i++) {
                    print(" stepvalues[" + i + "] " + stepvalues[i]);
                } println("");
            }
            
            animActive = true;
            framecount = 0;
        }
        
    }

    @Override
    public void paintComponent(Graphics g) {
        // If a proxy is defined, then use it and return.
        if (this.getPaintProxy() != null) {
            this.getPaintProxy().paintComponent(g, this);
        } else {
            this.drawGraph(g);
        }
    }
    
    @SuppressWarnings( "unused" )
    private void drawGraph(Graphics g) {

        final Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Calculate commonly used values:
        final int left = 0, right = width-1, top = 0, bottom = height-1;
        // final int midx = right/2, midy = bottom/2;

        if (background == null) this.createBackground();
        g2.drawImage(background, 0, 0, null);

        // Axes
        int barsLeftStart = 50, barsBottom = bottom-3;
        int axesWidth = 6;
        int axesLeft = barsLeftStart - 23;

        // Markers
        int x = axesLeft + axesWidth + 4 + 2;
        TriangleShape triangleShape = null;
        for (int n=0; n<5; n++) {
            int y = barsBottom - newvalues[n] - 20; // 'values' is an interesting effect // 'newvalues'
            triangleShape = new TriangleShape(
                new Point2D.Double(x, y),
                new Point2D.Double(x+5, y-4), 
                new Point2D.Double(x+5, y+4));
            g2.setColor(n < 4 ? Color.white : Color.red);
            g2.fill(triangleShape);
        }

        // Bars
        Point cursor = new Point(barsLeftStart, 0);
        for (int n=0; n<5; n++) {

            // Main Bar
            Rectangle bar = new Rectangle(cursor);
            bar.setSize(barsWidth, 20+newvalues[n]); // values
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
        
        g2.dispose();
    }
    
    @SuppressWarnings( "unused" )
    protected void createBackground() {
        println("creating background image");
        
        background = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g2 = background.createGraphics();        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Calculate commonly used values:
        final int left = 0, right = width-1, top = 0, bottom = height-1;
        // final int midx = right/2, midy = bottom/2;

        // old background
        //g2.setPaint(new GradientPaint(0, 50, darkblue, 112, 50, myblue, true));
        g2.setColor(myblue); // myblue is almost the same as: 004E90
        g2.fillRect(0, 0, this.width, this.height);

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
        int barsLeftStart = 50, barsBottom = bottom-3;
        this.drawMajorAxis(g2, barsLeftStart, barsBottom);
        this.drawMinorAxis(g2, barsLeftStart, barsBottom);
        
    }
    
    protected void drawMajorAxis(Graphics2D g2, int barsLeftStart, int barsBottom) {
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

    private void print(String msg) {
        if (debug) System.out.print(msg);
    }
    
    private void println(String msg) {
        if (debug) System.out.println(msg);
    }
    
    private static final long serialVersionUID = 1L ;

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
    
    /**
     * @return the barsWidth
     */
    public int getBarsWidth() {
        return barsWidth;
    }

    /**
     * @param barsWidth the barsWidth to set
     */
    public void setBarsWidth(int barsWidth) {
        this.barsWidth = barsWidth;
    }

    @ Override
    public void mouseClicked(MouseEvent e) {

        this.timerEnabled = !this.timerEnabled;
        
        if (timerEnabled) {
            timer.restart();
        } else {
            timer.stop();
        }
    }

    @ Override
    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @ Override
    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @ Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @ Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

}
