package org.jwellman.virtualdesktop.vapps;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import bibliothek.gui.DockController;
import bibliothek.gui.DockFrontend;
import bibliothek.gui.Dockable;
import bibliothek.gui.dock.DefaultDockable;
import bibliothek.gui.dock.ScreenDockStation;
import bibliothek.gui.dock.SplitDockStation;
import bibliothek.gui.dock.common.CContentArea;
import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.CLocation;
import bibliothek.gui.dock.common.DefaultSingleCDockable;
import bibliothek.gui.dock.common.SingleCDockable;
import bibliothek.gui.dock.common.theme.ThemeMap;
import bibliothek.gui.dock.station.screen.InternalBoundaryRestriction;
import bibliothek.gui.dock.station.screen.InternalFullscreenStrategy;
import bibliothek.gui.dock.station.screen.window.InternalScreenDockWindowFactory;
import bibliothek.gui.dock.station.split.SplitDockGrid;

/**
 * Demonstrate the Docking Framework API
 * <p>See JDesktopPaneExample in the Docking Framework project
 * for references as to how/why things are done the way they're done.
 *
 * @author Rick Wellman
 */
public class SpecDocking extends VirtualAppSpec {
	
    private static int counter = 0;
    
    private static final JFrame externalFrame = new JFrame("Docking Frame");
    
    // In order to drag/drop between internal frames,
    // they must all share a controller (therefore it is static)
	private static CControl control;
	
	// Each instance gets its own content area (by API design)
	private CContentArea content = null;
	
	private static final String DEFAULT_TITLE = "Docking Framework";

	public SpecDocking() {
        this(DEFAULT_TITLE);
    }

    public SpecDocking(String title) {
        super();

        this.setTitle(title);
        this.internalFrameProvider = true;

        // Update the counter so that content area names are unique
        counter++;

    }

    @Override
    public void populateInternalFrame(JInternalFrame frame, JDesktopPane desktop) {
        int version = 4;
        switch (version) {
        case 1: this.version01(frame, desktop); break;
        case 2: this.version02(frame, desktop); break;
        case 3: this.version03(frame, desktop); break;
        case 4: this.version04(frame, desktop); break;
        }

    }

    public void addDockable(String title, JComponent c) {
        SingleCDockable dockable = new DefaultSingleCDockable(title, title, c);        
        control.addDockable( dockable );
        
        dockable.setLocation( CLocation.base(content).normal() );
        dockable.setVisible( true );

    }
    
    /**
     * This is version 4 (obviously)...
     * I finally found out how to drag/drop between internal frames;  
     * Each must get its own "content area" which is created by 
     * a common controller object.
     *   
     * The current implementation is VERY similar to version 2.
     * 
     * @param frame
     * @param desktop
     */
    private void version04(JInternalFrame frame, JDesktopPane desktop) {

        // Setup Docking Controller...
        if (control == null) {
            control = new CControl( externalFrame ); // [1]
            // control = new CControl(); // [1]
            // control.putProperty( DockTheme.DOCKABLE_MOVING_IMAGE_FACTORY, ScreencaptureMovingImageFactory );
            
            this.content = control.getContentArea();
            
            // Besides my visual preference for the flat theme,
            // it also does not use animations (which I also prefer).
            final ThemeMap themes = control.getThemes();
            themes.select(ThemeMap.KEY_FLAT_THEME);
 
            externalFrame.setSize(300, 500);
            externalFrame.setLocationRelativeTo(null);
            externalFrame.setVisible(true);
        } else {
            this.content = control.createContentArea("ContentArea_" + counter);
        }
        
        // Add the Controller content area to the internal frame
        frame.setLayout(new BorderLayout());
        frame.add( content, BorderLayout.CENTER );

        // This populates the demo app spec.  Others will be "blank"
        if (this.getTitle().equals(DEFAULT_TITLE)) {
            // Create some Dockables...
            SingleCDockable red = demoDockable("Red_" + counter, Color.red);
            SingleCDockable blue = demoDockable("Blue_" + counter, Color.blue);
            SingleCDockable green = demoDockable("Green_" + counter, Color.green);
                    
            // Add Dockables to the Controller:
            control.addDockable( green );
            control.addDockable( red );
            control.addDockable( blue );

            // Locations cannot be set until:
            // 1) the Controller content area is added to a component
            // 2) the Dockable has been added to the Controller
            green.setLocation( CLocation.base(content).normal() );
            green.setVisible( true );
            
            red.setLocation( CLocation.base(content).minimalNorth(0) );
            red.setVisible( true );

  //          blue.setLocation( CLocation.base(content).minimalNorth(1) );
            blue.setLocation( CLocation.external( 300, 200, 200, 100 ) ); // [2]
            blue.setVisible( true );
            // [2] Note that external will only work if there is a visible JFrame
            //     attached.  Since the goal of this framework is to *not* proliferate
            //     JFrames, then this demo will not do so (but, again, it's possible).            
        }
        
        SwingUtilities.invokeLater(() -> {
            frame.pack();
            frame.setSize(new Dimension(200, 300));
            frame.setVisible(true);
        } );

    }

