package org.jwellman.virtualdesktop.desktop;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jwellman.dsp.DSP;
import org.jwellman.dsp.icons.IconSpecifier;
import org.jwellman.virtualdesktop.theme.ThemeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads desktop icon registrations from config/icon-theme.json and the
 * per-provider registry files in config/providers/.
 *
 * Two-layer design:
 *   icon-theme.json      — which provider handles each semantic key + color role
 *   providers/{p}-registry.json — maps semantic keys to that provider's icon names
 *
 * Providers themselves are still registered in code (FontAwesomeIconProvider, etc.);
 * only the key→name mappings live in config.
 */
public class IconRegistryLoader {

    private static final Logger LOG = LoggerFactory.getLogger(IconRegistryLoader.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static int shortcutWidth = 75;

    public static void load() {
        File configFile = new File("config/icon-theme.json");
        if (!configFile.exists()) {
            LOG.warn("Icon theme config not found: {}", configFile.getAbsolutePath());
            return;
        }

        try {
            IconThemeConfig config = MAPPER.readValue(configFile, IconThemeConfig.class);

            if (config.getShortcut() != null) {
                shortcutWidth = config.getShortcut().getWidth();
            }

            Map<String, Integer> sizes = config.getSizes();
            if (sizes == null || sizes.isEmpty()) {
                LOG.warn("No sizes defined in icon-theme.json; skipping icon registration");
                return;
            }

            Map<String, Map<String, String>> registryCache = new HashMap<>();

            for (IconEntry entry : config.getIcons()) {
                String providerName = entry.getProvider();

                if (!registryCache.containsKey(providerName)) {
                    registryCache.put(providerName, loadProviderRegistry(providerName));
                }

                Map<String, String> registry = registryCache.get(providerName);
                String iconName = registry.get(entry.getKey());

                if (iconName == null) {
                    LOG.warn("No mapping for key '{}' in {} registry", entry.getKey(), providerName);
                    continue;
                }

                Color color = resolveColor(entry.getColor(), config.getColorRoles());

                for (Map.Entry<String, Integer> sizeEntry : sizes.entrySet()) {
                    String dspKey = entry.getKey() + "-" + sizeEntry.getKey();
                    int pixels = sizeEntry.getValue();
                    DSP.Icons.register(dspKey,
                        new IconSpecifier(providerName, iconName, pixels, null, color, color));
                }
            }

        } catch (IOException ex) {
            LOG.error("Failed to load icon theme config", ex);
        }
    }

    public static int getShortcutWidth() {
        return shortcutWidth;
    }

    private static Map<String, String> loadProviderRegistry(String providerName) {
        String filename = providerName.toLowerCase() + "-registry.json";
        File file = new File("config/providers/" + filename);

        if (!file.exists()) {
            LOG.warn("Provider registry not found: {}", file.getAbsolutePath());
            return new HashMap<>();
        }

        try {
            return MAPPER.readValue(file, new TypeReference<Map<String, String>>() {});
        } catch (IOException ex) {
            LOG.error("Failed to load provider registry: {}", filename, ex);
            return new HashMap<>();
        }
    }

    private static Color resolveColor(String role, Map<String, String> colorRoles) {
        if (role == null || colorRoles == null) {
            return Color.WHITE;
        }
        String themeKey = colorRoles.get(role);
        if (themeKey == null) {
            return Color.WHITE;
        }
        int dot = themeKey.lastIndexOf('.');
        if (dot < 0) {
            return Color.WHITE;
        }
        String component = themeKey.substring(0, dot);
        String property  = themeKey.substring(dot + 1);
        Color c = ThemeManager.getComponentColor(component, property);
        return c != null ? c : Color.WHITE;
    }

    // -------------------------------------------------------------------------
    // JSON deserialization POJOs
    // -------------------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IconThemeConfig {

        private Map<String, Integer> sizes;
        private ShortcutConfig shortcut;
        private Map<String, String> colorRoles;
        private List<IconEntry> icons;

        public Map<String, Integer> getSizes() {
            return sizes;
        }

        public void setSizes(Map<String, Integer> sizes) {
            this.sizes = sizes;
        }

        public ShortcutConfig getShortcut() {
            return shortcut;
        }

        public void setShortcut(ShortcutConfig shortcut) {
            this.shortcut = shortcut;
        }

        public Map<String, String> getColorRoles() {
            return colorRoles;
        }

        public void setColorRoles(Map<String, String> colorRoles) {
            this.colorRoles = colorRoles;
        }

        public List<IconEntry> getIcons() {
            return icons;
        }

        public void setIcons(List<IconEntry> icons) {
            this.icons = icons;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShortcutConfig {

        private int width = 75;

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IconEntry {

        private String key;
        private String provider;
        private String color;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }

}
