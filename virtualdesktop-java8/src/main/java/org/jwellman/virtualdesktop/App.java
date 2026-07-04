package org.jwellman.virtualdesktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Properties;

import javax.swing.Action;
import javax.swing.DefaultDesktopManager;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel ;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import org.jwellman.dsp.DSP;
import org.jwellman.dsp.DirectoryIconProvider;
import org.jwellman.dsp.FontAwesomeIconProvider;
import org.jwellman.dsp.GoogleMaterialIconProvider;
import org.jwellman.dsp.icons.IconSpecifier;
import org.jwellman.swing.plaf.metal.MetalThemeManager ;
import org.jwellman.swing.thirdparty.BetterMemoryMonitor;
import org.jwellman.virtualdesktop.desktop.DialogManager;
import org.jwellman.virtualdesktop.desktop.IconRegistryLoader;
import org.jwellman.virtualdesktop.desktop.VActionLNF;
import org.jwellman.virtualdesktop.desktop.VException;
import org.jwellman.virtualdesktop.desktop.VShortcut;
import org.jwellman.virtualdesktop.desktopmgr.VAppListCellRenderer;
import org.jwellman.virtualdesktop.security.NoExitSecurityManager;
import org.jwellman.virtualdesktop.state.reducers.AppReducer;
import org.jwellman.virtualdesktop.state.store.AppStore;
import org.jwellman.virtualdesktop.state.store.LoggingMiddleware;
import org.jwellman.virtualdesktop.taskbar.SmartGridWindowListView;
import org.jwellman.virtualdesktop.taskbar.WindowListController;
import org.jwellman.virtualdesktop.vapps.ActionFactory;
import org.jwellman.virtualdesktop.vapps.DesktopAction;
import org.jwellman.virtualdesktop.vapps.MenuGroup;
import org.jwellman.virtualdesktop.vapps.VappConfig;
import org.jwellman.virtualdesktop.vapps.VappsConfig;
import org.jwellman.virtualdesktop.vapps.VirtualAppSpec;
import org.jwellman.virtualdesktop.vswing.VDesktopPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alee.laf.WebLookAndFeel;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.jtattoo.plaf.aluminium.AluminiumLookAndFeel;

import ca.odell.glazedlists.swing.DefaultEventListModel;
import ca.odell.glazedlists.swing.GlazedListsSwing;
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

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    /** The singleton */
    private static App app;

    /** the desktop */
    private JDesktopPane desktop;

    private JMenu appMenu;

    /** a custom scrollpane for a scrollable desktop */
    private DesktopScrollPane dsp;

    /** Redux-backed window list controller (version 6+) */
    @SuppressWarnings("unused")
    private WindowListController windowListController;

    /** Shared skin action list — same instances used by the Skin menu and the settings dialog. */
    private VActionLNF[] skinActions;

    // These are a workaround because VirtualAppFrame needs to know the LAF
    // for a workaround that it employs for WEBLAF only
    public static final int LAF_SYSTEM = 1;
    public static final int LAF_WEBLAF = 2;
    public static final int LAF_NIMBUS = 3;
    public static final int LAF_METAL = 5;
    public static final int LAF_PGS = 6;
    public static final int LAF_JTATTOO = 7; // definitely like this one - almost modern, somewhat Mac like
    public static final int LAF_FLATLAF = 8; // definitely like this one - modern but flat (which isn't necessarily bad... just sayin')
    public static final int LAF_SUBSTANCE = 9; // deprecated / not currently used
    public static final int LAF_NAPKIN = 10;
    public static final int CHOSEN_LAF = LAF_FLATLAF; // LAF_FLATLAF; // LAF_JTATTOO;

    /**
     * Having direct access to this object as a JFrame is not really
     * what I want so this will have to be refactored at some point.
     * 
     * There are two different use cases that need to be supported when
     * the factoring occurs:
     * 1. Return an object whose interface is "the application"
     * 2. Return the outermost application JFrame
     * 
     * @return
     */
    static public App getVSystem() {
        return app;
    }

    private App() {
        super("Java Virtual Desktop");
        App.app = this;

        //Make the big window be indented 50 pixels from each edge of the screen.
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(0, 0, screenSize.width , screenSize.height );
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        setJMenuBar(createMenuBar());

        //Set up the GUI.
        JPanel p = new JPanel(new BorderLayout());

        JPanel controls = null;

        desktop = new VDesktopPane(); // new JDesktopPane(); //a specialized layered pane
        DesktopManager.get().setDesktop(desktop);

        int version = 6; // 5=GlazedLists, 6=Redux WindowListController
        switch (version) {
            // previous versions are in source control if needed
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

            case 6:
                // Redux-backed window list using WindowListController
                controls = new JPanel(new BorderLayout());

                // JListWindowListView windowListView = new JListWindowListView();
                SmartGridWindowListView windowListView = new SmartGridWindowListView();
                this.windowListController = new WindowListController(windowListView);
                controls.add(windowListView.getComponent(), BorderLayout.CENTER);

                // Still need to set observed JList for DesktopManager compatibility
                // Create a dummy list for now - DesktopManager will be refactored later
                DefaultEventListModel<VirtualAppFrame> legacyModel = GlazedListsSwing.eventListModel(DesktopManager.get().getFrames());
                JList<VirtualAppFrame> legacyList = new JList<>(legacyModel);
                DesktopManager.get().setObservedJList(legacyList);

                // Power-off button panel at the very bottom of the taskbar
                JPanel powerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
                JButton settingsButton = new JButton(DSP.Icons.getIcon("settings-small"));
                settingsButton.setOpaque(false);
                settingsButton.setContentAreaFilled(false);
                settingsButton.setBorderPainted(false);
                settingsButton.setFocusPainted(false);
                settingsButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        DialogManager.showSkinDialog(getSkinActions());
                    }
                });
                powerPanel.add(settingsButton);

                JButton powerButton = new JButton(DSP.Icons.getIcon("power_off-small"));
