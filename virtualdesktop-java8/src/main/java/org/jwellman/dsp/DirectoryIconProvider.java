package org.jwellman.dsp;

import java.awt.Color;
import java.awt.Image;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.batik.transcoder.TranscoderException;
import org.jwellman.dsp.icons.IconProvider;
import org.jwellman.dsp.icons.IconSpecifier;
import org.jwellman.virtualdesktop.desktop.VIcon;
import org.jwellman.virtualdesktop.theme.IconSize;

/**
 * IconProvider implementation that loads icons from a specific classpath resource directory.
 * <p>
 * Supports both SVG and raster image formats (PNG, JPG, GIF, BMP) with automatic format detection.
 * SVG is the preferred format and will be tried first, with raster formats as fallback.
 * </p>
 * <p>
 * This provider is scoped to a specific base directory. Icon names are relative to that directory.
 * For example, if base directory is "org/jwellman/virtualdesktop/images/global_ui/",
 * then icon name "home156" will look for icons at:
 * "org/jwellman/virtualdesktop/images/global_ui/home156.svg" (preferred),
 * "org/jwellman/virtualdesktop/images/global_ui/home156.png" (fallback), etc.
 * </p>
 * <p>
 * Can auto-discover icons in the directory and register them with DSP.Icons.
 * </p>
 *
 * @author Rick Wellman
 */
public class DirectoryIconProvider implements IconProvider {

    /**
     * Supported raster image formats in order of preference.
     */
    private static final String[] RASTER_EXTENSIONS = {".png", ".jpg", ".gif", ".bmp"};

    /**
     * All supported icon file extensions (SVG + raster formats).
     */
    private static final String[] ALL_EXTENSIONS = {".svg", ".png", ".jpg", ".gif", ".bmp"};

    /**
     * Base directory path for icon resources (with trailing slash).
     */
    private final String baseDirectory;

    /**
     * Creates a DirectoryIconProvider for a specific base directory.
     *
     * @param baseDirectory the base classpath directory path (e.g., "org/jwellman/virtualdesktop/images/global_ui/")
     */
    public DirectoryIconProvider(String baseDirectory) {
        // Ensure base directory ends with a slash
        if (baseDirectory.endsWith("/")) {
            this.baseDirectory = baseDirectory;
        } else {
            this.baseDirectory = baseDirectory + "/";
        }
    }

    @Override
    public void initialize() {
        // Intentionally empty
        // This icon provider does not need any preparation/initialization
    }

    /**
     * Gets an icon from the configured base directory.
     * Icon name is relative to the base directory.
     * Tries SVG first, then falls back to raster formats (PNG, JPG, GIF, BMP).
     *
     * @param specifier IconSpecifier containing the icon name (relative to base directory) and size
     * @return Icon at the requested size, or null if not found
     */
    @Override
    public Icon getIcon(IconSpecifier specifier) {
        String iconName = specifier.getIconName();
        String basePath = baseDirectory + iconName;
        int size = specifier.getSize();

        // Try SVG first using VIcon
        try {
            Icon svgIcon = VIcon.createSVGIcon(basePath, size, size);
            if (svgIcon != null) {
                return svgIcon;
            }
        } catch (TranscoderException ex) {
            // SVG failed, will try raster formats
            System.out.println("SVG not found or failed to load: " + basePath + ".svg - trying raster formats");
        } catch (Exception ex) {
            // General exception (e.g., resource not found)
            System.out.println("SVG loading error for: " + basePath + " - " + ex.getMessage());
        }

        // Try raster formats in order
        for (String extension : RASTER_EXTENSIONS) {
            String fullPath = basePath + extension;
            if (resourceExists(fullPath)) {
                try {
                    return loadRasterIcon(fullPath, size);
                } catch (Exception ex) {
                    System.err.println("Failed to load raster icon: " + fullPath + " - " + ex.getMessage());
                }
            }
        }

        // No icon found
        System.err.println("Warning: No icon found for path: " + basePath + " (tried SVG and raster formats)");
        return null;
    }

