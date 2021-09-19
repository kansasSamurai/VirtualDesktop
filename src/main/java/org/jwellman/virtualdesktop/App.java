package org.jwellman.virtualdesktop;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel ;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import org.jwellman.swing.plaf.metal.MetalThemeManager ;
//import org.jwellman.vfsjfilechooser2.SpecVfsFileChooser2;
//import static org.jwellman.virtualdesktop.App.registeredApps;
import org.jwellman.virtualdesktop.desktop.VActionLNF;
import org.jwellman.virtualdesktop.desktop.VException;
import org.jwellman.virtualdesktop.desktop.VShortcut;
import org.jwellman.virtualdesktop.desktopmgr.VAppListCellRenderer;
import org.jwellman.virtualdesktop.security.NoExitSecurityManager;
import org.jwellman.virtualdesktop.vapps.ActionFactory;
import org.jwellman.virtualdesktop.vapps.DesktopAction;
import org.jwellman.virtualdesktop.vapps.SpecBeanShell;
import org.jwellman.virtualdesktop.vapps.SpecHyperSQL;
import org.jwellman.virtualdesktop.vapps.SpecJCXConsole;
import org.jwellman.virtualdesktop.vapps.SpecJFreeChart;
import org.jwellman.virtualdesktop.vapps.SpecUberDragAndDrop;
import org.jwellman.virtualdesktop.vapps.SpecXChartDemo;
import org.jwellman.virtualdesktop.vapps.SpecXionFM;
import org.jwellman.virtualdesktop.vswing.VDesktopPane;

import com.alee.laf.WebLookAndFeel;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.jtattoo.plaf.aluminium.AluminiumLookAndFeel;

import bibliothek.gui.dock.ScreenDockStation;
import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.station.screen.InternalBoundaryRestriction;
import bibliothek.gui.dock.station.screen.InternalFullscreenStrategy;
import bibliothek.gui.dock.station.screen.window.InternalScreenDockWindowFactory;
import ca.odell.glazedlists.swing.DefaultEventListModel;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import jiconfont.icons.FontAwesome;
import jiconfont.icons.GoogleMaterialDesignIcons;
import jiconfont.swing.IconFontSwing;
// import org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel;
import net.sourceforge.napkinlaf.NapkinLookAndFeel ;
import net.sourceforge.napkinlaf.NapkinTheme ;

/**
 * A Virtual Desktop.
 *
 * TODO Make this application "eat its own dog food"; i.e. use my Foundation app library
 *
 * @author Rick Wellman
 */
@SuppressWarnings("serial")
public class App extends JFrame implements ActionListener {

    /** The singleton */
    private static App app;

    /** the desktop */
    private JDesktopPane desktop;

    private JMenu appMenu;

    /** a custom scrollpane for a scrollable desktop */
    private DesktopScrollPane dsp;

    // These are a workaround because VirtualAppFrame needs to know the LAF
    // for a workaround that it employs for WEBLAF only
    public static final int LAF_SYSTEM = 1;
    public static final int LAF_WEBLAF = 2;
    public static final int LAF_NIMBUS = 3;
    public static final int LAF_METAL = 5;
    public static final int LAF_PGS = 6;
    public static final int LAF_JTATTOO = 7;
    public static final int LAF_FLATLAF = 8;
    public static final int LAF_SUBSTANCE = 9; // deprecated / not currently used
    public static final int LAF_NAPKIN = 10;
    public static final int CHOSEN_LAF = LAF_JTATTOO; //LAF_JTATTOO;

    /**
     * This is only nececessary for a temp dev menu item; can eventually be removed
     * Even now, I could probably use ActionFactory registeredApps instead.
     */
    static Class[] registeredApps = {
        SpecBeanShell.class
        ,SpecJCXConsole.class
        ,SpecHyperSQL.class
        ,SpecJFreeChart.class
        ,SpecXChartDemo.class
        ,SpecXionFM.class
        ,SpecUberDragAndDrop.class
//        ,SpecXionFM.class // this app is targeted for Linux
//        ,SpecJzy3D.class // this app sucks
    };

