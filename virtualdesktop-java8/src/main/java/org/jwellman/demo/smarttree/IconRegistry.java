package org.jwellman.demo.smarttree;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;

/**
 * 
 * @author rwellman
 *
 */
public class IconRegistry {

    private final Map<Class<?>, Icon> iconMap = new HashMap<>();

    public void register(Class<?> clazz, Icon icon) {
        iconMap.put(clazz, icon);
    }

    public Icon getIcon(Object obj) {
        if (obj == null) return null;
        // Search for class or superclass/interface matches
        for (Map.Entry<Class<?>, Icon> entry : iconMap.entrySet()) {
            if (entry.getKey().isInstance(obj)) return entry.getValue();
        }
        return null;
    }

}
