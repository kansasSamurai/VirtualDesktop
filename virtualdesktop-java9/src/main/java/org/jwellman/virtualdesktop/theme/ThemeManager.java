package org.jwellman.virtualdesktop.theme;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

import org.jwellman.dsp.DSP;
import org.jwellman.dsp.icons.IconSpecifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Central service for theme management in VirtualDesktop.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Discover available themes from config/themes/ directory</li>
 *   <li>Load and cache themes</li>
 *   <li>Track the current active theme</li>
 *   <li>Read/write user preferences (config/preferences.json)</li>
 *   <li>Provide convenience methods for accessing theme resources</li>
 * </ul>
 * </p>
 * <p>
 * Phase 1: Theme changes require application restart.
 * Phase 2 (future): Hot-swap theme switching with listener pattern.
 * </p>
 * <p>
 * Usage:
 * <pre>
 * // Initialize on application startup
 * ThemeManager.getInstance().initialize();
 *
 * // Get icon with semantic size
 * Icon icon = ThemeManager.getIcon("home156", IconSize.SMALL);
 *
 * // Get color from theme
 * Color bgColor = ThemeManager.getColor("background.desktop");
 * </pre>
 * </p>
 *
 * @author Rick Wellman
 */
public class ThemeManager {

    private static final String THEMES_DIR = "config/themes";
    private static final String PREFERENCES_FILE = "config/preferences.json";
    private static final String DEFAULT_THEME = "default";

    private static final ObjectMapper mapper = new ObjectMapper();

    // Singleton instance
    private static ThemeManager INSTANCE;

    // Theme cache
    private final Map<String, JsonTheme> themeCache = new HashMap<>();

    // Current active theme
    private JsonTheme currentTheme;

    /**
     * Private constructor for singleton pattern.
     */
    private ThemeManager() {
        // Empty
    }

