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
     * Loads vapps from JSON configuration file. The config file is mandatory.
     */
    private static void loadVappsConfig() {
        File configFile = new File("config/vapps-config.json");

        if (!configFile.exists()) {
            System.err.println("ERROR: VApps config file not found: " + configFile.getAbsolutePath());
            System.err.println("ERROR: The configuration file is mandatory. Please create config/vapps-config.json");
            throw new RuntimeException("Required configuration file not found: config/vapps-config.json");
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
            System.err.println("ERROR: Failed to load vapps configuration: " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Failed to load required configuration file", ex);
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

            Icon smallIcon = null;
            Icon largeIcon = null;

            // Try to load requested icon, fallback to add196 if not found
            try {
                smallIcon = DSP.Icons.getIcon(iconKey + "-small");
            } catch (Exception ex) {
                System.err.println("Icon not found: " + iconKey + "-small, using fallback add196-small");
                try {
                    smallIcon = DSP.Icons.getIcon("add196-small");
                } catch (Exception ex2) {
                    System.err.println("Fallback icon add196-small also not found");
                }
            }

            try {
                largeIcon = DSP.Icons.getIcon(iconKey + "-large");
            } catch (Exception ex) {
                System.err.println("Icon not found: " + iconKey + "-large, using fallback add196-large");
                try {
                    largeIcon = DSP.Icons.getIcon("add196-large");
                } catch (Exception ex2) {
                    System.err.println("Fallback icon add196-large also not found");
                }
            }

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

            Icon largeIcon = null;

            // Try to load requested icon, fallback to add196 if not found
            try {
                largeIcon = DSP.Icons.getIcon(iconKey + "-large");
            } catch (Exception ex) {
                System.err.println("Icon not found: " + iconKey + "-large, using fallback add196-large");
                try {
                    largeIcon = DSP.Icons.getIcon("add196-large");
                } catch (Exception ex2) {
                    System.err.println("Fallback icon add196-large also not found");
                }
            }

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