    static int count = -1; // just a simple development control variable

    static public App getVSystem() {
        return app;
    }

    private App() {
        super("Java Virtual Desktop");
        App.app = this;

        //Make the big window be indented 50 pixels from each edge of the screen.
        int inset = 50;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(inset, inset, screenSize.width  - inset*2, screenSize.height - inset*2);

        setJMenuBar(createMenuBar());

        //Set up the GUI.
        JPanel p = new JPanel(new BorderLayout());
        JTextArea species = new JTextArea("Species");
        JTextArea locations = new JTextArea("Locations");
        JTextArea travelPaths = new JTextArea("TravelPaths");

        JPanel controls = null;

        desktop = new VDesktopPane(); // new JDesktopPane(); //a specialized layered pane
        DesktopManager.get().setDesktop(desktop);

        int version = 5;
        switch (version) {
            case 1:
                dsp = new DesktopScrollPane(desktop);
                setContentPane(dsp); //(desktop);
                break;
            case 2:
                controls = new JPanel(new GridLayout(3, 0));
                controls.add(new JScrollPane(species));
                controls.add(new JScrollPane(locations));
                controls.add(new JScrollPane(travelPaths));

                p.add(controls, BorderLayout.WEST);
                p.add(desktop);
                this.setContentPane(p);
                break;
            case 3:
                controls = new JPanel(new GridLayout(3, 0));
                controls.add(new JScrollPane(species));
                controls.add(new JScrollPane(locations));
                controls.add(new JScrollPane(travelPaths));

                dsp = new DesktopScrollPane(desktop);
                p.add(controls, BorderLayout.WEST);
                p.add(dsp);
                this.setContentPane(p);
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
                this.setContentPane(p);
                break;
            case 5:
                controls = new JPanel(new BorderLayout());

                DefaultEventListModel<VirtualAppFrame> listmodel = GlazedListsSwing.eventListModel(DesktopManager.get().getFrames());

                final JList<VirtualAppFrame> jlist = new JList<>(listmodel);
                jlist.setCellRenderer(new VAppListCellRenderer());
                jlist.addListSelectionListener(DesktopManager.get()); // (new VAppListSelectionListener(jlist));
                controls.add(jlist, BorderLayout.CENTER); // (new JScrollPane(jlist), BorderLayout.CENTER);
            	DesktopManager.get().setObservedJList(jlist);

                dsp = new DesktopScrollPane(desktop);

                //Create a split pane with the two scroll panes in it.
                JSplitPane splitPane2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controls, dsp);
                splitPane2.setOneTouchExpandable(true);
                splitPane2.setDividerLocation(150);
                p.add(splitPane2);

                this.setContentPane(p);
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
        menuItem = new JMenuItem(new VActionLNF("Web",null,"com.alee.laf.WebLookAndFeel", this));
        menu.add(menuItem);

        menuItem = new JMenuItem(new VActionLNF("Nimbus",null,"javax.swing.plaf.nimbus.NimbusLookAndFeel", this));
        menu.add(menuItem);

        // TODO add Metal

        menuItem = new JMenuItem(new VActionLNF("System",null,UIManager.getSystemLookAndFeelClassName(), this));
        menu.add(menuItem);

        return menuBar;
    }