    /**
     * Gets the singleton instance of ThemeManager.
     *
     * @return the ThemeManager instance
     */
    public static ThemeManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ThemeManager();
        }
        return INSTANCE;
    }

    /**
     * Initializes the theme manager.
     * <p>
     * This should be called once during application startup.
     * It loads the user's preferred theme from preferences.json
     * and falls back to the default theme if not found.
     * </p>
     */
    public void initialize() {
        System.out.println("ThemeManager: Initializing...");

        // Create config directories if they don't exist
        new File(THEMES_DIR).mkdirs();
        new File("config").mkdirs();

        // Load user preferences
        String preferredTheme = loadPreferredTheme();

        // Load the preferred theme
        try {
            currentTheme = loadTheme(preferredTheme);
            System.out.println("ThemeManager: Loaded theme '" + currentTheme.getName() + "'");
        } catch (IOException ex) {
            System.err.println("ThemeManager: Failed to load theme '" + preferredTheme + "', falling back to default");
            ex.printStackTrace();

            // Fallback to default theme
            try {
                currentTheme = loadTheme(DEFAULT_THEME);
                System.out.println("ThemeManager: Loaded fallback theme '" + currentTheme.getName() + "'");
            } catch (IOException ex2) {
                System.err.println("ThemeManager: CRITICAL - Failed to load default theme!");
                ex2.printStackTrace();
                // Continue with null theme - will use defaults
            }
        }
    }

    /**
     * Loads the user's preferred theme name from preferences.json.
     *
     * @return the preferred theme name or "default" if not found
     */
    private String loadPreferredTheme() {
        File prefFile = new File(PREFERENCES_FILE);
        if (!prefFile.exists()) {
            System.out.println("ThemeManager: No preferences file found, using default theme");
            return DEFAULT_THEME;
        }

        try {
            JsonNode root = mapper.readTree(prefFile);
            if (root.has("theme")) {
                String themeName = root.get("theme").asText();
                System.out.println("ThemeManager: User preference: " + themeName);
                return themeName;
            }
        } catch (IOException ex) {
            System.err.println("ThemeManager: Failed to read preferences file: " + ex.getMessage());
        }

        return DEFAULT_THEME;
    }

    /**
     * Saves the preferred theme to preferences.json.
     *
     * @param themeName the theme name to save
     */
    public void savePreferredTheme(String themeName) {
        File prefFile = new File(PREFERENCES_FILE);

        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("theme", themeName);
            root.put("lastModified", Instant.now().toString());
            root.put("version", "1.0.0");

            mapper.writerWithDefaultPrettyPrinter().writeValue(prefFile, root);
            System.out.println("ThemeManager: Saved preference for theme '" + themeName + "'");
        } catch (IOException ex) {
            System.err.println("ThemeManager: Failed to save preferences: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Loads a theme by name from the config/themes/ directory.
     *
     * @param themeName the theme name (without .json extension)
     * @return the loaded theme
     * @throws IOException if theme file cannot be read or parsed
     */
    private JsonTheme loadTheme(String themeName) throws IOException {
        // Check cache first
        if (themeCache.containsKey(themeName)) {
            return themeCache.get(themeName);
        }

        // Load from file
        File themeFile = new File(THEMES_DIR, themeName + ".json");
        if (!themeFile.exists()) {
            throw new IOException("Theme file not found: " + themeFile.getAbsolutePath());
        }

        JsonTheme theme = JsonTheme.loadFromFile(themeFile);
        themeCache.put(themeName, theme);

        return theme;
    }

    /**
     * Discovers all available themes in the config/themes/ directory.
     *
     * @return list of theme names (without .json extension)
     */
    public List<String> getAvailableThemes() {
        List<String> themes = new ArrayList<>();
        File themesDir = new File(THEMES_DIR);

        if (!themesDir.exists() || !themesDir.isDirectory()) {
            return themes;
        }

        File[] files = themesDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                String themeName = file.getName().replace(".json", "");
                themes.add(themeName);
            }
        }

        return themes;
    }

    /**
     * Gets the current active theme.
     *
     * @return the current theme or null if not initialized
     */
    public Theme getCurrentTheme() {
        return currentTheme;
    }

    // ==================== Convenience Methods ====================

    /**
     * Gets an icon with a semantic size from the current theme.
     * <p>
     * This is a convenience wrapper around DSP.Icons that uses theme-specific
     * icon keys. It builds keys like "home156-small" based on the icon name
     * and semantic size.
     * </p>
     * <p>
     * Example:
     * <pre>
     * Icon icon = ThemeManager.getIcon("home156", IconSize.SMALL);
     * </pre>
     * </p>
     *
     * @param iconName the icon name (e.g., "home156")
     * @param size the semantic size
     * @return the icon or null if not found
     */
    public static Icon getIcon(String iconName, IconSize size) {
        if (iconName == null || size == null) {
            return null;
        }

        // Build semantic icon key: "home156-small"
        String key = iconName + "-" + size.toThemeKey();

        // Use DSP.Icons to retrieve the icon
        return DSP.Icons.getIcon(key);
    }

    /**
     * Gets a color by semantic path from the current theme.
     * <p>
     * Example:
     * <pre>
     * Color bgColor = ThemeManager.getColor("background.desktop");
     * </pre>
     * </p>
     *
     * @param semanticPath the semantic color path (e.g., "background.desktop")
     * @return the color or null if not found or theme not initialized
     */
    public static Color getColor(String semanticPath) {
        ThemeManager instance = getInstance();
        if (instance.currentTheme == null) {
            return null;
        }
        return instance.currentTheme.getColor(semanticPath);
    }

    /**
     * Gets a component-specific color from the current theme.
     *
     * @param componentName the component name (e.g., "menuIcon")
     * @param property the property name (e.g., "foreground")
     * @return the color or null if not found or theme not initialized
     */
    public static Color getComponentColor(String componentName, String property) {
        ThemeManager instance = getInstance();
        if (instance.currentTheme == null) {
            return null;
        }
        return instance.currentTheme.getComponentColor(componentName, property);
    }

    /**
     * Gets a font by key from the current theme.
     *
     * @param fontKey the font key (e.g., "base", "title", "menu")
     * @return the font or null if not found or theme not initialized
     */
    public static Font getFont(String fontKey) {
        ThemeManager instance = getInstance();
        if (instance.currentTheme == null) {
            return null;
        }
        return instance.currentTheme.getFont(fontKey);
    }

    /**
     * Gets the pixel size for a semantic icon size from the current theme.
     *
     * @param size the semantic icon size
     * @return the pixel size
     */
    public static int getIconSizePixels(IconSize size) {
        ThemeManager instance = getInstance();
        if (instance.currentTheme == null) {
            // Return default if theme not initialized
            switch (size) {
                case SMALL: return 16;
                case MENU: return 18;
                case TOOLBAR: return 24;
                case LARGE: return 48;
                case XLARGE: return 64;
                default: return 16;
            }
        }
        return instance.currentTheme.getIconSize(size);
    }

    /**
     * Builds an icon key suffix for theme-based icon registration.
     * <p>
     * This is used when registering icons with DSP.Icons to create
     * semantic keys like "home156-small" instead of "home156-16".
     * </p>
     *
     * @param size the semantic icon size
     * @return the key suffix (e.g., "small", "large")
     */
    public static String buildIconKeySuffix(IconSize size) {
        return size.toThemeKey();
    }
}
