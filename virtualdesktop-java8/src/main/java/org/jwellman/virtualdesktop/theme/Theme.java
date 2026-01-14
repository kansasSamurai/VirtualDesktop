package org.jwellman.virtualdesktop.theme;

import java.awt.Color;
import java.awt.Font;

/**
 * Defines the contract for a VirtualDesktop theme.
 * <p>
 * A theme controls:
 * <ul>
 *   <li>Icon sizes - Maps semantic size names (SMALL, LARGE) to pixel values</li>
 *   <li>Colors - Three-level system (Palette → Semantic Roles → Component Mappings)</li>
 *   <li>Fonts - Font configurations for UI components</li>
 *   <li>Icon metadata - Which icons are colorable (SVG/font) vs fixed (raster)</li>
 * </ul>
 * </p>
 * <p>
 * Themes can be loaded from JSON files and support inheritance.
 * Example: "large-icons" theme extends "default", overriding only icon sizes.
 * </p>
 *
 * @author Rick Wellman
 */
public interface Theme {

    // ==================== Theme Metadata ====================

    /**
     * Gets the unique identifier for this theme (e.g., "default", "dark", "large-icons").
     * Used in preferences.json and for theme lookup.
     *
     * @return theme identifier
     */
    String getName();

    /**
     * Gets the human-readable display name for this theme.
     * Example: "Default Theme", "Dark Theme", "Large Icons"
     *
     * @return display name
     */
    String getDisplayName();

    /**
     * Gets the description of this theme.
     * Example: "Default VirtualDesktop theme with standard sizes and light colors"
     *
     * @return theme description
     */
    String getDescription();

    /**
     * Gets the version of this theme.
     * Example: "1.0.0"
     *
     * @return theme version
     */
    String getVersion();

    /**
     * Gets the name of the parent theme if this theme extends another theme.
     * Returns null if this is a base theme (no inheritance).
     *
     * @return parent theme name or null
     */
    String getExtends();

    // ==================== Icon Sizes ====================

    /**
     * Gets the pixel size for a semantic icon size.
     * <p>
     * Example:
     * <pre>
     * int pixels = theme.getIconSize(IconSize.SMALL);  // Returns 16 in default theme, 24 in large-icons theme
     * </pre>
     * </p>
     *
     * @param size the semantic icon size
     * @return pixel size for the semantic size
     */
    int getIconSize(IconSize size);

    // ==================== Colors ====================

    /**
     * Gets a color from the palette by name.
     * <p>
     * Example: getColorFromPalette("blue500") returns #2196F3 in default theme
     * </p>
     *
     * @param paletteName the palette color name (e.g., "blue500", "gray900")
     * @return the color or null if not found
     */
    Color getColorFromPalette(String paletteName);

    /**
     * Gets a color by semantic path.
     * <p>
     * Semantic paths use dot notation to navigate the color hierarchy.
     * Examples:
     * <ul>
     *   <li>"background.desktop" → Color for desktop background</li>
     *   <li>"icon.menu" → Color for menu icons</li>
     *   <li>"status.error" → Color for error status</li>
     * </ul>
     * </p>
     * <p>
     * The path resolves through the three-level color system:
     * 1. Semantic role in colors section (e.g., "icon.menu" → "gray700")
     * 2. Palette lookup (e.g., "gray700" → "#616161")
     * 3. Color object creation
     * </p>
     *
     * @param semanticPath the dot-notation path (e.g., "background.desktop")
     * @return the resolved color or null if not found
     */
    Color getColor(String semanticPath);

    /**
     * Gets a component-specific color property.
     * <p>
     * Components can override semantic colors for specific needs.
     * Example: getComponentColor("toolbarIcon", "hoverBackground") returns the
     * hover background color specifically for toolbar icons.
     * </p>
     *
     * @param componentName the component name (e.g., "menuIcon", "toolbarIcon")
     * @param property the property name (e.g., "foreground", "background", "hoverBackground")
     * @return the component-specific color or null if not defined
     */
    Color getComponentColor(String componentName, String property);

    // ==================== Fonts ====================

    /**
     * Gets a font configuration by key.
     * <p>
     * Font keys include: "base", "title", "menu"
     * </p>
     * <p>
     * NOTE: In Phase 1, this is defined but not actively used.
     * Font integration is planned for a future phase.
     * </p>
     *
     * @param fontKey the font key (e.g., "base", "title", "menu")
     * @return the font or null if not defined
     */
    Font getFont(String fontKey);

    // ==================== Icon Metadata ====================

    /**
     * Checks if an icon is colorable (SVG or icon font) vs fixed color (raster).
     * <p>
     * Colorable icons can have their colors modified to match the theme.
     * Non-colorable icons use their original embedded colors.
     * </p>
     * <p>
     * NOTE: SVG color replacement is planned for Phase 2/3.
     * This method provides the metadata for future use.
     * </p>
     *
     * @param iconName the icon name (e.g., "home156", "calendar168")
     * @return true if icon supports color customization, false otherwise
     */
    boolean isIconColorable(String iconName);

    /**
     * Gets the icon type (e.g., "raster", "svg", "font").
     *
     * @param iconName the icon name
     * @return the icon type or null if not specified
     */
    String getIconType(String iconName);
}