    //React to menu selections.
    @Override public void actionPerformed(ActionEvent e) {
        if ("new".equals(e.getActionCommand())) {
            try {
                // TODO This is just temporary; need to implement a real feature
                DesktopManager.get().createVApp( registeredApps[(++count % registeredApps.length)].newInstance() );
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
     * This currently exists ONLY to support the prototyping of integrating
     * the Groovy Console via the jvdConsole.groovy script.
     *
     * It is currently not envisioned that apps can access the 'desktop'
     * directly.  Rather, it is preferred that we implement an API/methods
     * to allow JInternalFrames to be added to the desktop.
     *
     * @return
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
     * Create the GUI and show it.
     * For thread safety, this method should be invoked
     * from the event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        App frame = new App();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Display the window.
        frame.setVisible(true);
    }

    public static void main(String[] args) {

        // Install a custom security manager to prevent guests from shutting down the desktop.
        System.setSecurityManager(new NoExitSecurityManager());

        // Global Initialization(s) [ Frameworks, etc. ]
        IconFontSwing.register(FontAwesome.getIconFont());
        IconFontSwing.register(GoogleMaterialDesignIcons.getIconFont());
        
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(
            new Runnable() { @Override public void run() {
                try {

                    //Make sure we have nice window decorations.
                    JFrame.setDefaultLookAndFeelDecorated(true);

                    // This does not appear to be working as expected?
                    // System.setProperty(WebLookAndFeel.PROPERTY_HONOR_USER_BORDERS, "true");

                    Properties props = new Properties();

                    int choice = CHOSEN_LAF;
                    switch (choice) {
                        case LAF_SYSTEM:
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
                                    .setCurrentTheme(MetalThemeManager.XP); // MODERN | AQUA | LOW_VISION | ...
                                    //.setCurrentTheme(MetalThemeManager.LARGE_FONT);
                                    //.setCurrentTheme(new DefaultMetalTheme());
                                    //.setCurrentTheme(new OceanTheme());
                            UIManager.setLookAndFeel(new MetalLookAndFeel());
                            break;
                        case LAF_PGS:
                            // Sep. 2018:  evaluating pgs
                            UIManager.setLookAndFeel("com.pagosoft.plaf.PgsLookAndFeel");
                            break;
                        case LAF_NAPKIN:
                            String[] themeNames = NapkinTheme.Manager.themeNames();
                            String themeToUse = "napkin"; // napkin | blueprint
                            NapkinTheme.Manager.setCurrentTheme(themeToUse);
                            LookAndFeel laf = new NapkinLookAndFeel();
                            UIManager.setLookAndFeel(laf);
                            break;
                        case LAF_JTATTOO:
                            props.put("subTextFont", "Consolas BOLD 10"); // ???
                            props.put("userTextFont", "Calibri PLAIN 14"); // JLabel, JCheckbox, Tab Titles, ... // Aluminium only respects:  TableHeaders, Checkboxes, (I assume RadioButtons), ...
                            props.put("menuTextFont", "Calibri PLAIN 12"); // JMenu, ...
                            props.put("systemTextFont", "Calibri PLAIN 14"); // JToolTip, ... Baskerville BOLD 24
                            props.put("controlTextFont", "Calibri PLAIN 14"); // JButton, ... // Aluminium does not respect this... well... maybe it does, I just don't know what components it affects yet?
                            props.put("windowTitleFont", "Calibri PLAIN 16"); // JFrame, (JInternalFrame I asume), ...

                        	AluminiumLookAndFeel.setCurrentTheme(props);
                        	UIManager.setLookAndFeel("com.jtattoo.plaf.aluminium.AluminiumLookAndFeel");
                        	System.out.println("LAF := JTattoo");
                        	break;
                        case LAF_FLATLAF:
                        	System.out.println("LAF := FlatLAF");

                        	int flatTheme = 2;
                        	switch (flatTheme) {
                        	case 1: UIManager.setLookAndFeel( new FlatLightLaf() ); break;
                        	case 2: UIManager.setLookAndFeel( new FlatDarkLaf() ); break;
                        	}

                        	break;
                        case LAF_SUBSTANCE:
// June 2020 : I am deprecating this LAF for now; it is not terrible... just not worth keeping.
//                            UIManager.setLookAndFeel(new SubstanceGraphiteLookAndFeel());
//                        	System.out.println("LAF := Substance");

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
                	System.out.println("LAF := (fallback)");

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
                createAndShowGUI();
            } }
        );
    }

}
