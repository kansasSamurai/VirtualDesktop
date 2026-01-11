package org.jwellman.virtualdesktop.theme;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Theme implementation that loads configuration from JSON files.
 * <p>
 * Supports theme inheritance via the "extends" field. When a theme extends another,
 * it inherits all values from the parent and can override specific sections.
 * </p>
 * <p>
 * Example JSON structure:
 * <pre>
 * {
 *   "name": "large-icons",
 *   "displayName": "Large Icons",
 *   "extends": "default",
 *   "iconSizes": {
 *     "small": 24,
 *     "large": 64
 *   }
 * }
 * </pre>
 * </p>
 *
 * @author Rick Wellman
 */
public class JsonTheme implements Theme {

    private static final ObjectMapper mapper = new ObjectMapper();

    // Theme metadata
    private String name;
    private String displayName;
    private String description;
    private String version;
    private String extendsTheme;

    // Parent theme (for inheritance)
    private JsonTheme parent;

    // Theme data
    private Map<String, Integer> iconSizes;
    private Map<String, String> palette;  // color name → hex color
    private JsonNode colorsNode;  // semantic colors (hierarchical)
    private JsonNode componentsNode;  // component-specific colors (hierarchical)
    private JsonNode fontsNode;  // font configurations (hierarchical)
    private Map<String, JsonNode> iconMetadata;  // icon name → metadata

    /**
     * Loads a theme from a JSON file.
     *
     * @param themeFile the JSON file
     * @return the loaded theme
     * @throws IOException if file cannot be read or parsed
     */
    public static JsonTheme loadFromFile(File themeFile) throws IOException {
        JsonNode root = mapper.readTree(themeFile);
        return loadFromJson(root, themeFile.getParentFile());
    }

    /**
     * Loads a theme from a JSON node.
     *
     * @param root the JSON root node
     * @param themesDir the directory containing theme files (for resolving inheritance)
     * @return the loaded theme
     * @throws IOException if parent theme cannot be loaded
     */
    private static JsonTheme loadFromJson(JsonNode root, File themesDir) throws IOException {
        JsonTheme theme = new JsonTheme();

        // Load metadata
        theme.name = root.has("name") ? root.get("name").asText() : null;
        theme.displayName = root.has("displayName") ? root.get("displayName").asText() : theme.name;
        theme.description = root.has("description") ? root.get("description").asText() : "";
        theme.version = root.has("version") ? root.get("version").asText() : "1.0.0";
        theme.extendsTheme = root.has("extends") ? root.get("extends").asText() : null;

        // Load parent theme if specified
        if (theme.extendsTheme != null) {
            File parentFile = new File(themesDir, theme.extendsTheme + ".json");
            if (parentFile.exists()) {
                theme.parent = loadFromFile(parentFile);
            } else {
                System.err.println("Warning: Parent theme not found: " + theme.extendsTheme);
            }
        }

        // Load icon sizes
        theme.iconSizes = new HashMap<>();
        if (root.has("iconSizes")) {
            JsonNode sizesNode = root.get("iconSizes");
            sizesNode.fields().forEachRemaining(entry -> {
                theme.iconSizes.put(entry.getKey(), entry.getValue().asInt());
            });
        }

        // Load palette
        theme.palette = new HashMap<>();
        if (root.has("palette")) {
            JsonNode paletteNode = root.get("palette");
            paletteNode.fields().forEachRemaining(entry -> {
                theme.palette.put(entry.getKey(), entry.getValue().asText());
            });
        }

        // Store hierarchical nodes as-is for efficient traversal
        theme.colorsNode = root.has("colors") ? root.get("colors") : null;
        theme.componentsNode = root.has("components") ? root.get("components") : null;
        theme.fontsNode = root.has("fonts") ? root.get("fonts") : null;

        // Load icon metadata
        theme.iconMetadata = new HashMap<>();
        if (root.has("iconMetadata")) {
            JsonNode metadataNode = root.get("iconMetadata");
            metadataNode.fields().forEachRemaining(entry -> {
                theme.iconMetadata.put(entry.getKey(), entry.getValue());
            });
        }

        return theme;
    }

    // ==================== Theme Metadata ====================

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getExtends() {
        return extendsTheme;
    }

    // ==================== Icon Sizes ====================

    @Override
    public int getIconSize(IconSize size) {
        String key = size.toThemeKey();

        // Check this theme first
        if (iconSizes.containsKey(key)) {
            return iconSizes.get(key);
        }

        // Fallback to parent theme
        if (parent != null) {
            return parent.getIconSize(size);
        }

        // Fallback to defaults
        return getDefaultIconSize(size);
    }

    /**
     * Gets default icon sizes if not specified in theme.
     */
    private int getDefaultIconSize(IconSize size) {
        switch (size) {
            case SMALL: return 16;
            case MENU: return 18;
            case TOOLBAR: return 24;
            case LARGE: return 48;
            case XLARGE: return 64;
            default: return 16;
        }
    }

    // ==================== Colors ====================

    @Override
    public Color getColorFromPalette(String paletteName) {
        // Check this theme first
        if (palette.containsKey(paletteName)) {
            return parseColor(palette.get(paletteName));
        }

        // Fallback to parent theme
        if (parent != null) {
            return parent.getColorFromPalette(paletteName);
        }

        return null;
    }

