package org.jwellman.swing.indicator;

import java.awt.Color ;
import java.awt.Dimension ;
import java.awt.Graphics ;
import java.awt.Graphics2D ;
import java.awt.RenderingHints ;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;
import javax.swing.Timer;

import org.apache.commons.lang3.RandomUtils;

/**
 * This is a "generic" version of Sandbox2;
 * Its intent is to be a true working component.
 * <p>
 * Design goals:<br>
 * a) transparent background by default (to allow any background to show through)<br>
 * b) number,color, dimension of "normal" bars<br>
 * c) number,color, dimension of "warning" bars<br>
 * d) spacing between bars<br>
 * 
 * 
 * @author rwellman
 *
 */
public class LEDBars extends JComponent implements ActionListener, MouseListener {

    private static final long serialVersionUID = 1L ;

    private int height, width;
    
    private int gap;

    private int totalNormalBars;
    
    private int totalWarningBars;
    
    private Color colorNormalBars;
    
    private Color colorWarningBars;
    
    private Color colorOff;
    
    private Dimension barDimension = new Dimension();;

    // Variables to support animation
    private Timer timer;
    private boolean timerEnabled = true;
    private int animValue, animMax;

    public static final Color LED_GREEN = new Color(0x12FF00);
    public static final Color LED_YELLOW = new Color(0xFFB901);
    public static final Color LED_MIDNIGHTBLUE = new Color(0x012D5C);

    // This is not intended to be made public; a flyweight to simply avoid the overhead of a new object each time.
    private Dimension dimension = new Dimension();
    
    /**
     * Default constructor using default colors and dimensions.
     */
    public LEDBars() {
        this.setOpaque(false); 

        this.addMouseListener(this);
        
        this.totalNormalBars = 10;
        this.totalWarningBars = 5;

        this.colorOff = LED_MIDNIGHTBLUE;
        this.colorNormalBars = LED_GREEN;
        this.colorWarningBars = LED_YELLOW;

        this.barDimension.width = 4;
        this.barDimension.height = 10;

        this.gap = 1;
        
        animValue = 0;
        animMax = RandomUtils.nextInt(0, 15);

        this.calcSize();

        // 1000 / 5 is "five steps/frame per second (1000 ms)"
        this.timer = new Timer(1000 / 20, this);
        timer.setInitialDelay(0);
        timer.start();

    }
    
    private void calcSize() {
        this.width = 
            (this.barDimension.width + this.gap) * this.totalNormalBars  // Normal Bars
          + (this.barDimension.width + this.gap) * this.totalWarningBars  // Warning Bars
          - this.gap // remove the last gap
            ;

        this.height = this.barDimension.height;

        this.dimension.setSize(this.width, this.height);
        
        System.out.println("w: " + this.width + ", h: " + this.height);
    }

    @Override
    public void paintComponent(Graphics g) {
        final Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (this.isOpaque()) {
            g2.setColor(this.getBackground());
            g2.fillRect(0,0,this.width,this.height);            
        }
        
        final int spacing = this.barDimension.width + this.gap;

        // ===== Normal Bars =====
        int x=Math.negateExact(spacing), y=0;
        for (int n = 0; n < this.totalNormalBars; n++) {
            g2.setColor((n < animValue) ? this.colorNormalBars : this.colorOff );
            g2.fillRect(x += spacing, y, this.barDimension.width, this.barDimension.height);
        }

        // ===== Warning Bars =====
        for (int n = 0; n < this.totalWarningBars; n++) {
            g2.setColor(((n+this.totalNormalBars) < animValue) ? this.colorWarningBars : this.colorOff );
            g2.fillRect(x += spacing, y, this.barDimension.width, this.barDimension.height);
        }

        g2.dispose();
    }
    
    @Override
    public Dimension getPreferredSize() {
        System.out.print(".gps");
        return this.dimension;
    }

    @Override
    public Dimension getMinimumSize() {
        System.out.print(".gms");
        return this.dimension;
    }

    @Override
    public Dimension getMaximumSize() {
        System.out.print(".gxs");
        return this.dimension;
    }

    @Override
    public void actionPerformed(ActionEvent e) {        
        this.repaint();

        // ===== reset value
        if (this.timerEnabled) {
            if (++animValue > animMax) {
                animValue = 0;
                animMax = RandomUtils.nextInt(0, 15);
                /* Technically, this should be the sum of normal bars
                 * plus warning bars.  However, for the demo, it actually
                 * looks better leaving it at a number larger than the sum.
                 * This might be a future enhancement.
                 * 
                 */
            }            
        }

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

    /**
     * @return the gap
     */
    public int getGap() {
        return gap;
    }

    /**
     * @param gap the gap to set
     */
    public void setGap(int gap) {
        this.gap = gap;
        this.calcSize();
    }

    /**
     * @return the totalNormalBars
     */
    public int getTotalNormalBars() {
        return totalNormalBars;
    }

    /**
     * @param totalNormalBars the totalNormalBars to set
     */
    public void setTotalNormalBars(int totalNormalBars) {
        this.totalNormalBars = totalNormalBars;
        this.calcSize();
    }

    /**
     * @return the totalWarningBars
     */
    public int getTotalWarningBars() {
        return totalWarningBars;
    }

    /**
     * @param totalWarningBars the totalWarningBars to set
     */
    public void setTotalWarningBars(int totalWarningBars) {
        this.totalWarningBars = totalWarningBars;
        this.calcSize();
    }

    /**
     * @return the colorNormalBars
     */
    public Color getColorNormalBars() {
        return colorNormalBars;
    }

    /**
     * @param colorNormalBars the colorNormalBars to set
     */
    public void setColorNormalBars(Color colorNormalBars) {
        this.colorNormalBars = colorNormalBars;
    }

    /**
     * @return the colorWarningBars
     */
    public Color getColorWarningBars() {
        return colorWarningBars;
    }

    /**
     * @param colorWarningBars the colorWarningBars to set
     */
    public void setColorWarningBars(Color colorWarningBars) {
        this.colorWarningBars = colorWarningBars;
    }

    /**
     * @return the colorOff
     */
    public Color getColorOff() {
        return colorOff;
    }

    /**
     * @param colorOff the colorOff to set
     */
    public void setColorOff(Color colorOff) {
        this.colorOff = colorOff;
    }

    /**
     * @param w the bar width
     */
    public void setBarWidth(int w) {
        this.barDimension.width = w;
        this.calcSize();
    }
    
    /**
     * @param h the bar height
     */
    public void setBarHeight(int h) {
        this.barDimension.height = h;
        this.calcSize();
    }

}
