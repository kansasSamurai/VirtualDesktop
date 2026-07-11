package org.jwellman.diagram.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jwellman.diagram.api.CanvasTheme;

/**
 * Lookup table from theme name to theme instance. Backs the toolbar theme
 * selector and resolves theme names read back from persisted diagram files.
 */
public final class CanvasThemeRegistry {

    private static final Map<String, Supplier<CanvasTheme>> THEMES = new LinkedHashMap<>();

    static {
        THEMES.put("Whiteprint", WhiteprintCanvasTheme::new);
        THEMES.put("Blueprint",  BlueprintCanvasTheme::new);
        THEMES.put("Light",      LightCanvasTheme::new);
    }

    private CanvasThemeRegistry() {
    }

    /** Returns a fresh instance of the named theme, or {@code null} if unknown. */
    public static CanvasTheme byName(String name) {
        Supplier<CanvasTheme> factory = THEMES.get(name);
        return (factory != null) ? factory.get() : null;
    }

    /** Returns all registered theme names in registration order. */
    public static String[] names() {
        return THEMES.keySet().toArray(new String[0]);
    }
}
