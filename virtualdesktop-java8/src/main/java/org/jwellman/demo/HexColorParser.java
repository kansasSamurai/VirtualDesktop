package org.jwellman.demo;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for parsing comma-delimited HTML hex color strings into Color arrays.
 */
public class HexColorParser {
    
    /**
     * Parse a comma-delimited string of HTML hex colors into a Color array.
     * 
     * @param colorString Comma-delimited string like "#FF0000,#00FF00,#0000FF"
     * @return Array of Color objects
     * @throws IllegalArgumentException if any color format is invalid
     */
    public static Color[] parseColors(String colorString) {
        if (colorString == null || colorString.trim().isEmpty()) {
            return new Color[0];
        }
        
        // Split by comma and trim whitespace
        String[] hexColors = colorString.split(",");
        Color[] colors = new Color[hexColors.length];
        
        for (int i = 0; i < hexColors.length; i++) {
            colors[i] = parseHexColor(hexColors[i].trim());
        }
        
        return colors;
    }
    
    /**
     * Parse a comma-delimited string of HTML hex colors into a List of Color objects.
     * More flexible than array version - skips invalid colors instead of throwing exception.
     * 
     * @param colorString Comma-delimited string like "#FF0000,#00FF00,#0000FF"
     * @param skipInvalid If true, skip invalid colors; if false, throw exception
     * @return List of Color objects
     */
    public static List<Color> parseColorsToList(String colorString, boolean skipInvalid) {
        List<Color> colors = new ArrayList<>();
        
        if (colorString == null || colorString.trim().isEmpty()) {
            return colors;
        }
        
        String[] hexColors = colorString.split(",");
        
        for (String hexColor : hexColors) {
            try {
                Color color = parseHexColor(hexColor.trim());
                colors.add(color);
            } catch (IllegalArgumentException e) {
                if (!skipInvalid) {
                    throw e;
                }
                // Skip invalid color if skipInvalid is true
                System.err.println("Skipping invalid color: " + hexColor.trim());
            }
        }
        
        return colors;
    }
    
    /**
     * Parse a single HTML hex color string into a Color object.
     * Supports both #RRGGBB and #RGB formats.
     * 
     * @param hexColor HTML hex color string (e.g., "#FF0000" or "#F00")
     * @return Color object
     * @throws IllegalArgumentException if format is invalid
     */
    public static Color parseHexColor(String hexColor) {
        if (hexColor == null) {
            throw new IllegalArgumentException("Color string cannot be null");
        }
        
        String hex = hexColor.trim();
        
        // Remove # if present
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        
        // Validate format
        if (!hex.matches("^[0-9A-Fa-f]{3}$") && !hex.matches("^[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("Invalid hex color format: " + hexColor);
        }
        
        // Handle 3-digit format (#RGB -> #RRGGBB)
        if (hex.length() == 3) {
            String r = hex.substring(0, 1);
            String g = hex.substring(1, 2);
            String b = hex.substring(2, 3);
            hex = r + r + g + g + b + b;
        }
        
        try {
            // Parse RGB components
            int rgb = Integer.parseInt(hex, 16);
            return new Color(rgb);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex color format: " + hexColor, e);
        }
    }
    
    /**
     * Alternative method using Color.decode() - simpler but less flexible
     */
    public static Color[] parseColorsSimple(String colorString) {
        if (colorString == null || colorString.trim().isEmpty()) {
            return new Color[0];
        }
        
        String[] hexColors = colorString.split(",");
        Color[] colors = new Color[hexColors.length];
        
        for (int i = 0; i < hexColors.length; i++) {
            String hex = hexColors[i].trim();
            if (!hex.startsWith("#")) {
                hex = "#" + hex; // Add # if missing
            }
            colors[i] = Color.decode(hex);
        }
        
        return colors;
    }
    
    /**
     * Utility method to convert Color array back to comma-delimited hex string
     */
    public static String colorsToHexString(Color[] colors) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < colors.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format("#%06X", colors[i].getRGB() & 0xFFFFFF));
        }
        return sb.toString();
    }
    
    // Demo and test methods
    public static void main(String[] args) {
        // Test various color string formats
        testColorParsing();
    }
    
    private static void testColorParsing() {
        // Test cases
        String[] testStrings = {
            "#000000,#FF0000,#00FF00,#0000FF",           // Basic colors
            "#000000, #FF0000, #00FF00, #0000FF",       // With spaces
            "#FFF,#F00,#0F0,#00F",                      // 3-digit format
            "#FF5733,#33FF57,#5733FF",                  // Custom colors
            "FF0000,00FF00,0000FF",                     // Without # prefix
            "",                                         // Empty string
            "#INVALID,#FF0000"                          // Mixed valid/invalid
        };
        
        for (String testString : testStrings) {
            System.out.println("Testing: \"" + testString + "\"");
            
            try {
                // Test strict parsing
                Color[] colors = parseColors(testString);
                System.out.println("  Parsed " + colors.length + " colors:");
                for (int i = 0; i < colors.length; i++) {
                    System.out.printf("    Color %d: RGB(%d,%d,%d) = %s%n", 
                        i + 1, colors[i].getRed(), colors[i].getGreen(), colors[i].getBlue(),
                        String.format("#%06X", colors[i].getRGB() & 0xFFFFFF));
                }
                
                // Test round-trip conversion
                String backToHex = colorsToHexString(colors);
                System.out.println("  Back to hex: " + backToHex);
                
            } catch (Exception e) {
                System.out.println("  Error: " + e.getMessage());
                
                // Try lenient parsing
                try {
                    List<Color> colorList = parseColorsToList(testString, true);
                    System.out.println("  Lenient parsing found " + colorList.size() + " valid colors");
                } catch (Exception e2) {
                    System.out.println("  Lenient parsing also failed: " + e2.getMessage());
                }
            }
            
            System.out.println();
        }
        
        // Demonstrate usage in a real scenario
        demonstrateUsage();
    }
    
    private static void demonstrateUsage() {
        System.out.println("=== Real Usage Example ===");
        
        // Simulate reading from config file or user input
        String paletteConfig = "#FF6B6B,#4ECDC4,#45B7D1,#96CEB4,#FFEAA7,#DDA0DD";
        
        try {
            Color[] palette = parseColors(paletteConfig);
            
            System.out.println("Loaded color palette with " + palette.length + " colors:");
            for (int i = 0; i < palette.length; i++) {
                Color c = palette[i];
                System.out.printf("  Palette[%d]: %s (RGB: %d,%d,%d)%n", 
                    i, String.format("#%06X", c.getRGB() & 0xFFFFFF),
                    c.getRed(), c.getGreen(), c.getBlue());
            }
            
            // Use colors in your application
            // e.g., for drawing, UI themes, data visualization, etc.
            
        } catch (IllegalArgumentException e) {
            System.err.println("Failed to load color palette: " + e.getMessage());
        }
    }

}