//                powerButton.setOpaque(false);
                powerButton.setBackground(Color.red);
//                powerButton.setContentAreaFilled(false);
//                powerButton.setBorderPainted(false);
//                powerButton.setFocusPainted(false);
                powerButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (DialogManager.confirmAreYouSure()) {
                            quit();
                        }
                    }
                });
                powerPanel.add(powerButton);

                // Memory monitor panel above the power button
                BetterMemoryMonitor betterMemoryMonitor = new BetterMemoryMonitor();
                JPanel memMonitorPanel = betterMemoryMonitor.getMemoryMonitor();
                betterMemoryMonitor.start();

                // Stack: memory monitor CENTER, power button SOUTH
                JPanel taskbarBottomPanel = new JPanel(new BorderLayout());
                taskbarBottomPanel.add(memMonitorPanel, BorderLayout.CENTER);
                taskbarBottomPanel.add(powerPanel, BorderLayout.SOUTH);

                controls.add(taskbarBottomPanel, BorderLayout.SOUTH);

                dsp = new DesktopScrollPane(desktop);

                JSplitPane splitPane3 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controls, dsp);
                splitPane3.setOneTouchExpandable(true);
                splitPane3.setDividerLocation(230);
                p.add(splitPane3);

                this.setContentPane(p);
                break;

        }

        //Make dragging a little faster but perhaps uglier.
        desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
        desktop.setDesktopManager(new SnappingDesktopManager(20));

        DesktopAction.setDesktop(this);
        ActionFactory.initDesktop();

        // These are for desktop layout... this is VERY inelegant...
        // this needs to be from persistence mechanism ...
        // and overall, there needs to be a "layout manager" for the desktop
        int x = 10; int y = -70;

        // Build hierarchical menu structure
        buildVAppsMenu();

        // Add desktop shortcuts
        for (DesktopAction a : ActionFactory.getListOfActions()) {
            if (a.isDesktopOnly()) {
                final Icon icon = (Icon) a.getValue(Action.LARGE_ICON_KEY);
                final String label = (String) a.getValue(Action.NAME);
                final VShortcut vs = new VShortcut(a, label, icon, x, y+=80);
                desktop.add(vs);
            }
        }
        
        // Initialize the docking service
        // Note: This could also be done via SpecDocking.setJFrame(this) for backward compatibility
        VirtualAppSpec.setJFrame(this);

    }

    private VActionLNF[] getSkinActions() {
        if (skinActions == null) {
            skinActions = new VActionLNF[] {
                new VActionLNF("Web",    null, "com.alee.laf.WebLookAndFeel", this),
                new VActionLNF("Nimbus", null, "javax.swing.plaf.nimbus.NimbusLookAndFeel", this),
                new VActionLNF("System", null, UIManager.getSystemLookAndFeelClassName(), this)
            };
        }
        return skinActions;
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

        appMenu = new JMenu("Tools");
        appMenu.setMnemonic(KeyEvent.VK_V);
        menuBar.add(appMenu);

        menu = new JMenu("Skin");
        menu.setMnemonic(KeyEvent.VK_K);
        menuBar.add(menu);

        // TODO for sake of precision, it should probably be determined that each
        // lookandfeel is available before adding it to the menu.

        for (VActionLNF action : getSkinActions()) {
            menu.add(new JMenuItem(action));
        }

        return menuBar;
    }

    /**
     * React to menu selections.
     * 
     * TODO This is just a placeholder; need to determine how menus
     * are globally handled and implement that feature.
     */
    @Override public void actionPerformed(ActionEvent e) {
        if ("exit".equals(e.getActionCommand())) {
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
     * Build the VApps menu structure from configuration.
     */
    private void buildVAppsMenu() {
        VappsConfig config = ActionFactory.getVappsConfig();

        if (config == null) {
            logger.error("No VApps configuration loaded - cannot build menu");
            return;
        }

        // Build hierarchical menu from config
        for (MenuGroup menuGroup : config.getMenuStructure()) {
            if (menuGroup.isInlineType()) {
                // Inline type: add vapps directly to appMenu without creating submenu
                addVappsToMenu(appMenu, menuGroup);
            } else {
                // Regular type: create submenu
                JMenu topLevelMenu = createMenuFromGroup(menuGroup);
                appMenu.add(topLevelMenu);
            }
        }
    }

    /**
     * Create a JMenu from a MenuGroup configuration
     * @param group the menu group configuration
     * @return a JMenu populated with vapps and subgroups
     */
    private JMenu createMenuFromGroup(MenuGroup group) {
        JMenu menu = new JMenu(group.getLabel());

        // Set mnemonic if specified
        if (group.getMnemonic() != null && !group.getMnemonic().isEmpty()) {
            menu.setMnemonic(group.getMnemonic().charAt(0));
        }

        // Add vapps at this level using helper method
        addVappsToMenu(menu, group);

        // Recursively add subgroups
        if (group.isGroupType()) {
            for (MenuGroup subgroup : group.getGroups()) {
                JMenu submenu = createMenuFromGroup(subgroup);
                menu.add(submenu);
            }
        }

        return menu;
    }

    /**
     * Check if an action class belongs to a menu group
     * @param actionClass the fully qualified class name
     * @param group the menu group to check
     * @return true if the action belongs to this group
     */
    private boolean belongsToMenuGroup(String actionClass, MenuGroup group) {
        // Check vapps at this level
        for (VappConfig vappConfig : group.getVapps()) {
            if (vappConfig.getClassName().equals(actionClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add vapps from a MenuGroup directly to a target menu
     * @param targetMenu the menu to add items to
     * @param group the menu group containing vapps to add
     */
    private void addVappsToMenu(JMenu targetMenu, MenuGroup group) {
        // Add vapps at this level (non-desktopOnly)
        for (DesktopAction action : ActionFactory.getListOfActions()) {
            if (!action.isDesktopOnly()) {
                String actionClass = (String) action.getValue(Action.ACTION_COMMAND_KEY);
                if (actionClass == null) {
                    actionClass = action.getClazzName();
                }

                // Check if this action belongs to this menu group
                if (belongsToMenuGroup(actionClass, group)) {
                    targetMenu.add(action);
                }
            }
        }
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

    private static void createTheme() {

        // 1. Define your new typography theme
        // (Monospaced or a clean sans-serif like Segoe UI / Inter)
        Font customDefaultFont = new Font("Segoe UI", Font.PLAIN, 13);

        // 2. Set FlatLaf's global font UI default key BEFORE L&F setup
        UIManager.put("defaultFont", customDefaultFont);

        // Initialize ThemeManager first - loads user's preferred theme from preferences.json
        org.jwellman.virtualdesktop.theme.ThemeManager.getInstance().initialize();

        DSP.Icons.registerProvider("FontAwesome",       new FontAwesomeIconProvider());
        DSP.Icons.registerProvider("GoogleMaterial",    new GoogleMaterialIconProvider());

        // Create and configure DirectoryIconProvider for the global UI icons
        DirectoryIconProvider directoryProvider = new DirectoryIconProvider("org/jwellman/virtualdesktop/images/global_ui");
        DSP.Icons.registerProvider("Directory", directoryProvider);

        // Auto-discover and register all icons using semantic sizes from the active theme
        // Icons will be registered with both semantic keys ("home156-small") and pixel keys ("home156-16") for backward compatibility
        directoryProvider.discoverAndRegisterIcons("Directory",
            org.jwellman.virtualdesktop.theme.IconSize.SMALL,
            org.jwellman.virtualdesktop.theme.IconSize.MENU,
            org.jwellman.virtualdesktop.theme.IconSize.TOOLBAR,
            org.jwellman.virtualdesktop.theme.IconSize.LARGE,
            org.jwellman.virtualdesktop.theme.IconSize.XLARGE);

        // TODO create something like DSP.Theme for color definitions (I think I have some previous notes somewhere)
        Color iconColor = Color.lightGray;

        DSP.Icons.register("jpad.java", new IconSpecifier( "FontAwesome", "COFFEE", 18, null, iconColor, Color.white) );
        DSP.Icons.register("jpad.eye", new IconSpecifier( "FontAwesome", "EYE", 18, null, iconColor, Color.white) );
        DSP.Icons.register("jpad.bsh_class_browser", new IconSpecifier( "FontAwesome", "EYE", 18, null, iconColor, Color.white) );
        DSP.Icons.register("jpad.calendar", new IconSpecifier( "FontAwesome", "CALENDAR", 18, null, iconColor, Color.white) );
        DSP.Icons.register("jpad.cog", new IconSpecifier( "FontAwesome", "COG", 18, null, iconColor, Color.white) );
        DSP.Icons.register("jpad.leaf", new IconSpecifier( "FontAwesome", "LEAF", 18, null, iconColor, Color.white) );
        DSP.Icons.register("jpad.check", new IconSpecifier( "FontAwesome", "CHECK", 18, null, iconColor, Color.white) );
        DSP.Icons.register("jpad.clock", new IconSpecifier( "FontAwesome", "CLOCK_O", 14, null, Color.white, Color.white) );

        IconRegistryLoader.load();

    }

    /**
     * Initialize the Redux-style state store with reducer and middleware.
     */
    private static void initializeStore() {
        AppStore store = AppStore.get();
        store.setReducer(new AppReducer());
        store.addMiddleware(new LoggingMiddleware());
        logger.info("Redux store initialized");
    }

    public static void main(String[] args) {

        // Install a custom security manager to prevent guests from shutting down the desktop.
        System.setSecurityManager(new NoExitSecurityManager());

        // Global Initialization(s) [ Frameworks, etc. ]
        createTheme();

        // Initialize Redux-style state store
        initializeStore();

        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(
            new Runnable() { @Override public void run() {
                try {

                    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                        logger.debug("Installed LAF: {} : {}", info.getName(), info.getClassName());
                    }

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
                        case LAF_WEBLAF:
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
                            @SuppressWarnings("unused") String[] themeNames = NapkinTheme.Manager.themeNames();
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
                        	logger.info("LAF := JTattoo");
                        	break;
                        case LAF_FLATLAF:
                        	logger.info("LAF := FlatLAF");

                        	int flatTheme = 6;
                        	switch (flatTheme) {
                        	case 1: UIManager.setLookAndFeel( new FlatLightLaf() ); break;
                        	case 2: UIManager.setLookAndFeel( new FlatDarkLaf() ); break;
                        	case 3: UIManager.setLookAndFeel( new FlatDarculaLaf() ); break;
                        	case 4: UIManager.setLookAndFeel( new FlatIntelliJLaf() ); break;
                        	case 5: UIManager.setLookAndFeel( new FlatMacLightLaf() ); break;
                        	case 6: UIManager.setLookAndFeel( new FlatMacDarkLaf() ); break;
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
                                    logger.debug("{} : {}", info.getName(), info.getClassName());
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
                	logger.warn("LAF := (fallback)");

                    // If Nimbus is not available, you can set the GUI to another look and feel.
                    final String sys = UIManager.getSystemLookAndFeelClassName();
                    try {
                        UIManager.setLookAndFeel(sys);
                    } catch (ClassNotFoundException ex) {
                        logger.error("Failed to set fallback LAF", ex);
                    } catch (InstantiationException ex) {
                        logger.error("Failed to set fallback LAF", ex);
                    } catch (IllegalAccessException ex) {
                        logger.error("Failed to set fallback LAF", ex);
                    } catch (UnsupportedLookAndFeelException ex) {
                        logger.error("Failed to set fallback LAF", ex);
                    }
                }
                createAndShowGUI();
            } }
        );
    }

    public class SnappingDesktopManager extends DefaultDesktopManager {

        private final int gridSize;

        public SnappingDesktopManager(int gridSize) {
            this.gridSize = gridSize;
        }

        @Override
        public void setBoundsForFrame(JComponent f, int newX, int newY, int newWidth, int newHeight) {
            // Snap to grid
            int snappedX = (newX / gridSize) * gridSize;
            int snappedY = (newY / gridSize) * gridSize;
            
            // Optionally snap the width/height too if you want perfect squares
            int snappedWidth = ((newWidth + gridSize / 2) / gridSize) * gridSize;
            int snappedHeight = ((newHeight + gridSize / 2) / gridSize) * gridSize;

            super.setBoundsForFrame(f, snappedX, snappedY, snappedWidth, snappedHeight);
        }
    }

}
