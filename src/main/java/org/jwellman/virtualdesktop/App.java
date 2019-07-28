package org.jwellman.virtualdesktop;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import org.jwellman.foundation.Foundation;
import org.jwellman.foundation.uContext;
import org.jwellman.foundation.extend.AbstractSimpleMain;
import org.jwellman.foundation.interfaces.uiThemeProvider;
import org.jwellman.foundation.swing.IWindow;
import org.jwellman.foundation.interfaces.uiDesktopProvider;
import org.jwellman.swing.plaf.metal.MetalThemeManager;
//import org.jwellman.vfsjfilechooser2.SpecVfsFileChooser2;
//import static org.jwellman.virtualdesktop.App.registeredApps;
import org.jwellman.virtualdesktop.desktop.VActionLNF;
import org.jwellman.virtualdesktop.desktop.VException;
import org.jwellman.virtualdesktop.desktop.VShortcut;
import org.jwellman.virtualdesktop.security.NoExitSecurityManager;
import org.jwellman.virtualdesktop.vapps.ActionFactory;
import org.jwellman.virtualdesktop.vapps.DesktopAction;
import org.jwellman.virtualdesktop.vapps.SpecBeanShell;
import org.jwellman.virtualdesktop.vapps.SpecHyperSQL;
import org.jwellman.virtualdesktop.vapps.SpecJCXConsole;
import org.jwellman.virtualdesktop.vapps.SpecJFreeChart;
import org.jwellman.virtualdesktop.vapps.SpecUberDragAndDrop;
import org.jwellman.virtualdesktop.vapps.SpecXChartDemo;
import org.jwellman.virtualdesktop.vapps.VirtualAppSpec;
import org.jwellman.virtualdesktop.vswing.VDesktopManager;
import org.jwellman.virtualdesktop.vswing.VDesktopPane;

import com.alee.laf.WebLookAndFeel;

/**
 * A Virtual Desktop.
 *
 * TODO Make this application "eat its own dog food"; i.e. use my Foundation app library
 *
 * @author Rick Wellman
 */
