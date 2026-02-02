package org.jwellman.demo.smarttree;

import java.util.HashSet;
import java.util.Set;

/**
 * Usage:
 * <pre>
    SmartTreePanel panel = new SmartTreePanel(myData);
    
    // Even though Address has 'street', 'city', and 'zip' fields, 
    // the user will only see the Summary string (e.g., "123 Main St, New York")
    panel.getLeafPolicyRegistry().forceLeaf(Address.class);

    // Clean up standard Swing noise if you are inspecting UI components
    panel.getLeafPolicyRegistry().forceLeaf(java.awt.Color.class);
    panel.getLeafPolicyRegistry().forceLeaf(java.awt.Font.class);
    panel.getLeafPolicyRegistry().forceLeaf(javax.swing.tree.DefaultTreeModel.class);
    </pre>
 *
 * @author rwellman
 *
 */
public class LeafPolicyRegistry {

    private final Set<Class<?>> terminalClasses = new HashSet<>();

    /**
     * Mark a class as a Leaf. The tree will show its summary 
     * but will not allow the user to expand it to see fields.
     */
    public void forceLeaf(Class<?> clazz) {
        terminalClasses.add(clazz);
    }

    public boolean isForceLeaf(Object obj) {
        if (obj == null) return true;
        for (Class<?> clazz : terminalClasses) {
            if (clazz.isInstance(obj)) return true;
        }
        return false;
    }

}
