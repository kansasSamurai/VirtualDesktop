package org.jwellman.virtualdesktop.vapps;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;

import org.jwellman.dsp.DSP;
import org.jwellman.vfsjfilechooser2.SpecVfsFileChooser2;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class is responsible for creating all desktop actions
 * and their corresponding action components.
 *
 * @author rwellman
 */
public class ActionFactory {

    public static Class<?>[] registeredApps = {
         SpecBeanShell.class
         ,SpecJCXConsole.class
         ,SpecScar.class
        ,SpecJetty.class
        ,SpecHyperSQL.class
        ,SpecHyperSQLClient.class
        ,SpecJDatePicker.class
        ,SpecJFreeChart.class
        ,SpecXChartDemo.class
        ,SpecXChartBarChart.class
        ,SpecSVGViewer.class
        ,SpecVfsFileChooser2.class // this isn't as bad as I originally thought... I was confusing it with XionFM and/or SpecJzy3D below.
        ,SpecUberDragAndDrop.class
//      ,SpecXionFM.class // this app is targeted for Linux
        ,SpecDocking.class
        ,SpecJediTerm.class
        ,SpecGroovyConsole.class // newly readded
        ,SpecGroovyGraphics.class
        ,SpecThemeSelector.class // Theme selection vapp
//      ,SpecJzy3D.class // this app sucks
    };

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final List<DesktopAction> listOfActions = new ArrayList<>();

    private static VappsConfig vappsConfig = null;

    /**
     * Get the loaded vapps configuration
     * @return the vapps configuration, or null if not loaded
     */
    public static VappsConfig getVappsConfig() {
        return vappsConfig;
    }

    public static void initDesktop() {
        // Load vapps from config (with fallback to hardcoded)
        loadVappsConfig();

        // Load external applications from configuration file
        loadExternalApps();
    }

    /**
     * Loads vapps from JSON configuration file or falls back to hardcoded registrations
     */
    private static void loadVappsConfig() {
        File configFile = new File("config/vapps-config.json");

        if (!configFile.exists()) {
            System.out.println("VApps config file not found: " + configFile.getAbsolutePath());
            System.out.println("Using hardcoded vapp registrations (legacy mode)");
            loadHardcodedApps();
            return;
        }

        try {
            vappsConfig = mapper.readValue(configFile, VappsConfig.class);
            System.out.println("Loaded VApps configuration version " + vappsConfig.getVersion());

            // Process menu vapps
            for (MenuGroup menuGroup : vappsConfig.getMenuStructure()) {
                processMenuGroup(menuGroup, vappsConfig.getDefaultIcon());
            }

            // Process desktop shortcuts
            for (DesktopShortcut shortcut : vappsConfig.getDesktopShortcuts()) {
                if (shortcut.isEnabled()) {
                    registerDesktopShortcut(shortcut, vappsConfig.getDefaultIcon());
                }
            }

        } catch (IOException ex) {
            System.err.println("Failed to load vapps configuration: " + ex.getMessage());
            ex.printStackTrace();
            System.err.println("Falling back to hardcoded vapp registrations");
            loadHardcodedApps();
        }
    }

    /**
     * Recursively process a menu group and register all vapps within it
     * @param group the menu group to process
     * @param defaultIcon the default icon key to use if vapp has none
     */
    private static void processMenuGroup(MenuGroup group, String defaultIcon) {
        // Process vapps at this level
        for (VappConfig vappConfig : group.getVapps()) {
            if (vappConfig.isEnabled()) {
                registerVapp(vappConfig, defaultIcon);
            }
        }

        // Recursively process nested groups
        for (MenuGroup subgroup : group.getGroups()) {
            processMenuGroup(subgroup, defaultIcon);
        }
    }

    /**
     * Register a vapp from configuration
     * @param vappConfig the vapp configuration
     * @param defaultIcon the default icon key to use if vapp has none
     */
    private static void registerVapp(VappConfig vappConfig, String defaultIcon) {
        try {
            // Determine title
            String title = vappConfig.getTitle();
            if (title == null || title.isEmpty()) {
                Class<?> clazz = Class.forName(vappConfig.getClassName());
                title = clazz.getSimpleName();
            }

            DesktopAction action = new DesktopAction(title);

            // Set icon
            String iconKey = vappConfig.getIcon();
            if (iconKey == null || iconKey.isEmpty()) {
                iconKey = defaultIcon;
            }

            Icon smallIcon = DSP.Icons.getIcon(iconKey + "-small");
            Icon largeIcon = DSP.Icons.getIcon(iconKey + "-large");

            if (smallIcon != null) {
                action.putValue(Action.SMALL_ICON, smallIcon);
            }
            if (largeIcon != null) {
                action.putValue(Action.LARGE_ICON_KEY, largeIcon);
            }

            action.putValue(Action.ACTION_COMMAND_KEY, vappConfig.getClassName());
            action.setDesktopOnly(vappConfig.isDesktopOnly());

            getListOfActions().add(action);

        } catch (ClassNotFoundException ex) {
            System.err.println("Failed to register vapp: " + vappConfig.getClassName());
            System.err.println("Class not found - check that the class name is correct");
            ex.printStackTrace();
        }
    }

