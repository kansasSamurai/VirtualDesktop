package org.jwellman.demo.smarttree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.List;

/**
 * Reusable Panel containing the "Smart Tree".
 * Drop this into any Frame or TabbedPane in your framework.
 */
public class SmartTreePanel extends JPanel {

    private final JTree tree;

    public SmartTreePanel(Object dataToDisplay) {
        setLayout(new BorderLayout());

        // 1. Setup Default Policy (Expand Collections and POJOs)
        ExpansionPolicy defaultPolicy = new ExpansionPolicy() {
            @Override
            public boolean shouldExpand(Field f, Object val) {
                return val != null && 
                    (val instanceof Collection || !val.getClass().getName().startsWith("java.lang"));
            }
        };

        // 2. Initialize Tree with Smart Nodes
        RefinedNode root = new RefinedNode("Root", dataToDisplay, defaultPolicy);
        this.tree = new JTree(root);
        
        // 3. Apply the "JS-Console" Visuals
        this.tree.setCellRenderer(new PolishedRenderer());
        this.tree.setRowHeight(24);

        add(new JScrollPane(tree), BorderLayout.CENTER);
    }

    public JTree getTree() {
        return tree;
    }

    // --- SUPPORTING ARCHITECTURE ---

    static class PropertyPair {
        private final String name;
        private final Object value;
        public PropertyPair(String name, Object value) { this.name = name; this.value = value; }
        public String getName() { return name; }
        public Object getValue() { return value; }
    }

    interface ExpansionPolicy {
        boolean shouldExpand(Field f, Object val);
    }

    static class RefinedNode extends DefaultMutableTreeNode {
        public RefinedNode(String label, Object userObject, ExpansionPolicy policy) {
            super(new PropertyPair(label, userObject));
            if (userObject == null) return;

            if (userObject instanceof Collection) {
                int i = 0;
                for (Object item : (Collection<?>) userObject) {
                    this.add(new RefinedNode("[" + (i++) + "]", item, policy));
                }
            } else if (!userObject.getClass().getName().startsWith("java.lang")) {
                for (Field field : userObject.getClass().getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    try {
                        field.setAccessible(true);
                        Object val = field.get(userObject);
                        if (policy.shouldExpand(field, val)) {
                            this.add(new RefinedNode(field.getName(), val, policy));
                        } else {
                            this.add(new DefaultMutableTreeNode(new PropertyPair(field.getName(), val)));
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    static class PolishedRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean exp, boolean leaf, int row, boolean focus) {
            super.getTreeCellRendererComponent(tree, value, sel, exp, leaf, row, focus);
            if (value instanceof DefaultMutableTreeNode) {
                Object userObj = ((DefaultMutableTreeNode) value).getUserObject();
                if (userObj instanceof PropertyPair) {
                    PropertyPair pair = (PropertyPair) userObj;
                    setText("<html><font color='#9876aa'><b>" + pair.getName() + "</b></font>: " + formatValue(pair.getValue()) + "</html>");
                }
            }
            setIcon(leaf ? null : getClosedIcon()); 
            return this;
        }

        private String formatValue(Object val) {
            if (val == null) return "<font color='#ff6b68'>null</font>";
            if (val instanceof String) return "<font color='#6a8759'>\"" + val + "\"</font>";
            if (val instanceof Number) return "<font color='#6897bb'>" + val + "</font>";
            if (val instanceof Boolean) return "<font color='#cc7832'>" + val + "</font>";
            if (val instanceof Collection) return "<i>Array(" + ((Collection<?>) val).size() + ")</i>";
            return "<i>" + val.getClass().getSimpleName() + "</i>";
        }
    }

    // --- DEMO LAUNCHER (For testing outside your framework) ---
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Framework Component Test");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                
                // Your arbitrary object
                Map<String, Object> testData = new HashMap<>();
                testData.put("ID", 101);
                testData.put("Status", "Active");
                testData.put("Meta", new String[]{"Internal", "Verified"});

                // Usage in your framework:
                JPanel mySmartPanel = new SmartTreePanel(testData);
                
                frame.add(mySmartPanel);
                frame.setSize(400, 500);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}