    /**
     * Gets an icon with a color override.
     * <p>
     * Note: Color customization for SVG icons is complex and requires modifying SVG content.
     * For raster images, the color parameter doesn't apply directly.
     * This implementation delegates to the primary getIcon method.
     * </p>
     *
     * @param specifier IconSpecifier containing the resource path and size
     * @param color Color override (currently not applied)
     * @return Icon at the requested size, or null if not found
     */
    @Override
    public Icon getIcon(IconSpecifier specifier, Color color) {
        // TODO: Future enhancement could apply color filters to raster images
        // or manipulate SVG content to apply colors
        return getIcon(specifier);
    }

    /**
     * Checks if a classpath resource exists.
     *
     * @param resourcePath the resource path to check
     * @return true if the resource exists, false otherwise
     */
    private boolean resourceExists(String resourcePath) {
        URL url = getClass().getClassLoader().getResource(resourcePath);
        return url != null;
    }

    /**
     * Loads a raster image (PNG, JPG, GIF) from the classpath and scales it to the requested size.
     *
     * @param resourcePath the full resource path including extension
     * @param size the desired width and height in pixels
     * @return ImageIcon scaled to the requested size
     * @throws Exception if the resource cannot be loaded
     */
    private Icon loadRasterIcon(String resourcePath, int size) throws Exception {
        URL url = getClass().getClassLoader().getResource(resourcePath);
        if (url == null) {
            throw new Exception("Resource not found: " + resourcePath);
        }

        ImageIcon icon = new ImageIcon(url);

        // Scale the icon if it's not already the requested size
        if (icon.getIconWidth() != size || icon.getIconHeight() != size) {
            Image scaledImage = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImage);
        }

