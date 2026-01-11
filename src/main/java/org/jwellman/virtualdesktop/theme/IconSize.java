package org.jwellman.virtualdesktop.theme;

/**
 * Semantic names for icon sizes used throughout the VirtualDesktop application.
 * <p>
 * Instead of hard-coding pixel values (16px, 48px), code requests icons by
 * their contextual use (SMALL for lists, LARGE for desktop shortcuts).
 * The actual pixel size is determined by the active theme, allowing users
 * to switch between themes with different icon sizes.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * // Before (bad - hard-coded pixel size):
 * Icon icon = DSP.Icons.getIcon("home156-16");
 *
 * // After (good - semantic size):
 * Icon icon = ThemeManager.getIcon("home156", IconSize.SMALL);
 * </pre>
 * </p>
 * <p>
 * Default pixel mappings (in default theme):
 * <ul>
 *   <li>SMALL: 16px - Lists, trees, small UI elements</li>
 *   <li>MENU: 18px - Menu items</li>
 *   <li>TOOLBAR: 24px - Toolbar buttons</li>
 *   <li>LARGE: 48px - Desktop shortcuts, prominent icons</li>
 *   <li>XLARGE: 64px - Special purposes</li>
 * </ul>
 * </p>
 *
 * @author Rick Wellman
 */
public enum IconSize {
    /**
     * Small icons for lists, trees, and compact UI elements.
     * Default: 16px
     */
    SMALL,

    /**
     * Menu icons for menu items.
     * Default: 18px
     */
    MENU,

    /**
     * Toolbar icons for toolbar buttons.
     * Default: 24px
     */
    TOOLBAR,

    /**
     * Large icons for desktop shortcuts and prominent UI elements.
     * Default: 48px
     */
    LARGE,

    /**
     * Extra-large icons for special purposes.
     * Default: 64px
     */
    XLARGE;

    /**
     * Returns the lowercase name of the enum constant for use in theme JSON keys.
     * Example: IconSize.SMALL.toThemeKey() returns "small"
     *
     * @return lowercase name suitable for theme JSON
     */
    public String toThemeKey() {
        return name().toLowerCase();
    }
}
