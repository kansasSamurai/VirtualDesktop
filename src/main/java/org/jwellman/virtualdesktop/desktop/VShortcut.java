package org.jwellman.virtualdesktop.desktop;

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import org.apache.batik.transcoder.TranscoderException;

/**
 * A desktop shortcut
 *
 * @author Rick Wellman
 */
public class VShortcut extends JLabel {

    /** The last shortcut that was selected. */
    private static VShortcut lastItem;

    /** The shortcut where the mouse is being hovered over (i.e. can also be null) */
    private static VShortcut curItem;

    /** Logger for this class */
    private static final Logger log = Logger.getLogger(VShortcut.class.getName() );


    
    
    
    public VShortcut( String label, int xPos, int yPos ) {
        this(null, label, null, xPos, yPos);
    }

    public VShortcut( String label, String iconPath, int xPos, int yPos ) {
        this(null, label, iconPath, xPos, yPos);
    }

    /**
     * Creates a DeskItem that is on a JLayeredPane with the passed String identifier.
     * 
     * @param pane The JLayeredPane that the item will be placed on.
     * @param label The Label string to use.
     */
    public VShortcut( JLayeredPane pane, String label, String iconPath, int xPos, int yPos ) {
        super();
        
        this.setText(label);
        this.setHorizontalAlignment(JLabel.CENTER);
        
        this.setOpaque(false);
        this.setVerticalTextPosition(JLabel.BOTTOM);
        this.setHorizontalTextPosition(JLabel.CENTER);
        
        oldUI = getUI();
        if ( transparentBg ) { setUI( myui ); }

        this.setBorder();
        this.setLocation(xPos, yPos);
        
        this.pane = pane;
        
        this.setSize(this.getPreferredSize());
                        
        final Icon icon = this.genIcon(iconPath);
        this.setIcon(icon);

        final Dimension d = this.getSize();
        this.setSize(icon.getIconWidth() + 10, icon.getIconHeight() + d.height);

        this.setVisible(true);
        
        initMouseListeners();
    }

