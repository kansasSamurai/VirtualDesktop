package org.jwellman.demo.smarttree;

import java.lang.reflect.Field;
import java.util.*;

/**
 * How to use it in your application:
 * 
 * If you have a DomainUser object but only want to show the name and email (and
 * hide the hashedPassword or internalId):<pre>
    SmartTreePanel panel = new SmartTreePanel(myUser);

    // Explicitly whitelist fields for a specific domain object
    panel.getFilterRegistry().setVisibleFields(DomainUser.class, "name", "email");
    
    // Or register a summary so it looks clean
    panel.getSummaryRegistry().register(DomainUser.class, o -> ((DomainUser)o).getName());
</pre>
 *
 * @author rwellman
 *
 */
public class FieldFilterRegistry {

    // Map of Class -> List of allowed field names
    private final Map<Class<?>, Set<String>> whiteList = new HashMap<>();

    /**
     * Define exactly which fields to show for a specific class.
     */
    public void setVisibleFields(Class<?> clazz, String... fieldNames) {
        whiteList.put(clazz, new HashSet<>(Arrays.asList(fieldNames)));
    }

    public boolean isVisible(Field field, Object parent) {
        if (parent == null) return true;

        // 1. Check for Annotation (Internal control)
        if (field.isAnnotationPresent(TreeHide.class)) {
            return false;
        }

        // 2. Check the Registry (External/Framework control)
        Class<?> clazz = parent.getClass();
        if (whiteList.containsKey(clazz)) {
            return whiteList.get(clazz).contains(field.getName());
        }

        // 3. Default Sanity Filters
        return !field.getName().startsWith("_") && 
               !java.lang.reflect.Modifier.isStatic(field.getModifiers());
    }

}
