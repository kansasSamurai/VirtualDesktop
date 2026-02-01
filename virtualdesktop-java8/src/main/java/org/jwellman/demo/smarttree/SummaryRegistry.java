package org.jwellman.demo.smarttree;

import java.awt.*;
import java.util.*;

public class SummaryRegistry {
    // Map of Class -> Function to describe that class
    private final Map<Class<?>, SummaryFunction> registry = new HashMap<>();

    public interface SummaryFunction {
        String summarize(Object obj);
    }

    public SummaryRegistry() {
        // Register some defaults for common Java 8 types
        register(Dimension.class, o -> {
            Dimension d = (Dimension) o;
            return d.width + "x" + d.height;
        });
        register(Color.class, o -> {
            Color c = (Color) o;
            return String.format("RGB(%d,%d,%d)", c.getRed(), c.getGreen(), c.getBlue());
        });
        register(Rectangle.class, o -> {
            Rectangle r = (Rectangle) o;
            return String.format("[%d,%d, w:%d, h:%d]", r.x, r.y, r.width, r.height);
        });
    }

    public void register(Class<?> clazz, SummaryFunction func) {
        registry.put(clazz, func);
    }

    public String getSummary(Object obj) {
        if (obj == null) return "null";
        
        // Check if we have a specific provider for this exact class or interface
        for (Map.Entry<Class<?>, SummaryFunction> entry : registry.entrySet()) {
            if (entry.getKey().isInstance(obj)) {
                return entry.getValue().summarize(obj);
            }
        }

        // Default: If it's a collection, show size. Otherwise, a generic "Object" hint.
        if (obj instanceof Collection) return "Array(" + ((Collection<?>) obj).size() + ")";
        if (obj instanceof Map) return "Map(" + ((Map<?, ?>) obj).size() + ")";
        
        return obj.getClass().getSimpleName();
    }

}