    /**
     * This version also works but (again)...
     * 1) suffers from the same inability to drag/drop between internal frames
     * 2) is based on just the Core API which further restricts its features
     * 
     * This version is based on http://www.docking-frames.org/help.html
     * 
     * @param frame
     * @param desktop
     */
    @SuppressWarnings("deprecation")
    private void version03(JInternalFrame frame, JDesktopPane desktop) {
        
        //JFrame frame = new JFrame( "Demo" );
        DockFrontend frontend = new DockFrontend(); // frame );
        DockController.disableCoreWarning();
        
        SplitDockStation station = new SplitDockStation();        
        frame.add( station.getComponent() );
        frontend.addRoot( station, "station" );
        
        SplitDockGrid grid = new SplitDockGrid();
        grid.addDockable( 0, 0, 1, 1, createDock( "Red", Color.RED ) );
        grid.addDockable( 0, 1, 1, 1, createDock( "Green", Color.GREEN ) );
        grid.addDockable( 1, 0, 1, 1, createDock( "Blue", Color.BLUE ) );
        grid.addDockable( 1, 1, 1, 1, createDock( "Yellow", Color.YELLOW ) );
        station.dropTree( grid.toTree() );
        
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.setBounds( 20, 20, 400, 400 );
        frame.setVisible( true );
    }

    public static Dockable createDock(String title, Color color) {
        JPanel panel = new JPanel();
        panel.setOpaque(true);
        panel.setBackground(color);
        return new DefaultDockable(panel, title);
    }
 
    /**
     * This version also works, but... it was an attempt to get dockables
     * to transfer from one internal frame to another.  However, it just
     * ended up combining them all into one internal frame. 
     * 
     * @param frame
     * @param desktop
     */
    private void version02(JInternalFrame frame, JDesktopPane desktop) {
        // Setup Docking Controller...
        if (control == null) {
            control = new CControl( new JFrame() ); // [1]
            System.out.println("[A]");
            // [1] An "anonymous" JFrame does work (and never has to be visible);
            //     however, it prevents "externalized" dockables.  See note [2] below.
    
            /* The ScreenDockStation needs some special factories and 
             * strategies to handle the JDesktopPane.
             *  - The boundary restriction ensures that a window cannot be moved out of the desktop
             *  - The fullscreen strategy tells when a window is in fullscreen mode and when not
             *  - The window factory creates the windows on which Dockables are shown */
            control.putProperty( ScreenDockStation.BOUNDARY_RESTRICTION, new InternalBoundaryRestriction( desktop ) );
            control.putProperty( ScreenDockStation.FULL_SCREEN_STRATEGY, new InternalFullscreenStrategy( desktop ) );
            control.putProperty( ScreenDockStation.WINDOW_FACTORY, new InternalScreenDockWindowFactory( desktop ) );
        }
        
        // Add the Controller content area to the internal frame
        frame.setLayout(new BorderLayout());
        frame.add( control.getContentArea(), BorderLayout.CENTER );

        // Create some Dockables...
        SingleCDockable red = demoDockable("Red_" + counter, Color.red);
        SingleCDockable blue = demoDockable("Blue_" + counter, Color.blue);
        SingleCDockable green = demoDockable("Green_" + counter++, Color.green);
                
        // Add Dockables to the Controller:
        control.addDockable( green );
        control.addDockable( red );
        control.addDockable( blue );

        // Locations cannot be set until:
        // 1) the Controller content area is added to a component
        // 2) the Dockable has been added to the Controller
        green.setLocation( CLocation.base().normal() );
        green.setVisible( true );
        
        red.setLocation( CLocation.base().minimalNorth(0) );
        red.setVisible( true );

        blue.setLocation( CLocation.base().minimalNorth(1) );
        // blue.setLocation( CLocation.external( 300, 200, 200, 100 ) ); // [2]
        blue.setVisible( true );
        // [2] Note that external will only work if there is a visible JFrame
        //     attached.  Since the goal of this framework is to *not* proliferate
        //     JFrames, then this demo will not do so (but, again, it's possible).
        
        SwingUtilities.invokeLater(() -> {
            frame.pack();
            frame.setSize(new Dimension(200, 300));
            frame.setVisible(true);
        } );

    }

