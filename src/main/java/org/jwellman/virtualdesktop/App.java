package org.jwellman.virtualdesktop;

import com.alee.laf.WebLookAndFeel;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import static org.jwellman.virtualdesktop.App.registeredApps;
import org.jwellman.virtualdesktop.desktop.VActionLNF;
import org.jwellman.virtualdesktop.desktop.VException;
import org.jwellman.virtualdesktop.desktop.VShortcut;
import org.jwellman.virtualdesktop.security.NoExitSecurityManager;
import org.jwellman.virtualdesktop.vapps.*;
import org.jwellman.virtualdesktop.vswing.VDesktopPane;

/**
 * A Virtual Desktop.
 *
 * @author Rick Wellman
 */
public class App extends JFrame implements ActionListener {

    /** The singleton */
    private static App app;

    /** the desktop */
    private JDesktopPane desktop;

    private JMenu appMenu;

    /** a custom scrollpane for a scrollable desktop */
    private DesktopScrollPane dsp;

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
        int version = 4;
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

        }

        // createVApp(new SpecBeanShell()); //(new SpecEmpty()); //create first "window"

        //Make dragging a little faster but perhaps uglier.
        desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);

        DesktopAction.setDesktop(this);
        ActionFactory.initDesktop();
        JMenuItem m = null;

        int x = 10; int y = -70;
        for (DesktopAction a : ActionFactory.getListOfActions()) {
            if (a.isDesktopOnly()) {
                final Icon icon = (Icon) a.getValue(Action.LARGE_ICON_KEY);
                final String label = (String) a.getValue(Action.NAME);
                final VShortcut vs = new VShortcut(label, icon, x, y+=80);
                desktop.add(vs);
            } else {
                m = new JMenuItem(a);
                appMenu.add(m);
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

        menuItem = new JMenuItem(new VActionLNF("Nimbus",null,"javax.swing.plaf.nimbus.NimbusLookAndFeel", this));
        menu.add(menuItem);

        menuItem = new JMenuItem(new VActionLNF("Web",null,"com.alee.laf.WebLookAndFeel", this));
        menu.add(menuItem);

        menuItem = new JMenuItem(new VActionLNF("System",null,UIManager.getSystemLookAndFeelClassName(), this));
        menu.add(menuItem);

        return menuBar;
    }

    //React to menu selections.
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

    public void createVApp(Object newInstance) {
        this.createVApp((VirtualAppSpec)newInstance);
    }

    /**
     * Create a new application.
     * [This is the definitive method of the overloaded versions.]
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

                    if ((c.getWidth() * c.getHeight()) != 0) {
                        frame.setSize(c.getWidth(), c.getHeight());
                    } else {
                        frame.pack(); // see Note [1] below
                    }

                    frame.setVisible(true); //necessary as of 1.3
                    // frame.setSelected(true);
                    desktop.add(frame);
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
    }

    /**
     * Quit the application (not sure if there is "cleanup" to do)
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

        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(
            new Runnable() { @Override public void run() {
                try {

                    //Make sure we have nice window decorations.
                    JFrame.setDefaultLookAndFeelDecorated(true);

                    // This does not appear to be working as expected?
                    System.setProperty(WebLookAndFeel.PROPERTY_HONOR_USER_BORDERS, "true");

                    int choice = 1;
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
                createAndShowGUI();
            } }
        );
    }

}
