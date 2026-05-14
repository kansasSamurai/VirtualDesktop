package org.jwellman.swing.grid;

import java.awt.Component;
import java.awt.Container;

/**
 * Static utility for finding a named component within a container hierarchy.
 *
 * Uses {@link Component#getName()} set during blueprint construction.
 * BeanShell bind scripts call: ComponentFinder.find(panel, "labelId")
 */
public class ComponentFinder {

    private ComponentFinder() {}

    /**
     * Searches {@code root} and all descendant containers for a component
     * whose name equals {@code id}. Returns {@code null} if not found.
     */
    public static Component find(Container root, String id) {
        if (id == null || root == null) {
            return null;
        }
        int count = root.getComponentCount();
        for (int i = 0; i < count; i++) {
            Component c = root.getComponent(i);
            if (id.equals(c.getName())) {
                return c;
            }
            if (c instanceof Container) {
                Component found = find((Container) c, id);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