    private Icon genIcon(String iconPath) {
        Icon img = null;
        
        String path = (iconPath != null) ? iconPath : "org/jwellman/virtualdesktop/images/global_ui/add196";
        
        int genType = 2; // 1 = png, 2 = svg
        final String ext = (genType == 1) ? ".png" : ".svg";
        final URL url = this.getClass().getClassLoader().getResource(path + ext);                
        switch (genType) {
            case 1:
                img = new ImageIcon(url);

                break;
            case 2:
                try {
                    final Icon i = new VIcon(url.toString(), 32, 32);
                    img = i;
                } catch (TranscoderException ex) {
                    Logger.getLogger(VShortcut.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;                                            
        }
        
        return img;
    }

    /** Activates all the mouse listening activities */
    private void initMouseListeners() {
        addMouseListener( new MyEnterExitAdapter() );
        addMouseListener( new MyMouseAdapter() );
    }

    /**
     * Used to deselect the last selected DeskItem so that none are active.
     * This can be called when the background of the desk is clicked on,
     * or some other focus change event.
     */
    public static synchronized void clearSelection() {
        if ( lastItem != null ) { lastItem.exit(); }
        lastItem = null;
    }

    /**
     * This is called when the item is double clicked on.
     * A subclass can provide an action here.
     */
    public void invoke() {
        log.finer("Double-click desktop item");
    }

    /**
     * Called to report exception encountered during processing of events.
     * @param ex The Exception to report.
     */
    protected void reportException( Exception ex ) {
        log.log( Level.SEVERE, ex.toString(), ex );
    }

    /**
     * Make the selectable object bigger to surround the text with coloring.
     */
    protected void setBorder() {
        setBorder( BorderFactory.createEmptyBorder(5,5,5,5) );
    }

    /**
     * Called when the mouse enters this DeskItem.
     */
    protected void enter() {
        curItem = this;
        selected = true;
        if( transparentBg ) { setUI( selui ); }
        // setOpaque( true );
        repaint();
    }

    /**
     * Called when the mouse exits this DeskItem.
     */
    protected void exit() {
        curItem = null;
        selected = false;
        if( transparentBg ) { setUI( myui ); }
        // setOpaque( false );
        repaint();
    }

    /**
     * Called when the user performs a context menu click using the popup trigger button/operation
     * associated with the component.
     *

     * Subclasses can override this method to provide an operation.
     *
     * @param ev The associated mouse event to get the context from.
     */
    protected void popup( MouseEvent ev ) {
    }

    private class MyEnterExitAdapter extends MouseAdapter {

        @Override
        public void mouseEntered(MouseEvent ev) {
            if (transparentBg) {
                setUI(selected ? selactui : actui);
                // setOpaque(true);
            }
            repaint();
        }

        @Override
        public void mouseExited(MouseEvent ev) {
            if (transparentBg) {
                setUI(selected ? selui : myui);
                // setOpaque(selected);
            }
            repaint();
        }
    }

    /**
     * The MouseAdapter that handles click events to establish the MouseMotionListener.
     */
    private class MyMouseAdapter extends MouseAdapter {
        /**
         * The item this listener is associated with.
         */
        VShortcut item;

        /**
         * Creates an instance.
         */
        public MyMouseAdapter() {
            this.item = VShortcut.this;
        }

        /**
         * Called when the user clicks on the DeskItem.
         * @param ev The event associated with the click operation.
         */
        @Override
        public void mouseClicked( MouseEvent ev ) {
            if( ev.getClickCount() == 2 && ev.getButton() == 1 ) {
                log.log(Level.FINER, "Invoking: {0}", VShortcut.this);
                try {
                    // Invoke the item to process any action.
                    item.invoke();
                } catch( Exception ex ) {
                    reportException(ex);
                }
            }
        }

        /**
         * Called when the mouse is pressed down.
         * @param ev The associated event.
         */
        @Override
        public void mousePressed( MouseEvent ev ) {
            // Check if popup
            if( ev.isPopupTrigger() ) {
                // switch selected to this item
                if( lastItem != VShortcut.this ) {
                    lastItem.exit();
                }
                lastItem = VShortcut.this;
                lastItem.enter();

                // Show the menu if any
                popup(ev);
                return;
            }

            // Not popup, so clear last selected
            if( lastItem != null ) {
                lastItem.exit();
            }

            // If not button 1, just ignore
            if ( ev.getButton() != 1 ) return;

            // Activate motion listener and what for drag.
            createMotionListener(ev);

            // Add the mouse listener created, or already existing.
            VShortcut.this.addMouseMotionListener(mml);
        }

        /**
         * called to create the MouseMotionListener when the drag operation starts.
         * @param ev The associated mouse event.
         */
        private void createMotionListener(MouseEvent ev) {
            lastItem = VShortcut.this;
            lastItem.enter();
            Point p = lastItem.getLocation();
            final int offx = ev.getX();
            final int offy = ev.getY();

            // Create new listener as needed
            if( mml == null ) {
                mml = new MyMouseMotionListener();
            }

            // Set drag offsets into object
            mml.setOffsets( offx, offy );
        }

        /**
         * Called when the mouse button is released.
         * @param ev The associated event.
         */
        @Override
        public void mouseReleased( MouseEvent ev ) {

            if( ev.isPopupTrigger() ) {
                // Make sure the correct last item is identified.
                if( lastItem != null && lastItem != VShortcut.this ) {
                    lastItem.exit();
                }

                lastItem = VShortcut.this;
                lastItem.enter();
                popup(ev);

                // Stop listening to mouse motion events.
                if ( mml != null ) VShortcut.this.removeMouseMotionListener(mml);

                return;
            }

            // Not popup, remove motion listener
            VShortcut.this.removeMouseMotionListener(mml);

            // When dropped, move back to the default layer.
            if( pane != null ) {
                pane.setLayer(VShortcut.this,
                    JLayeredPane.DEFAULT_LAYER.intValue(), 0 );
            }
        }
    }

    /** The original UI that we revert to when not selected, hovered or otherwise active. */
    private ComponentUI oldUI;

    /** Is this DeskItem in a selected state? */
    protected boolean selected;

    /** The JLayeredPane we are on, if any. */
    private JLayeredPane pane;

    /** The MouseMotionListener created to manage our drag state. */
    private MyMouseMotionListener mml;

    /** The ComponentUI used for normal rendering. */
    protected static MyUI myui = new MyUI( new Color( 0, 0, 255, 0) );

    /** The ComponentUI used for active rendering (hover). */
    protected static MyUI actui = new MyUI( new Color( 0, 0, 255, 20) );

    /** The ComponentUI used for selected state. */
    protected static MyUI selui = new MyUI( new Color( 0, 0, 255, 55 ) );

    /** The ComponentUI used for selected and active rendering (hover). */
    protected static MyUI selactui = new MyUI( new Color( 0, 0, 255, 90 ) );

    /** Should gradient rendering be down at all? */
    protected static boolean transparentBg = true;

    /**
     * The MouseMotionAdapter used to track the drag operation.
     */
    private class MyMouseMotionListener extends MouseMotionAdapter {

        /** The x offset of the initial mouse click from the left edge of the DeskItem. */
        int offx;

        /** The y offset of the mouse from the top of the DeskItem */
        int offy;

        /**
         * updates the current offsets for each successive drag operation
         * to the click point that the mouse was out when the mouse was pressed.
         * @param x The X location of the initial mouse down event.
         * @param y The Y location of the initial mouse down event.
         */
        public void setOffsets(int x, int y ) {
            offx = x;
            offy = y;
        }

        /**
         * Called when the mouse is moved without a button down.
         * @param ev The associated event for this operation.
         */
        @Override
        public void mouseMoved( MouseEvent ev ) {
            mouseDragged(ev);
        }

        /**
         * Called when the mouse is moved with button one down.
         * @param ev The associated mouse event.
         */
        @Override
        public void mouseDragged( MouseEvent ev ) {
            Point pt = getLocation();
            Point p = new Point( ev.getX()+pt.x-offx, ev.getY()+pt.y-offy );

            // Positioning is every 5 pixels to make it easier to line things up.
            int xoff = p.x % 5;
            int yoff = p.y % 5;

            p = new Point( p.x-xoff+5, p.y-yoff+5 );

            // On a JDesktopPane, change the layer so that we pass over everything on the desktop
            if( pane != null ) {
                pane.setLayer(VShortcut.this, JLayeredPane.DRAG_LAYER.intValue() );
            }

            setLocation( p.x, p.y );
        }
    }

    /**
     * The UI used to control the drawing of the DeskItem
     * without having to conditionalize the paint operations.
     */
    private static class MyUI extends com.sun.java.swing.plaf.windows.WindowsLabelUI {

        /** The color of the background. */
        Color col;

        /**
         * Constructs an instance with the default coloring using the default background color
         * associated with the look and feel.
         * TODO:  this method of getting the background color is a hack
         */
        public MyUI() {
            col = new JLabel().getBackground();
        }

        /**
         * Constructs and instance using the passed color for the background.
         * @param c The color to use for the background painting operations.
         */
        public MyUI( Color c ) {
            col = c;
        }

        /**
         * Called to perform the paint operation on the passed component.
         * 
         * @param g The graphics context for the paint operation.
         * @param c The component to paint into the graphics environment.
         */
        @Override
        public void update(Graphics g, JComponent c) {
            if( !transparentBg ) {
                super.update( g, c );
            } else {
                final Graphics2D g2 = (Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                final Color original = g2.getColor();
                
                    g2.setColor(col);
                    // g2.fillRect(0, 0, c.getWidth(),c.getHeight());
                    g2.fillRoundRect(0, 0, c.getWidth(),c.getHeight(), 15, 15 );
                
                g2.setColor( original );

                super.update(g, c);                 
            }            
        }

    }

}