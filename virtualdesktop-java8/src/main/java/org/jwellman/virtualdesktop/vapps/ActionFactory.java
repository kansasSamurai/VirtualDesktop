package org.jwellman.virtualdesktop.vapps;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.Icon;

import org.jwellman.dsp.DSP;
import org.jwellman.virtualdesktop.DesktopManager;
import org.jwellman.virtualdesktop.tools.InMemoryToolCatalog;
import org.jwellman.virtualdesktop.tools.ToolCatalog;
import org.jwellman.virtualdesktop.tools.ToolDefinition;
import org.jwellman.virtualdesktop.tools.ToolEnvironment;
import org.jwellman.virtualdesktop.tools.ToolLaunchKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads tool configuration into a {@link ToolCatalog} and builds Swing
 * {@link DesktopAction} adapters for menus and desktop shortcuts.
 *
 * <p>The catalog is the product roster of what can be launched. Actions are
 * thin adapters that call {@code ToolService.open(definitionId)}.</p>
 *
 * @author rwellman
 */
public class ActionFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ActionFactory.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final List<DesktopAction> listOfActions = new ArrayList<DesktopAction>();

    private static VappsConfig vappsConfig = null;

    private static InMemoryToolCatalog catalog = null;

    /**
     * Get the loaded vapps configuration
     * @return the vapps configuration, or null if not loaded
     */
    public static VappsConfig getVappsConfig() {
        return vappsConfig;
    }

    /**
     * @return the populated tool catalog
     */
    public static ToolCatalog getCatalog() {
        if (catalog == null) {
            throw new IllegalStateException("Catalog not initialized — call initDesktop() first");
        }
        return catalog;
    }

    public static void initDesktop() {
        catalog = new InMemoryToolCatalog();
        listOfActions.clear();

        loadVappsConfig();
        loadExternalApps();

        ToolEnvironment.init(catalog, DesktopManager.get());
        LOG.info("Tool catalog ready: {} definition(s), {} action adapter(s)",
            catalog.size(), listOfActions.size());
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

            for (MenuGroup menuGroup : vappsConfig.getMenuStructure()) {
                processMenuGroup(menuGroup, vappsConfig.getDefaultIcon());
            }

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

    private static void processMenuGroup(MenuGroup group, String defaultIcon) {
        for (VappConfig vappConfig : group.getVapps()) {
            if (vappConfig.isEnabled()) {
                registerVapp(vappConfig, defaultIcon);
            }
        }

        for (MenuGroup subgroup : group.getGroups()) {
            processMenuGroup(subgroup, defaultIcon);
        }
    }

    private static void registerVapp(VappConfig vappConfig, String defaultIcon) {
        try {
            String title = vappConfig.getTitle();
            if (title == null || title.isEmpty()) {
                Class<?> clazz = Class.forName(vappConfig.getClassName());
                title = clazz.getSimpleName();
            }

            String iconKey = vappConfig.getIcon();
            if (iconKey == null || iconKey.isEmpty()) {
                iconKey = defaultIcon;
            }

            Map<String, String> attrs = vappConfig.getAttrs();
            if (attrs == null) {
                attrs = vappConfig.getArgs();
            }

            ToolDefinition definition = ToolDefinition.builder()
                .id(ToolDefinition.internalId(vappConfig.getClassName(), attrs))
                .title(title)
                .iconKey(iconKey)
                .className(vappConfig.getClassName())
                .attrs(attrs)
                .launchKind(ToolLaunchKind.INTERNAL)
                .build();

            definition = catalog.register(definition);

            // desktopOnly in config means "do not put in Tools menu" — adapter placement only
            DesktopAction action = new DesktopAction(definition, vappConfig.isDesktopOnly());
            applyIcons(action, iconKey, "add196");
            getListOfActions().add(action);

        } catch (ClassNotFoundException ex) {
            LOG.error("Failed to register vapp: {} - class not found", vappConfig.getClassName(), ex);
        }
    }

    private static void registerDesktopShortcut(DesktopShortcut shortcut, String defaultIcon) {
        try {
            String iconKey = shortcut.getIcon();
            if (iconKey == null || iconKey.isEmpty()) {
                iconKey = defaultIcon;
            }

            String id = ToolDefinition.internalId(shortcut.getClassName(), null);
            ToolDefinition definition = catalog.findById(id);
            if (definition == null) {
                definition = ToolDefinition.builder()
                    .id(id)
                    .title(shortcut.getLabel())
                    .iconKey(iconKey)
                    .className(shortcut.getClassName())
                    .launchKind(ToolLaunchKind.INTERNAL)
                    .build();
                definition = catalog.register(definition);
            }

            DesktopAction action = new DesktopAction(definition, true);
            // Shortcut tiles prefer the shortcut label when present
            if (shortcut.getLabel() != null && !shortcut.getLabel().isEmpty()) {
                action.putValue(Action.NAME, shortcut.getLabel());
            }
            applyIcons(action, iconKey, "add196");
            getListOfActions().add(action);

        } catch (Exception ex) {
            LOG.error("Failed to register desktop shortcut: {}", shortcut.getLabel(), ex);
        }
    }

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

    private static void registerExternalApp(ExternalAppConfig appConfig) {
        try {
            String iconKey = appConfig.getIcon();

            ToolDefinition definition = ToolDefinition.builder()
                .id(ToolDefinition.externalId(appConfig.getName()))
                .title(appConfig.getName())
                .iconKey(iconKey)
                .className("org.jwellman.virtualdesktop.vapps.ExternalAppSpec")
                .launchKind(ToolLaunchKind.EXTERNAL)
                .command(appConfig.getCommand())
                .workingDirectory(appConfig.getWorkingDir())
                .waitForCompletion(appConfig.isWaitForCompletion())
                .build();

            definition = catalog.register(definition);

            DesktopAction action = new DesktopAction(definition, appConfig.isDesktopOnly());
            applyIcons(action, iconKey, "winking18");
            getListOfActions().add(action);

        } catch (Exception ex) {
            LOG.error("Failed to register external app: {}", appConfig.getName(), ex);
        }
    }

    private static void applyIcons(DesktopAction action, String iconKey, String fallbackKey) {
        if (iconKey == null || iconKey.isEmpty()) {
            iconKey = fallbackKey;
        }

        Icon smallIcon = loadIcon(iconKey + "-small", fallbackKey + "-small");
        Icon largeIcon = loadIcon(iconKey + "-large", fallbackKey + "-large");

        if (smallIcon != null) {
            action.putValue(Action.SMALL_ICON, smallIcon);
        }
        if (largeIcon != null) {
            action.putValue(Action.LARGE_ICON_KEY, largeIcon);
        }
    }

    private static Icon loadIcon(String key, String fallbackKey) {
        try {
            return DSP.Icons.getIcon(key);
        } catch (Exception ex) {
            LOG.warn("Icon not found: {}, trying fallback {}", key, fallbackKey);
            try {
                return DSP.Icons.getIcon(fallbackKey);
            } catch (Exception ex2) {
                LOG.error("Fallback icon also not found: {}", fallbackKey);
                return null;
            }
        }
    }

    /**
     * @return Swing action adapters (not the catalog)
     */
    public static List<DesktopAction> getListOfActions() {
        return listOfActions;
    }

}