    /**
     * Register a desktop shortcut from configuration
     * @param shortcut the desktop shortcut configuration
     * @param defaultIcon the default icon key to use if shortcut has none
     */
    private static void registerDesktopShortcut(DesktopShortcut shortcut, String defaultIcon) {
        try {
            String iconKey = shortcut.getIcon();
            if (iconKey == null || iconKey.isEmpty()) {
                iconKey = defaultIcon;
            }

            Icon largeIcon = DSP.Icons.getIcon(iconKey + "-large");

            DesktopAction action = new DesktopAction(shortcut.getLabel());
            action.setDesktopOnly(true);
            action.setClazzName(shortcut.getClassName());

            if (largeIcon != null) {
                action.putValue(Action.LARGE_ICON_KEY, largeIcon);
            }

            getListOfActions().add(action);

        } catch (Exception ex) {
            System.err.println("Failed to register desktop shortcut: " + shortcut.getLabel());
            ex.printStackTrace();
        }
    }

    /**
     * Fallback method that loads vapps using the hardcoded registeredApps array
     * This preserves legacy behavior when no config file is present
     */
    private static void loadHardcodedApps() {
        DesktopAction a = null;

        for (Class<?> clazz : registeredApps) {
            a = new DesktopAction(clazz.getSimpleName());
            getListOfActions().add(a);

            Icon icon = DSP.Icons.getIcon("home156-small");
            a.putValue(Action.SMALL_ICON, icon);
            a.putValue(Action.ACTION_COMMAND_KEY, clazz.getCanonicalName());
        }

        // Hardcoded desktop shortcuts
        String[] labels = {"Home", "Calendar", "Office Writer", "Trash"};
        String[] icons = {"home156", "calendar168", "document176", "rubbish1"};
        String[] clazzs = {"SpecJCXConsole", "SpecJCXConsole", "SpecJCXConsole", "SpecJCXConsole"};

        for (int i = 0; i < labels.length; i++) {
            final Icon icon = DSP.Icons.getIcon(icons[i] + "-large");

            a = new DesktopAction(labels[i]);
            a.setDesktopOnly(true);
            a.setClazzName("org.jwellman.virtualdesktop.vapps." + clazzs[i]);
            a.putValue(Action.LARGE_ICON_KEY, icon);
            getListOfActions().add(a);
        }
    }

    /**
     * Loads external application definitions from a JSON configuration file.
     * If the file doesn't exist or cannot be read, this method fails silently.
     */
    private static void loadExternalApps() {
        File configFile = new File("config/external-apps.json");

        if (!configFile.exists()) {
            System.out.println("External apps config file not found: " + configFile.getAbsolutePath());
            System.out.println("Skipping external app loading. Create this file to define external applications.");
            return;
        }

        try {
            ExternalAppsConfig config = mapper.readValue(configFile, ExternalAppsConfig.class);

            for (ExternalAppConfig appConfig : config.getExternalApps()) {
                registerExternalApp(appConfig);
            }

            System.out.println("Loaded " + config.getExternalApps().size() + " external app(s) from configuration");

        } catch (IOException ex) {
            System.err.println("Failed to load external apps configuration: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Registers an external application as a desktop action
     * @param appConfig the external app configuration
     */
    private static void registerExternalApp(ExternalAppConfig appConfig) {
        try {
            // Create the action
            ExternalAppAction action = new ExternalAppAction(
                appConfig.getName(),
                appConfig.getCommand(),
                appConfig.getWorkingDir(),
                appConfig.isWaitForCompletion()
            );
            action.setClazzName("org.jwellman.virtualdesktop.vapps.ExternalAppSpec");

            // Set desktop-only flag
            action.setDesktopOnly(appConfig.isDesktopOnly());

            // Load icon if specified
            final String iconValue = appConfig.getIcon();
            if (iconValue != null && !iconValue.isEmpty()) {
                // Get icons from DSP.Icons registry using semantic size keys
                Icon largeIcon = DSP.Icons.getIcon(iconValue + "-large");
                Icon smallIcon = DSP.Icons.getIcon(iconValue + "-small");

                if (largeIcon != null && smallIcon != null) {
                    action.putValue(Action.LARGE_ICON_KEY, largeIcon);
                    action.putValue(Action.SMALL_ICON, smallIcon);
                } else {
                    System.err.println("Icon not found in registry for " + appConfig.getName() + ": " + iconValue);
                    System.err.println("Make sure the icon is in the auto-discovered directory or manually registered.");
                    // Use default icon
                    Icon defaultLargeIcon = DSP.Icons.getIcon("winking18-large");
                    Icon defaultSmallIcon = DSP.Icons.getIcon("winking18-small");
                    if (defaultLargeIcon != null && defaultSmallIcon != null) {
                        action.putValue(Action.LARGE_ICON_KEY, defaultLargeIcon);
                        action.putValue(Action.SMALL_ICON, defaultSmallIcon);
                    }
                }
            }

            action.putValue(Action.NAME, appConfig.getName());

            // Add to list of actions
            getListOfActions().add(action);

        } catch (Exception ex) {
            System.err.println("Failed to register external app: " + appConfig.getName());
            ex.printStackTrace();
        }
    }

    /**
     * @return the listOfActions
     */
    public static List<DesktopAction> getListOfActions() {
        return listOfActions;
    }

}
