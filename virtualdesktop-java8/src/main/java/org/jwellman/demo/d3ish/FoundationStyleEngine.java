package org.jwellman.demo.d3ish;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class FoundationStyleEngine {

    // Level 1: Raw hex codes/colors
    private static final Map<String, Color> primitives = new HashMap<>();
    
    // Level 2: Intent (Maps 'ui.action' to 'brand-main')
    private static final Map<String, String> semanticMap = new HashMap<>();
    
    // Level 3: Component Wiring (Maps 'bar.fill' to 'ui.action')
    private static final Map<String, String> componentMap = new HashMap<>();

    public static void setPrimitive(String key, Color color) {
        primitives.put(key, color);
    }

    public static void mapSemantic(String semanticKey, String primitiveKey) {
        semanticMap.put(semanticKey, primitiveKey);
    }

    public static void mapComponent(String componentProp, String semanticKey) {
        componentMap.put(componentProp, semanticKey);
    }

    /**
     * The Component calls this in paintComponent:
     * g2.setColor(FoundationStyleEngine.getStyle("bar.fill"));
     */
    public static Color getStyle(String componentProp) {
        String semanticKey = componentMap.get(componentProp);
        if (semanticKey == null) return Color.MAGENTA;
        
        String primitiveKey = semanticMap.get(semanticKey);
        if (primitiveKey == null) return Color.MAGENTA;
        
        return primitives.getOrDefault(primitiveKey, Color.MAGENTA);
    }
}
