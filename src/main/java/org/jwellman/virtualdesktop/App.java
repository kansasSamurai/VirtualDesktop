package org.jwellman.virtualdesktop;

import java.awt.*;
import java.awt.event.*;
import java.security.Permission;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import org.jwellman.virtualdesktop.security.NoExitSecurityManager;
import org.jwellman.virtualdesktop.vapps.*;

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
        setBounds(inset, inset, screenSize.width  - inset*10, screenSize.height - inset*2);

        //Set up the GUI.
        desktop = new JDesktopPane(); //a specialized layered pane
        dsp = new DesktopScrollPane(desktop);
        setContentPane(dsp); //(desktop);
        setJMenuBar(createMenuBar());

        createVApp(new SpecEmpty()); //create first "window"

        //Make dragging a little faster but perhaps uglier.
        desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);

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

        menu = new JMenu("VApps");
        menu.setMnemonic(KeyEvent.VK_V);
        menuBar.add(menu);
        for (Class clazz : registeredApps) {
            menuItem = new JMenuItem(clazz.getSimpleName());
            menuItem.setActionCommand(clazz.getCanonicalName());
            menuItem.addActionListener(this);
            menu.add(menuItem);
        }

        return menuBar;
    }

    //React to menu selections.
    public void actionPerformed(ActionEvent e) {
        if ("new".equals(e.getActionCommand())) {
            try {
                createVApp( registeredApps[(++count % registeredApps.length)].newInstance() );
            } catch (InstantiationException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if ("exit".equals(e.getActionCommand())) {
            quit();
        } else {
            try {
                createVApp( Class.forName(e.getActionCommand()).newInstance() );
            } catch (InstantiationException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    protected void createVApp(Object newInstance) {
        this.createVApp((VirtualAppSpec)newInstance);
    }

    /**
     * Create a new application.
     * [This is the definitive method of the overloaded versions.]
     *
     * @param spec
     */
    protected void createVApp(final VirtualAppSpec spec) {

        this.createVApp(spec.getContent(), spec.getTitle(), null);

//        javax.swing.SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
//                try {
//                    final VirtualAppFrame frame = new VirtualAppFrame(spec.getTitle());
//                    frame.setContentPane(spec.getContent());
//                    frame.setSize(spec.getWidth(), spec.getHeight());
//                    frame.setVisible(true); //necessary as of 1.3
//                    desktop.add(frame);
//                    frame.setSelected(true);
//                } catch (java.beans.PropertyVetoException e) {
//                    e.printStackTrace(); // for now, simply swallow the exception
//                } catch (Exception e) {
//                    e.printStackTrace(); // for now, simply swallow the exception
//                }
//            }
//        });

    }

    /**
     * This public method allows internal apps to create internal apps/windows.
     * i.e. via beanshell or others
     *
     * @param c
     * @param title
     */
    public VirtualAppFrame createVApp(final Container c, final String title) {
        return this.createVApp(c, title, null);
    }

    /**
     * This public method allows internal apps to create internal apps/windows.
     * i.e. via beanshell or others
     *
     * @param c
     * @param title
     */
    public VirtualAppFrame createVApp(final Container c, final String title, final Icon icon) {

        final VirtualAppFrame frame = new VirtualAppFrame(title);

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    frame.setContentPane(c);
                    if ((c.getWidth() * c.getHeight()) != 0) frame.setSize(c.getWidth(), c.getHeight());
                    if (icon != null) frame.setFrameIcon(icon);
                    frame.pack(); // see Note [1] below
                    frame.setVisible(true); //necessary as of 1.3
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
        //Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);

        //Create and set up the window.
        App frame = new App();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Display the window.
        frame.setVisible(true);
    }

// TODO move this out of this class and into its own package
//    public static class ExitException extends SecurityException

// TODO move this out of this class and into its own package
//    private static class NoExitSecurityManager extends SecurityManager

    public JDesktopPane getDesktop() {
        return this.desktop;
    }

    public static void main(String[] args) {

        // Improve visual look; may affect performance... we'll see
        // Initial testing does seem to slow initial display of content so disabling for now
        // System.setProperty("swing.aatext", "true");

        // Install a custom security manager to prevent guests from shutting down the desktop.
        System.setSecurityManager(new NoExitSecurityManager());

        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(
            new Runnable() { public void run() {
                try {
                    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                        if ("Nimbus".equals(info.getName())) {
                            UIManager.setLookAndFeel(info.getClassName());
                            break;
                        }
                    }
                } catch (Exception e) {
                    // If Nimbus is not available, you can set the GUI to another look and feel.
                }
                createAndShowGUI();
            } }
        );
    }

}