@SuppressWarnings("serial")
public class App extends AbstractSimpleMain 
implements 
	ActionListener,
	uiThemeProvider,
	uiDesktopProvider {

    /** The singleton */
    private static App app;

    /** the desktop */
    private JDesktopPane desktop;

    /** */
    private JMenu appMenu;

    /** a custom scrollpane for a scrollable desktop */
    private DesktopScrollPane dsp;

    /**
     * This is only nececessary for a temp dev menu item (File > New); can eventually be removed.
     * Even now, I could probably use ActionFactory registeredApps instead.
     */
    static Class[] registeredApps = {
         SpecBeanShell.class
        ,SpecJCXConsole.class
        ,SpecHyperSQL.class
        ,SpecJFreeChart.class
        ,SpecXChartDemo.class
        ,SpecUberDragAndDrop.class
    };

    static int count = -1; // just a simple development control variable

    /**
     * I am going to mark this as deprecated because jPAD should not call
     * this directly; it is here to allow beanshell scripts a way to access
     * the jPAD inner workings.  I'm not entirely sure if this will 
     * become non-deprecated or removed... time will tell.
     * 
     * @return
     * 
     * @deprecated see the javadoc for this method
     */
    static public App getVSystem() {
        return app;
    }

    private App() {
    }
    
    /**
     * This was the constructor when App was a JFrame;
     * can be deleted once the Foundation refactor is complete.
     */
    private void oldAppConstructor() {
        // super("Java Virtual Desktop");
        App.app = this;

        //Make the big window be indented 50 pixels from each edge of the screen.
        int inset = 50;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        // setBounds(inset, inset, screenSize.width  - inset*2, screenSize.height - inset*2);

        // setJMenuBar(createMenuBar());

        //Set up the GUI.
        JPanel p = new JPanel(new BorderLayout());
        JTextArea species = new JTextArea("Species");
        JTextArea locations = new JTextArea("Locations");
        JTextArea travelPaths = new JTextArea("TravelPaths");

        JPanel controls = null;

        JFrame dummy = new JFrame(); // not part of the original; just here to make this compile
        // to make it work like the original replace with: dummy = this;
        
        desktop = new VDesktopPane(); // new JDesktopPane(); //a specialized layered pane
        desktop.setDesktopManager(new VDesktopManager());
        int version = 4;
        switch (version) {
            case 1:
                dsp = new DesktopScrollPane(desktop);
                dummy.setContentPane(dsp); //(desktop);
                break;
            case 2:
                controls = new JPanel(new GridLayout(3, 0));
                controls.add(new JScrollPane(species));
                controls.add(new JScrollPane(locations));
                controls.add(new JScrollPane(travelPaths));

                p.add(controls, BorderLayout.WEST);
                p.add(desktop);
                dummy.setContentPane(p);
                break;
            case 3:
                controls = new JPanel(new GridLayout(3, 0));
                controls.add(new JScrollPane(species));
                controls.add(new JScrollPane(locations));
                controls.add(new JScrollPane(travelPaths));

                dsp = new DesktopScrollPane(desktop);
                p.add(controls, BorderLayout.WEST);
                p.add(dsp);
                dummy.setContentPane(p);
                break;
            case 4:
                controls = new JPanel(new GridLayout(3, 0));
                controls.add(new JScrollPane(species));
                controls.add(new JScrollPane(locations));
                controls.add(new JScrollPane(travelPaths));

                dsp = new DesktopScrollPane(desktop);

                //Create a split pane with the two scroll panes in it.
                JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controls, dsp);
                splitPane.setOneTouchExpandable(true);
                splitPane.setDividerLocation(150);
                p.add(splitPane);
                dummy.setContentPane(p);
                break;

        }

        // createVApp(new SpecBeanShell()); //(new SpecEmpty()); //create first "window"

        //Make dragging a little faster but perhaps uglier.
        desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);

        DesktopAction.setDesktop(this);
        ActionFactory.initDesktop();

        // These are for desktop layout... this is VERY inelegant...
        // this needs to be from persistence mechanism ...
        // and overall, there needs to be a "layout manager" for the desktop
        int x = 10; int y = -70;

        JMenuItem m = null;
        for (DesktopAction a : ActionFactory.getListOfActions()) {
            /* TODO Currently this is coded so that it is either
               on the desktop or in the menu.  I do not have a current
               working example, but there can probably be both so
               this implementation will have to change.
             */
            if (a.isDesktopOnly()) {
                final Icon icon = (Icon) a.getValue(Action.LARGE_ICON_KEY);
                final String label = (String) a.getValue(Action.NAME);
                final VShortcut vs = new VShortcut(a, label, icon, x, y+=80);
                desktop.add(vs);
            } else {
                //m = new JMenuItem(a);
                appMenu.add(a);
            }
        }

    }

    protected JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        //Set up the lone menu.
        JMenu menu = new JMenu("System");
        menu.setMnemonic(KeyEvent.VK_S);
        menuBar.add(menu);

        //Set up the first menu item.
        JMenuItem menuItem = new JMenuItem("New");
        menuItem.setMnemonic(KeyEvent.VK_N);
        menuItem.setAccelerator(KeyStroke.getKeyStroke( KeyEvent.VK_N, ActionEvent.ALT_MASK));
        menuItem.setActionCommand("new");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        //Set up the second menu item.
        menuItem = new JMenuItem("Exit");
        menuItem.setMnemonic(KeyEvent.VK_X);
        menuItem.setAccelerator(KeyStroke.getKeyStroke( KeyEvent.VK_X, ActionEvent.ALT_MASK));
        menuItem.setActionCommand("exit");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        appMenu = new JMenu("VApps");
        appMenu.setMnemonic(KeyEvent.VK_V);
        menuBar.add(appMenu);

        menu = new JMenu("Skin");
        menu.setMnemonic(KeyEvent.VK_K);
        menuBar.add(menu);

        // TODO for sake of precision, it should probably be determined that each
        // lookandfeel is available before adding it to the menu.

        // I have decided that weblaf will be the default and baseline look and feel
        menuItem = new JMenuItem(new VActionLNF("Web",null,"com.alee.laf.WebLookAndFeel", (JFrame)this.window));
        menu.add(menuItem);

        menuItem = new JMenuItem(new VActionLNF("Nimbus",null,"javax.swing.plaf.nimbus.NimbusLookAndFeel", (JFrame)this.window));
        menu.add(menuItem);

        // TODO add Metal

        menuItem = new JMenuItem(new VActionLNF("System",null,UIManager.getSystemLookAndFeelClassName(), (JFrame)this.window));
        menu.add(menuItem);

        return menuBar;
    }

    // React to menu selections.
    @Override public void actionPerformed(ActionEvent e) {
        if ("new".equals(e.getActionCommand())) {
            try {
                // TODO This is just temporary; need to implement a real feature
                createVApp( registeredApps[(++count % registeredApps.length)].newInstance() );
            } catch (InstantiationException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if ("exit".equals(e.getActionCommand())) {
            quit();
        } else {
            throw new VException("Unknown Action", null);
        }
    }

    /**
     * I think this is the least desired use case...
     * do a search for usage and then document.
     * 
     * @param newInstance
     */
    public void createVApp(Object newInstance) {
        this.createVApp((VirtualAppSpec)newInstance);
    }

    /**
     * Create a new application.
     * This is the most common use case for jPAD.
     *
     * @param spec
     */
    public void createVApp(final VirtualAppSpec spec) {

        if (spec.isInternalFrameProvider()) {
            final VirtualAppFrame frame = new VirtualAppFrame(spec.getTitle());
            spec.populateInternalFrame(frame);
        } else {
            this.createVApp(spec.getContent(), spec.getTitle(), null);
        }

    }

    public VirtualAppFrame createVApp(final Container c, final String title) {
        return this.createVApp(c, title, null);
    }

    /**
     * This public method allows internal apps to create internal apps/windows.
     * i.e. via beanshell or others
     *
     * [This is the definitive method of the overloaded versions; i.e. all roads lead here]
     * 
     * @param c
     * @param title
     * @param icon
     * @return
     */
    public VirtualAppFrame createVApp(final Container c, final String title, final Icon icon) {

        final VirtualAppFrame frame = new VirtualAppFrame(title);

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                try {
                    // final VirtualAppFrame frame = new VirtualAppFrame(title);
                    frame.setContentPane(c);

                    if (icon != null) frame.setFrameIcon(icon);

                    frame.pack(); // see Note [1] below
                    if ((c.getWidth() * c.getHeight()) != 0) {
                        frame.setSize(c.getWidth(), c.getHeight());
                        System.out.println("JIF setSize()");
                    } else {
//                        frame.pack(); // see Note [1] below
                    }

//                    frame.setVisible(true); //necessary as of 1.3
//                    frame.setSelected(true);
                    desktop.add(frame);
                    frame.setVisible(true); //necessary as of 1.3
                    frame.setSelected(true);
                } catch (java.beans.PropertyVetoException e) {
                    e.printStackTrace(); // for now, simply swallow the exception
                } catch (Exception e) {
                    e.printStackTrace(); // for now, simply swallow the exception
                }
            }
        });

        return frame;

        // Note [1]: For now I am removing this via comment as it has undesired
        // side effects.  However, I have a feeling that I should be using
        // pack() and the side effects are due to a design error elsewhere.
        // 9/2/2018:  Trying to always call pack() first, then setsize().
    }

    /**
     * This currently exists ONLY to support the prototyping of integrating
     * the Groovy Console via the jvdConsole.groovy script.
     *
     * It is currently not envisioned that apps can access the 'desktop'
     * directly.  Rather, it is preferred that we implement an API/methods
     * to allow JInternalFrames to be added to the desktop.
     *
     * @return the JDesktopPane
     * 
     * @deprecated see the javadoc why this will go away eventually
     */
    public JDesktopPane getDesktop() {
        return this.desktop;
    }

    /**
     * Quit the application (not sure if there is "cleanup" to do)
     * TODO implement some sort of app notification and/or veto feature
     */
    protected void quit() {
        System.exit(0);
    }


    /**
     * 
     * @param args
     */
    public static void main(String[] args) {

        // Install a custom security manager to prevent guests from shutting down the desktop.
        System.setSecurityManager(new NoExitSecurityManager());
        
        App.app = new App().startup(true, args);
        
    }
    
    /**
     * 
     * @param asMainFrame
     * @param args
     * @return
     */
    private App startup(boolean asMainFrame, String[] args) {
    	
        // Prepare - User Interface Context
        final uContext context = uContext.createContext();
        context.setDimension(85);
        context.setDesktopTitle("jPAD - Java Powered Alternative Desktop - powered by the Foundation API");
        context.setThemeProvider(this);
        context.setDesktopProvider(this);
        context.setDesktopMode(true);

        // Step 1 - Initialize Swing
        final Foundation f = Foundation.init(context); // context

        // Step 2 - Create your UIs in JPanel(s)
        mainui = f.registerUI("desktop.control", this.createDesktop()); // new DataBrowser());

        /* Design Note, 7/26/2019 @ 11:28pm
         * The issue in a desktop setup is how to differentiate/identify the JPanel
         * that serves as the desktop container in the main JFrame.  i.e. the insertion
         * of a main JPanel (that contains a desktop) is straight forward given the
         * current design (i.e. use the useDesktop() method) but... what is not so 
         * straight forward (because I am in the middle of designing it) is how to
         * know that future IWindow objects must be added to the desktop *within*
         * the JFrame/JPanel/JDesktoppane.  
         * 
         * Since desktop layouts are rare "in the wild" and usually require
         * knowledge of Swing beyond the novice, I think it is fair to assume
         * that I can require someone to register a desktop pane early in
         * the foundation bootstrapping sequence if that is what they are wanting.
         * (which jPAD would do).  That way, the registering of a desktop
         * would explicitly indicate a desktop mode/use-case and future
         * invocations of useDesktop()/useWindow() can respond accordingly.
         * 
         * What is confusing me (probably because I never did think it completely through)
         * is what I intend to accomplish with registerUI() vs. useDesktop()/useWindow().
         * They return different object types but there still seems to be some
         * duplication of intent?  Well... the next step (described next) will
         * probably prove this out.
         * 
         * Next Step(s):
         * -- mainui needs to be the JPanel with the desktop pane
         *    instead of the empty JPanel I am currently using as a placeholder
         * -- this also means removing the default creation of a desktop pane
         *    within the showGUI() method in Stone.java
         * -- by having a desktop pane registered, the next implementation
         *    of the showGUI() method can know whether an IWindow represents
         *    a JFrame or a JInternalFrame; obviously, JInternalFrame(s)
         *    would be placed on (i.e. added to) the desktop pane.
         * 
         */
        
        // Step 3 - Use Foundation to create your "window"; give it your UI.
        window = f.useWindow(mainui); // f.useWindow(mainui);
        // Step 3a (optional) - Customize your window
        window.setTitle("Control"); 
        window.setResizable(true);
        window.setMaximizable(true);

//		final ComponentGlassPane gp = new ComponentGlassPane((JFrame)this.window);		
//		final DataBrowser b = (DataBrowser)this.mainui.getChild();
//		b.getGlassPaneButton().addActionListener(gp);

        // Step 4a - Create data models, controllers, and other non-UI objects
        // n/a
        
        // Step 4b (optional)- Associate models with views
        // n/a

        // Step 5 - Display your User Interface
        f.showGUI(window);

    	return this;
    }

    private JPanel createDesktop() {
		
    	// The JPanel to return
        JPanel p = new JPanel(new BorderLayout());
        
        JTextArea species = new JTextArea("Species");
        JTextArea locations = new JTextArea("Locations");
        JTextArea travelPaths = new JTextArea("TravelPaths");

        final JPanel controls = new JPanel(new GridLayout(3, 0));
        controls.add(new JScrollPane(species));
        controls.add(new JScrollPane(locations));
        controls.add(new JScrollPane(travelPaths));

        this.desktop = new VDesktopPane(); // new JDesktopPane(); //a specialized layered pane
        this.desktop.setDesktopManager(new VDesktopManager());

        this.dsp = new DesktopScrollPane(desktop);

        //Create a split pane with the two scroll panes in it.
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controls, dsp);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(150);
        p.add(splitPane);
        // window.setContentPane(p);
		
		return p;
	}

	/**
     * This is the copy of main before refactoring for Foundation API;
     * it is here for reference during the refactoring but can be deleted
     * once the refactoring is complete.
     * 
     * The main thing to determine this can be removed is if the
     * look and feel code has been incorporated into Foundation API.
     * 
     * @param args
     */
    public static void oldmain(String[] args) {

        // Install a custom security manager to prevent guests from shutting down the desktop.
        System.setSecurityManager(new NoExitSecurityManager());

        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(
            new Runnable() { @Override public void run() {
                try {

                    //Make sure we have nice window decorations.
                    JFrame.setDefaultLookAndFeelDecorated(true);

                    // This does not appear to be working as expected?
                    // System.setProperty(WebLookAndFeel.PROPERTY_HONOR_USER_BORDERS, "true");

                    int choice = 2;
                    switch (choice) {
                        case 1:
                            final String sys = UIManager.getSystemLookAndFeelClassName();
                            UIManager.setLookAndFeel(sys);
                            break;
                        case 2:
                            //UIManager.installLookAndFeel("Web", "com.alee.laf.WebLookAndFeel");
                            WebLookAndFeel.install();
                            break;
                        case 3:
                            UIManager.setLookAndFeel ( NimbusLookAndFeel.class.getCanonicalName () );
                            WebLookAndFeel.initializeManagers ();
                            break;
                        case 4:
                            // Sep. 2018:  Sea Glass seems to now work with Java8; I like certain things
                            // but it now feels "dated" as it seems to look like a Windows version
                            // of the mac aqua look and feel.  i.e. see the JTabbedPane implementation
                            // Also, I think it is still pretty incomplete with intermittent buggy behavior :(
                            UIManager.setLookAndFeel("com.seaglasslookandfeel.SeaGlassLookAndFeel");
                            break;
                        case 5:
                            MetalLookAndFeel
                                    .setCurrentTheme(MetalThemeManager.MODERN);
                                    //.setCurrentTheme(MetalThemeManager.LARGE_FONT);
                                    //.setCurrentTheme(new DefaultMetalTheme());
                                    //.setCurrentTheme(new OceanTheme());
                            UIManager.setLookAndFeel(new MetalLookAndFeel());
                            break;
                        case 6:
                            // Sep. 2018:  evaluating pgs
                            UIManager.setLookAndFeel("com.pagosoft.plaf.PgsLookAndFeel");
                            break;
                        case 99:
                            // This is my original code; don't use it.
                            UIManager.installLookAndFeel("Web", "com.alee.laf.WebLookAndFeel");
                            WebLookAndFeel.install();
                                for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                                    System.out.println(info.getName() + " : " + info.getClassName());
                                    if ("Nimbus".equals(info.getName())) {
                                        UIManager.setLookAndFeel(info.getClassName()); break;
                                    }
                                    // if ("Web".equals(info.getName())) {
                                    //     UIManager.setLookAndFeel(info.getClassName()); break;
                                    // }
                                }
                            break;
                    }
                } catch (Exception e) {
                    // If Nimbus is not available, you can set the GUI to another look and feel.
                    final String sys = UIManager.getSystemLookAndFeelClassName();
                    try {
                        UIManager.setLookAndFeel(sys);
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InstantiationException ex) {
                        Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalAccessException ex) {
                        Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (UnsupportedLookAndFeelException ex) {
                        Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                // createAndShowGUI();
            } }
        );
    }

	@Override
	public void doTheme() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doCustomDesktop(IWindow w) {

		//Set up the GUI.
        JPanel p = new JPanel(new BorderLayout());
        JTextArea species = new JTextArea("Species");
        JTextArea locations = new JTextArea("Locations");
        JTextArea travelPaths = new JTextArea("TravelPaths");

        JPanel controls = null;

        // comment out the next line... use the incoming 'window' parameter 
        // JFrame window = new JFrame(); // not part of the original; just here to make this compile
        // to make it work like the original replace with: dummy = this;
        
        desktop = new VDesktopPane(); // new JDesktopPane(); //a specialized layered pane
        desktop.setDesktopManager(new VDesktopManager());
        int version = 4;
        switch (version) {
            case 1:
                dsp = new DesktopScrollPane(desktop);
                window.setContentPane(dsp); //(desktop);
                break;
            case 2:
                controls = new JPanel(new GridLayout(3, 0));
                controls.add(new JScrollPane(species));
                controls.add(new JScrollPane(locations));
                controls.add(new JScrollPane(travelPaths));

                p.add(controls, BorderLayout.WEST);
                p.add(desktop);
                window.setContentPane(p);
                break;
            case 3:
                controls = new JPanel(new GridLayout(3, 0));
                controls.add(new JScrollPane(species));
                controls.add(new JScrollPane(locations));
                controls.add(new JScrollPane(travelPaths));

                dsp = new DesktopScrollPane(desktop);
                p.add(controls, BorderLayout.WEST);
                p.add(dsp);
                window.setContentPane(p);
                break;
            case 4:
                controls = new JPanel(new GridLayout(3, 0));
                controls.add(new JScrollPane(species));
                controls.add(new JScrollPane(locations));
                controls.add(new JScrollPane(travelPaths));

                dsp = new DesktopScrollPane(desktop);

                //Create a split pane with the two scroll panes in it.
                JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controls, dsp);
                splitPane.setOneTouchExpandable(true);
                splitPane.setDividerLocation(150);
                p.add(splitPane);
                window.setContentPane(p);
                break;

        }
		
	}

}
