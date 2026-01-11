package org.jwellman.dsp;

import java.awt.Color;
import java.awt.Image;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.batik.transcoder.TranscoderException;
import org.jwellman.dsp.icons.IconProvider;
import org.jwellman.dsp.icons.IconSpecifier;
import org.jwellman.virtualdesktop.desktop.VIcon;

/**
 * IconProvider implementation that loads icons from classpath resource directories.
 * <p>
 * Supports both SVG and raster image formats (PNG, JPG, GIF) with automatic format detection.
 * SVG is the preferred format and will be tried first, with raster formats as fallback.
 * </p>
 * <p>
 * Resource paths follow the VIcon convention - no file extension in the path.
 * For example: "org/jwellman/virtualdesktop/images/global_ui/home156"
 * </p>
 *
 * @author Rick Wellman
 */
public class DirectoryIconProvider implements IconProvider {

    /**
     * Supported raster image formats in order of preference.
     */
    private static final String[] RASTER_EXTENSIONS = {".png", ".jpg", ".gif", ".bmp"};

    @Override
    public void initialize() {
        // Intentionally empty
        // This icon provider does not need any preparation/initialization
    }

    /**
     * Gets an icon from the classpath resource directory.
     * Tries SVG first, then falls back to raster formats (PNG, JPG, GIF).
     *
     * @param specifier IconSpecifier containing the resource path and size
     * @return Icon at the requested size, or null if not found
     */
    @Override
    public Icon getIcon(IconSpecifier specifier) {
        String basePath = specifier.getIconName();
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

}