    @Override
    public Color getColor(String semanticPath) {
        // Navigate the hierarchical colors structure
        String[] parts = semanticPath.split("\\.");
        JsonNode current = colorsNode;

        // Traverse this theme's color hierarchy
        if (current != null) {
            for (String part : parts) {
                if (current.has(part)) {
                    current = current.get(part);
                } else {
                    current = null;
                    break;
                }
            }

            // If we found a value, resolve it
            if (current != null && current.isTextual()) {
                String value = current.asText();
                // Value could be a palette reference or a direct color
                Color paletteColor = getColorFromPalette(value);
                if (paletteColor != null) {
                    return paletteColor;
                }
                // Try parsing as direct color
                return parseColor(value);
            }
        }

        // Fallback to parent theme
        if (parent != null) {
            return parent.getColor(semanticPath);
        }

        return null;
    }

    @Override
    public Color getComponentColor(String componentName, String property) {
        JsonNode current = componentsNode;

        // Traverse this theme's components structure
        if (current != null && current.has(componentName)) {
            JsonNode component = current.get(componentName);
            if (component.has(property)) {
                String value = component.get(property).asText();

                // Value could be:
                // 1. A semantic color path (e.g., "icon.default")
                // 2. A palette reference (e.g., "gray700")
                // 3. A direct color (e.g., "#616161" or "rgba(0,0,0,0)")

                // Try semantic path first
                Color semanticColor = getColor(value);
                if (semanticColor != null) {
                    return semanticColor;
                }

                // Try palette reference
                Color paletteColor = getColorFromPalette(value);
                if (paletteColor != null) {
                    return paletteColor;
                }

                // Try direct color
                return parseColor(value);
            }
        }

        // Fallback to parent theme
        if (parent != null) {
            return parent.getComponentColor(componentName, property);
        }

        return null;
    }

    /**
     * Parses a color string (hex or rgba).
     * Supports: "#RRGGBB", "rgba(r,g,b,a)", "transparent"
     */
    private Color parseColor(String colorString) {
        if (colorString == null) {
            return null;
        }

        colorString = colorString.trim();

        // Handle special case: transparent
        if ("transparent".equalsIgnoreCase(colorString) ||
            colorString.matches("rgba?\\(\\s*0\\s*,\\s*0\\s*,\\s*0\\s*,\\s*0(\\.0)?\\s*\\)")) {
            return new Color(0, 0, 0, 0);
        }

        // Handle hex colors: #RRGGBB
        if (colorString.startsWith("#")) {
            try {
                return Color.decode(colorString);
            } catch (NumberFormatException ex) {
                System.err.println("Failed to parse hex color: " + colorString);
                return null;
            }
        }

        // Handle rgba: rgba(r,g,b,a)
        if (colorString.startsWith("rgba(")) {
            try {
                String values = colorString.substring(5, colorString.length() - 1);
                String[] parts = values.split(",");
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                float a = Float.parseFloat(parts[3].trim());
                return new Color(r, g, b, (int)(a * 255));
            } catch (Exception ex) {
                System.err.println("Failed to parse rgba color: " + colorString);
                return null;
            }
        }

        // Handle rgb: rgb(r,g,b)
        if (colorString.startsWith("rgb(")) {
            try {
                String values = colorString.substring(4, colorString.length() - 1);
                String[] parts = values.split(",");
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                return new Color(r, g, b);
            } catch (Exception ex) {
                System.err.println("Failed to parse rgb color: " + colorString);
                return null;
            }
        }

        System.err.println("Unsupported color format: " + colorString);
        return null;
    }

    // ==================== Fonts ====================

    @Override
    public Font getFont(String fontKey) {
        JsonNode current = fontsNode;

        // Traverse this theme's fonts structure
        if (current != null && current.has(fontKey)) {
            JsonNode fontNode = current.get(fontKey);
            String family = fontNode.has("family") ? fontNode.get("family").asText() : "Dialog";
            String styleStr = fontNode.has("style") ? fontNode.get("style").asText() : "PLAIN";
            int size = fontNode.has("size") ? fontNode.get("size").asInt() : 12;

            int style = Font.PLAIN;
            if ("BOLD".equalsIgnoreCase(styleStr)) {
                style = Font.BOLD;
            } else if ("ITALIC".equalsIgnoreCase(styleStr)) {
                style = Font.ITALIC;
            } else if ("BOLDITALIC".equalsIgnoreCase(styleStr) || "BOLD_ITALIC".equalsIgnoreCase(styleStr)) {
                style = Font.BOLD | Font.ITALIC;
            }

            return new Font(family, style, size);
        }

        // Fallback to parent theme
        if (parent != null) {
            return parent.getFont(fontKey);
        }

        return null;
    }

    // ==================== Icon Metadata ====================

    @Override
    public boolean isIconColorable(String iconName) {
        // Check this theme first
        if (iconMetadata.containsKey(iconName)) {
            JsonNode metadata = iconMetadata.get(iconName);
            if (metadata.has("colorable")) {
                return metadata.get("colorable").asBoolean();
            }
        }

        // Fallback to parent theme
        if (parent != null) {
            return parent.isIconColorable(iconName);
        }

        // Default: assume non-colorable
        return false;
    }

    @Override
    public String getIconType(String iconName) {
        // Check this theme first
        if (iconMetadata.containsKey(iconName)) {
            JsonNode metadata = iconMetadata.get(iconName);
            if (metadata.has("type")) {
                return metadata.get("type").asText();
            }
        }

        // Fallback to parent theme
        if (parent != null) {
            return parent.getIconType(iconName);
        }

        return null;
    }
}