    /**
     * This was my first version which is almost identical to the JDesktopPaneExample
     * in the docking frames demo-tutorial project.  This does work in general but
     * does not allow dragging/dropping between internal frames which I desire
     * quite a bit.  Also, see the internal notes for important information
     * within this use-case.
     * 
     * @param frame
     * @param desktop
     */
    public void version01(JInternalFrame frame, JDesktopPane desktop) {

        // Setup Docking Framework
        CControl control = new CControl(); // [1a] // ( new JFrame() ); // [1]
		// [1] An "anonymous" JFrame does work (and never has to be visible);
		//     however, it prevents "externalized" dockables.  See note [2] below.
        // [1a] Similarly, the API supports no JFrame in the constructor at all
        //      however, it also prevents "externalized" dockables.

        /* The ScreenDockStation needs some special factories and 
         * strategies to handle the JDesktopPane. [3]
		 *  - The boundary restriction ensures that a window cannot be moved out of the desktop
		 *  - The fullscreen strategy tells when a window is in fullscreen mode and when not
		 *  - The window factory creates the windows on which Dockables are shown */
//		control.putProperty( ScreenDockStation.BOUNDARY_RESTRICTION, new InternalBoundaryRestriction( desktop ) );
//		control.putProperty( ScreenDockStation.FULL_SCREEN_STRATEGY, new InternalFullscreenStrategy( desktop ) );
//		control.putProperty( ScreenDockStation.WINDOW_FACTORY, new InternalScreenDockWindowFactory( desktop ) );
        // [3] 1/1/2021: At first glance, I think these properties are only applicable
        //     when used in a "typical" internal frame application.  Particularly, when
        //     the JFrame passed to the controller is the same as the one that contains
        //     the JDesktopPane.  However, this application does not follow that paradigm.
        //     p.s. I have verified that these properties are not necessary for basic usage
        //     within this demo so they can be enabled or commented out at your discretion.

		// Add the Controller content area to the internal frame
    	frame.setLayout(new BorderLayout());
        frame.add( control.getContentArea(), BorderLayout.CENTER );

    	// Create some Dockables...
        SingleCDockable red = demoDockable("Red", Color.red);
        SingleCDockable blue = demoDockable("Blue", Color.blue);
        SingleCDockable green = demoDockable("Green", Color.green);
		        
		// Add Dockables to the Controller:
		control.addDockable( green );
		control.addDockable( red );
		control.addDockable( blue );

		// Locations cannot be set until:
		// 1) the Controller content area is added to a component
		// 2) the Dockable has been added to the Controller
        green.setLocation( CLocation.base().normal() );
		green.setVisible( true );
		
		red.setLocation( CLocation.base().minimalNorth(0) );
		red.setVisible( true );

		blue.setLocation( CLocation.base().minimalNorth(1) );
		// blue.setLocation( CLocation.external( 300, 200, 200, 100 ) ); // [2]
		blue.setVisible( true );
		// [2] Note that external will only work if there is a visible JFrame
		//     attached.  Since the goal of this framework is to *not* proliferate
		//     JFrames, then this demo will not do so (but, again, it's possible).
		
        SwingUtilities.invokeLater(() -> {
            frame.pack();
            frame.setSize(new Dimension(200, 300));
            frame.setVisible(true);
        } );

    }

	private SingleCDockable demoDockable(String title, Color color) {

		JPanel p = new JPanel();
		p.setOpaque(true);
		p.setBackground(color);
		
		return new DefaultSingleCDockable(title, title, p);
	}

}
