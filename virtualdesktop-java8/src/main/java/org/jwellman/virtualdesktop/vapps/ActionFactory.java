package org.jwellman.virtualdesktop.vapps;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;

import org.jwellman.dsp.DSP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class is responsible for creating all desktop actions
 * and their corresponding action components.
 *
 * @author rwellman
 */
public class ActionFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ActionFactory.class);
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
            LOG.error("VApps config file not found: {}", configFile.getAbsolutePath());
            LOG.error("The configuration file is mandatory. Please create config/vapps-config.json");
            throw new RuntimeException("Required configuration file not found: config/vapps-config.json");
        }

        try {
            vappsConfig = mapper.readValue(configFile, VappsConfig.class);
            LOG.info("Loaded VApps configuration version {}", vappsConfig.getVersion());

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
            LOG.error("Failed to load vapps configuration", ex);
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
                LOG.warn("Icon not found: {}-small, using fallback add196-small", iconKey);
                try {
                    smallIcon = DSP.Icons.getIcon("add196-small");
                } catch (Exception ex2) {
                    LOG.error("Fallback icon add196-small also not found");
                }
            }

            try {
                largeIcon = DSP.Icons.getIcon(iconKey + "-large");
            } catch (Exception ex) {
                LOG.warn("Icon not found: {}-large, using fallback add196-large", iconKey);
                try {
                    largeIcon = DSP.Icons.getIcon("add196-large");
                } catch (Exception ex2) {
                    LOG.error("Fallback icon add196-large also not found");
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

            // Pass configuration attributes for Configurable specs
            if (vappConfig.getAttrs() != null) {
                action.setAttrs(vappConfig.getAttrs());
            }

            getListOfActions().add(action);

        } catch (ClassNotFoundException ex) {
            LOG.error("Failed to register vapp: {} - class not found", vappConfig.getClassName(), ex);
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
            Icon smallIcon = null;

            // Try to load requested icon, fallback to add196 if not found
            try {
                largeIcon = DSP.Icons.getIcon(iconKey + "-large");
            } catch (Exception ex) {
                LOG.warn("Icon not found: {}-large, using fallback add196-large", iconKey);
                try {
                    largeIcon = DSP.Icons.getIcon("add196-large");
                } catch (Exception ex2) {
                    LOG.error("Fallback icon add196-large also not found");
                }
            }

            // Small variant — used for the internal frame / toolbar decoration when this
            // shortcut's vapp is launched; the large variant above is only for the desktop tile.
            try {
                smallIcon = DSP.Icons.getIcon(iconKey + "-small");
            } catch (Exception ex) {
                LOG.warn("Icon not found: {}-small, using fallback add196-small", iconKey);
                try {
                    smallIcon = DSP.Icons.getIcon("add196-small");
                } catch (Exception ex2) {
                    LOG.error("Fallback icon add196-small also not found");
                }
            }

            DesktopAction action = new DesktopAction(shortcut.getLabel());
            action.setDesktopOnly(true);
            action.setClazzName(shortcut.getClassName());

            if (largeIcon != null) {
                action.putValue(Action.LARGE_ICON_KEY, largeIcon);
            }
            if (smallIcon != null) {
                action.putValue(Action.SMALL_ICON, smallIcon);
            }

            getListOfActions().add(action);

        } catch (Exception ex) {
            LOG.error("Failed to register desktop shortcut: {}", shortcut.getLabel(), ex);
        }
    }

    /**
     * Loads external application definitions from a JSON configuration file.
     * If the file doesn't exist or cannot be read, this method fails silently.
     */
    private static void loadExternalApps() {
        File configFile = new File("config/external-apps.json");

        if (!configFile.exists()) {
            LOG.info("External apps config file not found: {}", configFile.getAbsolutePath());
            LOG.info("Skipping external app loading. Create this file to define external applications.");
            return;
        }

        try {
            ExternalAppsConfig config = mapper.readValue(configFile, ExternalAppsConfig.class);

            for (ExternalAppConfig appConfig : config.getExternalApps()) {
                registerExternalApp(appConfig);
            }

            LOG.info("Loaded {} external app(s) from configuration", config.getExternalApps().size());

        } catch (IOException ex) {
            LOG.error("Failed to load external apps configuration", ex);
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
                // Get icons from DSP.Icons registry using semantic size keys.
                // A missing key throws rather than returning null, so both lookups
                // must be guarded individually or a single miss aborts the whole
                // external app registration before the fallback below ever runs.
                Icon largeIcon = null;
                Icon smallIcon = null;
                try {
                    largeIcon = DSP.Icons.getIcon(iconValue + "-large");
                    smallIcon = DSP.Icons.getIcon(iconValue + "-small");
                } catch (Exception ex) {
                    LOG.warn("Icon not found in registry for {}: {}", appConfig.getName(), iconValue);
                    LOG.warn("Make sure the icon is in the auto-discovered directory or manually registered.");
                }

                if (largeIcon != null && smallIcon != null) {
                    action.putValue(Action.LARGE_ICON_KEY, largeIcon);
                    action.putValue(Action.SMALL_ICON, smallIcon);
                } else {
                    // Use default icon
                    try {
                        Icon defaultLargeIcon = DSP.Icons.getIcon("winking18-large");
                        Icon defaultSmallIcon = DSP.Icons.getIcon("winking18-small");
                        if (defaultLargeIcon != null && defaultSmallIcon != null) {
                            action.putValue(Action.LARGE_ICON_KEY, defaultLargeIcon);
                            action.putValue(Action.SMALL_ICON, defaultSmallIcon);
                        }
                    } catch (Exception ex2) {
                        LOG.error("Fallback icon winking18 also not found");
                    }
                }
            }

            action.putValue(Action.NAME, appConfig.getName());

            // Add to list of actions
            getListOfActions().add(action);

        } catch (Exception ex) {
            LOG.error("Failed to register external app: {}", appConfig.getName(), ex);
        }
    }

    /**
     * @return the listOfActions
     */
    public static List<DesktopAction> getListOfActions() {
        return listOfActions;
    }

}