        return icon;
    }

    /**
     * Discovers all icons in the base directory (recursively scanning subdirectories)
     * and registers them with DSP.Icons at multiple sizes using size-suffixed keys.
     * <p>
     * This method scans for .svg, .png, .jpg, .gif, and .bmp files.
     * If multiple formats exist for the same filename, only one registration is created
     * (the IconProvider will handle format preference during getIcon()).
     * </p>
     * <p>
     * Each icon is registered with BOTH semantic keys (e.g., "home156-small") and
     * pixel-based keys (e.g., "home156-16") for backward compatibility during migration.
     * </p>
     *
     * @param providerName the provider name to use in IconSpecifier (typically "Directory")
     * @param sizes array of semantic icon sizes to register each icon at
     */
    public void discoverAndRegisterIcons(String providerName, IconSize... sizes) {
        try {
            Set<String> discoveredIconNames = new HashSet<>();
            discoverIconsRecursive(baseDirectory, "", discoveredIconNames);

            System.out.println("DirectoryIconProvider: Discovered " + discoveredIconNames.size() + " unique icons in " + baseDirectory);

            // Register each discovered icon at each size with DSP.Icons
            int registrationCount = 0;
            for (String iconName : discoveredIconNames) {
                for (IconSize semanticSize : sizes) {
                    // Get pixel size from ThemeManager
                    int pixels = org.jwellman.virtualdesktop.theme.ThemeManager.getIconSizePixels(semanticSize);

                    // Register with semantic key: "home156-small"
                    String semanticKey = iconName + "-" + semanticSize.toThemeKey();
                    IconSpecifier semanticSpec = new IconSpecifier(providerName, iconName, pixels, null, null, null);
                    DSP.Icons.register(semanticKey, semanticSpec);
                    registrationCount++;

                    // Also register with pixel key for backward compatibility: "home156-16"
                    String pixelKey = iconName + "-" + pixels;
                    IconSpecifier pixelSpec = new IconSpecifier(providerName, iconName, pixels, null, null, null);
                    DSP.Icons.register(pixelKey, pixelSpec);
                    registrationCount++;
                }
            }
            System.out.println("DirectoryIconProvider: Registered " + registrationCount + " icon/size combinations (semantic + pixel keys)");

        } catch (Exception ex) {
            System.err.println("Error during icon discovery: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Legacy method for backward compatibility during migration.
     * Registers icons with pixel-based sizes only.
     *
     * @param providerName the provider name
     * @param sizes array of pixel sizes
     * @deprecated Use {@link #discoverAndRegisterIcons(String, IconSize...)} instead
     */
    @Deprecated
    public void discoverAndRegisterIcons(String providerName, int... sizes) {
        try {
            Set<String> discoveredIconNames = new HashSet<>();
            discoverIconsRecursive(baseDirectory, "", discoveredIconNames);

            System.out.println("DirectoryIconProvider: Discovered " + discoveredIconNames.size() + " unique icons in " + baseDirectory);

            // Register each discovered icon at each size with DSP.Icons
            int registrationCount = 0;
            for (String iconName : discoveredIconNames) {
                for (int size : sizes) {
                    // Create key with size suffix: "home156-16", "home156-48", etc.
                    String key = iconName + "-" + size;
                    IconSpecifier spec = new IconSpecifier(providerName, iconName, size, null, null, null);
                    DSP.Icons.register(key, spec);
                    registrationCount++;
                }
            }
            System.out.println("DirectoryIconProvider: Registered " + registrationCount + " icon/size combinations");

        } catch (Exception ex) {
            System.err.println("Error during icon discovery: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Recursively discovers icon files in a directory.
     *
     * @param fullPath the full classpath path to the current directory
     * @param relativePath the path relative to the base directory (for subdirectories)
     * @param discoveredNames the set to add discovered icon names to
     */
    private void discoverIconsRecursive(String fullPath, String relativePath, Set<String> discoveredNames) {
        try {
            URL dirURL = getClass().getClassLoader().getResource(fullPath);
            if (dirURL == null) {
                System.err.println("Directory not found: " + fullPath);
                return;
            }

            // For file-based resources (typical in development)
            if ("file".equals(dirURL.getProtocol())) {
                File dir = new File(dirURL.toURI());
                if (dir.exists() && dir.isDirectory()) {
                    scanDirectory(dir, relativePath, fullPath, discoveredNames);
                }
            } else {
                // For JAR-based resources, we can't easily list directory contents
                // This is a limitation of classpath resources
                System.out.println("Warning: Cannot auto-discover icons from JAR resources (directory: " + fullPath + ")");
                System.out.println("Auto-discovery only works when running from file system (development mode)");
            }

        } catch (Exception ex) {
            System.err.println("Error scanning directory: " + fullPath + " - " + ex.getMessage());
        }
    }

    /**
     * Scans a file system directory for icon files.
     *
     * @param dir the directory to scan
     * @param relativePath the path relative to the base directory
     * @param fullPath the full classpath path
     * @param discoveredNames the set to add discovered icon names to
     */
    private void scanDirectory(File dir, String relativePath, String fullPath, Set<String> discoveredNames) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // Recursive scan of subdirectory
                String subRelativePath = relativePath.isEmpty() ? file.getName() : relativePath + "/" + file.getName();
                String subFullPath = fullPath + file.getName() + "/";
                discoverIconsRecursive(subFullPath, subRelativePath, discoveredNames);

            } else if (file.isFile()) {
                String filename = file.getName();

                // Check if this is an icon file
                for (String extension : ALL_EXTENSIONS) {
                    if (filename.endsWith(extension)) {
                        // Extract icon name (filename without extension)
                        String iconNameOnly = filename.substring(0, filename.length() - extension.length());

                        // Include relative path if in subdirectory
                        String fullIconName = relativePath.isEmpty() ? iconNameOnly : relativePath + "/" + iconNameOnly;

                        discoveredNames.add(fullIconName);
                        break; // Only add once per filename, regardless of how many formats exist
                    }
                }
            }
        }
    }

}
