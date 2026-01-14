package org.jwellman.swing.jpanel;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

/**
 * GradientPanel is a class with a gradient background.
 *
 * This was taken from swing-utils but then improved; it needs
 * to be added back to swing-utils when it is ready.
 * 
 * @author rwellman
 */
public class GradientPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	/** Starting Gradient Color. */
    private Color startColor;
    
    /** Ending Gradient Color. */
    private Color endColor;
    
    /** direction of gradient; use DIRECTION_TOPDOWN | DIRECTION_LEFTRIGHT */
    private int direction;

    public static final int DIRECTION_TOPDOWN = 0;
    public static final int DIRECTION_LEFTRIGHT = 1;

    public static final Color HEADER_COLOR_START = new Color(0x830401);
    public static final Color HEADER_COLOR_END = new Color(0xDD5731);
    public static final Color WIZARD_COLOR_START = new Color(0x000080);
    public static final Color WIZARD_COLOR_END = new Color(0x2179DA);
    public static final Color WARNING_COLOR_START = new Color(0xE80000);
    public static final Color WARNING_COLOR_END = new Color(0x000000);
    
    public static final Color SELECTED_GRID_CELL_BG_COLOR = new Color(0xE2F5FE);

    /**
     * Constructor supplying a color.
     * Default gradient is top to bottom.
     * 
     * @param startColor
     * @param endColor
     */
    public GradientPanel( Color start , Color end ) {
        super();
        this.setOpaque(true);
        this.setBackground(Color.blue);
        
        this.startColor = start;
        this.endColor = end;
        this.direction = DIRECTION_TOPDOWN;
    }

    @Override protected void paintComponent( Graphics g ) {
        super.paintComponent( g );
        
        int panelHeight = getHeight();
        int panelWidth = getWidth();
        GradientPaint gradientPaint = new GradientPaint( panelWidth / 2 , 0 , startColor , panelWidth / 2 , panelHeight , endColor );
        if( g instanceof Graphics2D ) {
        }
        Graphics2D graphics2D = (Graphics2D)g;
        graphics2D.setPaint( gradientPaint );
        graphics2D.fillRect( 0 , 0 , panelWidth , panelHeight );
    }

    /**
     * @return the direction
     */
    public int getDirection() {
        return direction;
    }

    /**
     * @param direction the direction to set
     */
    public void setDirection(int direction) {
        this.direction = direction;
    }

}
