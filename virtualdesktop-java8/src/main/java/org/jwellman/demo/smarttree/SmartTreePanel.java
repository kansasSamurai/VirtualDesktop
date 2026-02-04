package org.jwellman.demo.smarttree;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * Reusable Panel containing the "Smart Tree".
 * <P>
 * Drop this into any Swing container in your framework.
 */
@SuppressWarnings("serial")
public class SmartTreePanel extends JPanel {

    private final JTree tree;
    private ExpansionPolicy expansionPolicy;
    private SummaryRegistry summaryRegistry = new SummaryRegistry();
    private LeafPolicyRegistry leafRegistry = new LeafPolicyRegistry();
    private FieldFilterRegistry filterRegistry = new FieldFilterRegistry();
    private TransformationRegistry transformerRegistry = new TransformationRegistry();
    private SmartTreeRenderer renderer;
    private Object currentData;

    public SmartTreePanel(Object initialData) {

        this.currentData = initialData;
        this.setLayout(new BorderLayout());

        // 1. Initialize Registries
        // Register the Map transformer so it is available for updateData()
        this.transformerRegistry.addProvider(new MapTransformer());
        this.leafRegistry.forceLeaf(Color.class);

        this.tree = new JTree();
        this.tree.setCellRenderer(renderer = new SmartTreeRenderer(summaryRegistry));
        this.tree.addTreeWillExpandListener(new ExpansionGuardrail());

        ToolTipManager.sharedInstance().registerComponent(this.tree);
        
        // Define expansion logic
        this.expansionPolicy = new ExpansionPolicy() {
            @Override
            public boolean shouldExpand(java.lang.reflect.Field f, Object val) {
                return val != null && (val instanceof Collection || val instanceof Map
                        || !val.getClass().getName().startsWith("java.lang"));
            }
        };

        // --- ADD REFRESH POPUP ---
        JPopupMenu popup = new JPopupMenu();
        JMenuItem refreshItem = new JMenuItem("Refresh");
        refreshItem.addActionListener(e -> updateData(currentData));
        popup.add(refreshItem);

        tree.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup(e);
            }
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup(e);
            }
            private void showPopup(MouseEvent e) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        add(new JScrollPane(tree), BorderLayout.CENTER);
        updateData(initialData);
    }

    /**
     * Resets the tree with a new root object.
     */
    public void updateData(Object newData) {
        this.currentData = newData;

        // Create the fresh "Passport" for cycle detection
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());

        // Generate new nodes
        RefinedNode newRoot = new RefinedNode("Root", newData, this, visited);

        // Swap the model (this triggers the JTree to repaint)
        this.tree.setModel(new DefaultTreeModel(newRoot));
    }

    public ExpansionPolicy getExpansionPolicy() {
        return expansionPolicy;
    }

    public SummaryRegistry getSummaryRegistry() {
        return summaryRegistry;
    }

    public FieldFilterRegistry getFilterRegistry() {
        return filterRegistry;
    }

    public LeafPolicyRegistry getLeafRegistry() {
        return leafRegistry;
    }

    public void setExpansionPolicy(ExpansionPolicy expansionPolicy) {
        this.expansionPolicy = expansionPolicy;
    }

    public void setSummaryRegistry(SummaryRegistry summaryRegistry) {
        this.summaryRegistry = summaryRegistry;
    }

    public void setLeafRegistry(LeafPolicyRegistry leafRegistry) {
        this.leafRegistry = leafRegistry;
    }

    public void setFilterRegistry(FieldFilterRegistry filterRegistry) {
        this.filterRegistry = filterRegistry;
    }

    public TransformationRegistry getTransformerRegistry() {
        return transformerRegistry;
    }

    public void setTransformerRegistry(TransformationRegistry transformerRegistry) {
        this.transformerRegistry = transformerRegistry;
    }

    public JTree getTree() {
        return tree;
    }

    public SmartTreeRenderer getRenderer() {
        return renderer;
    }

    // --- SUPPORTING ARCHITECTURE ---

    public static class PropertyPair {
        private final String name;
        private final Object value;
        public PropertyPair(String name, Object value) { this.name = name; this.value = value; }
        public String getName() { return name; }
        public Object getValue() { return value; }
    }

    interface ExpansionPolicy {
        boolean shouldExpand(Field f, Object val);
    }

    /**
     * 
     * @author rwellman
     *
     */
    static class RefinedNode extends DefaultMutableTreeNode {

        public RefinedNode(String label, Object rawObject, SmartTreePanel context, Set<Object> visited) {

            // --- STEP 1: TRANSFORM ---
            // Map types or 3rd party proxies are converted here
            Object userObject = context.getTransformerRegistry().transform(rawObject);

            // Initialize the node with the (potentially) transformed object
            setUserObject(new PropertyPair(label, userObject));

            // Safety checks (nulls and primitives don't have children)
            if (userObject == null || isPrimitive(userObject)) return;

            // --- STEP 2: GUARDRAILS ---
            if (visited.contains(userObject)) {
                this.add(new DefaultMutableTreeNode(new PropertyPair(label, "[Circular Reference]")));
                return;
            }

            // --- NEW: LEAF POLICY CHECK ---
            // If the registry says this should be a leaf, we stop here.
            if (context.getLeafRegistry().isForceLeaf(userObject)) {
                return; // No children will be added
            }

            // --- STEP 3: RECURSE ---
            // We create a NEW set for this specific branch. 
            // This allows the same object to appear in different branches, 
            // but never as its own ancestor.
            Set<Object> nextVisited = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
            nextVisited.addAll(visited);
            nextVisited.add(userObject);

            // --- RECURSE ---

            // Handle Collections (including transformed Maps)
            if (userObject instanceof Collection) {
                int i = 0;
                for (Object item : (Collection<?>) userObject) {
                    String childLabel = "[" + (i++) + "]";

                    // If this is a transformed map entry, use the 'name' as the label
                    if (item instanceof PropertyPair) {
                        childLabel = ((PropertyPair) item).getName();
                        // We can even pass the 'value' directly so we don't get double PropertyPairs
                        this.add(new RefinedNode(childLabel, ((PropertyPair) item).getValue(), context, nextVisited));
                    } else {
                        this.add(new RefinedNode(childLabel, item, context, nextVisited));
                    }
                }
            } 

            // new : Handle POJOs
            else if (!userObject.getClass().getName().startsWith("java.lang")) {
                for (Field field : userObject.getClass().getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    try {
                        field.setAccessible(true);
                        Object val = field.get(userObject);
                        if (context.getExpansionPolicy().shouldExpand(field, val)) {
                            this.add(new RefinedNode(field.getName(), val, context, nextVisited));
                        } else {
                            this.add(new DefaultMutableTreeNode(new PropertyPair(field.getName(), val)));
                        }
                    } catch (Exception ignored) {}
                }
            }
            /*
             * A Note on getDeclaredFields() vs getFields() I used getDeclaredFields() here
             * because:
             * 
             * getFields() only returns public fields (very rare in well-encapsulated Java
             * POJOs).
             * 
             * getDeclaredFields() returns all fields (private, protected, etc.) defined in
             * that specific class.
             * 
             * If your object model uses Inheritance (e.g., Manager extends Employee),
             * getDeclaredFields() only sees the fields in Manager. If you want to see the
             * whole hierarchy, you would need a small while loop to crawl up the superClass
             * chain—but for a first pass, getDeclaredFields() is usually what developers
             * expect to see.
             */
           
        } // end constructor

        private static boolean isPrimitive(Object obj) {
            return obj.getClass().getName().startsWith("java.lang");
        }

    }

    /**
     * 
     * @author rwellman
     *
     */
    static class SmartTreeRenderer extends javax.swing.tree.DefaultTreeCellRenderer {

        private final SummaryRegistry summaryRegistry;
        private final List<NodeFormatProvider> formatProviders = new ArrayList<>();
        private final Map<Class<?>, IconProvider> iconProviders = new HashMap<>();

        public SmartTreeRenderer(SummaryRegistry summaryRegistry) {
            this.summaryRegistry = summaryRegistry;
        }

        // Registration Methods
        public void addFormatProvider(NodeFormatProvider provider) { formatProviders.add(provider); }
        public void registerIcon(Class<?> clazz, IconProvider provider) { iconProviders.put(clazz, provider); }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, 
                boolean sel, boolean exp, boolean leaf, int row, boolean focus) {

            super.getTreeCellRendererComponent(tree, value, sel, exp, leaf, row, focus);

            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObj = node.getUserObject();

                if (userObj instanceof SmartTreePanel.PropertyPair) {
                    SmartTreePanel.PropertyPair pair = (SmartTreePanel.PropertyPair) userObj;
                    Object actualValue = pair.getValue();

                    // --- 1. HANDLE ICONS ---
                    javax.swing.Icon customIcon = findIcon(actualValue);
                    if (customIcon != null) {
                        setIcon(customIcon);
                    } else {
                        // Fallback to standard Leaf/Folder icons
                        setIcon(leaf ? getLeafIcon() : getClosedIcon());
                    }

                    // --- 2. HANDLE TEXT FORMATTING ---
                    String renderedText = null;
                    for (NodeFormatProvider provider : formatProviders) {
                        renderedText = provider.format(pair, summaryRegistry);
                        if (renderedText != null) break; 
                    }

                    if (focus) {
                        // for now, do not decorate focused nodes
//                        setText(pair.getName() + " : " + summary);
                        setText(renderedText);
                    } else {
                        if (renderedText != null) {
                            setText(renderedText);
                        } else {
                            // --- 3. DEFAULT FALLBACK ---
                            String key = "<b>" + pair.getName() + "</b>";
                            String summary = summaryRegistry.getSummary(actualValue);
                            setText("<html><font color='#4FC3F7'>" + key + "</font> : <font color='#CE93D8'>" + summary + "</font></html>");
                        }
                    }

                }
            }
            return this;
        }

        private Icon findIcon(Object value) {
            if (value == null) return null;
            for (Map.Entry<Class<?>, IconProvider> entry : iconProviders.entrySet()) {
                if (entry.getKey().isInstance(value)) {
                    return entry.getValue().getIcon(value);
                }
            }
            return null;
        }

        public String getPathToNode(DefaultMutableTreeNode node) {
            Object[] path = node.getPath();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < path.length; i++) {
                DefaultMutableTreeNode n = (DefaultMutableTreeNode) path[i];
                Object userObj = n.getUserObject();

                if (userObj instanceof PropertyPair) {
                    String name = ((PropertyPair) userObj).getName();
                    // Clean up name for JS notation (remove [0] brackets or handle spaces)
                    sb.append(name);
                    if (i < path.length - 1)
                        sb.append(".");
                }
            }
            return sb.toString();
        }

    }
    
//    static class PolishedRenderer extends javax.swing.tree.DefaultTreeCellRenderer {
//
//        private final SummaryRegistry registry;
//
//        public PolishedRenderer(SummaryRegistry registry) {
//            this.registry = registry;
//        }
//
//        @Override
//        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean exp, boolean leaf, int row, boolean focus) {
//            super.getTreeCellRendererComponent(tree, value, sel, exp, leaf, row, focus);
//
//            if (value instanceof javax.swing.tree.DefaultMutableTreeNode) {
//                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
//
//                // --- ADD TOOLTIP LOGIC ---
//                // This provides the JS-style breadcrumb on hover
//                setToolTipText(getPathToNode(node));
//
//                Object userObj = ((javax.swing.tree.DefaultMutableTreeNode) value).getUserObject();
//                if (userObj instanceof SmartTreePanel.PropertyPair) {
//                    SmartTreePanel.PropertyPair pair = (SmartTreePanel.PropertyPair) userObj;
//
//                    String key = "<b>" + pair.getName() + "</b>";
//                    String summary = registry.getSummary(pair.getValue());
//
//                    // Format: Key: Preview
//                    if (focus) {
//                        // for now, do not decorate focused nodes
//                        setText(pair.getName() + " : " + summary);
//                    } else {
//                        setText("<html><font color='#4FC3F7'>" + key + "</font> : <font color='#CE93D8'>" + summary + "</font></html>");
//                    }
//                }
//            }
//            return this;
//        }
//        
//        public String getPathToNode(DefaultMutableTreeNode node) {
//            Object[] path = node.getPath();
//            StringBuilder sb = new StringBuilder();
//            for (int i = 0; i < path.length; i++) {
//                DefaultMutableTreeNode n = (DefaultMutableTreeNode) path[i];
//                Object userObj = n.getUserObject();
//
//                if (userObj instanceof PropertyPair) {
//                    String name = ((PropertyPair) userObj).getName();
//                    // Clean up name for JS notation (remove [0] brackets or handle spaces)
//                    sb.append(name);
//                    if (i < path.length - 1)
//                        sb.append(".");
//                }
//            }
//            return sb.toString();
//        }
//
//    }

    public class ExpansionGuardrail implements javax.swing.event.TreeWillExpandListener {
        private int threshold = 20; // Configurable size
        private boolean enabled = true;

        @Override
        public void treeWillExpand(javax.swing.event.TreeExpansionEvent event) throws javax.swing.tree.ExpandVetoException {
            if (!enabled) return;

            Object node = event.getPath().getLastPathComponent();
            if (node instanceof RefinedNode) {
                RefinedNode rNode = (RefinedNode) node;
                Object data = ((PropertyPair)rNode.getUserObject()).getValue();

                // Check if the data is a collection and exceeds the threshold
                if (data instanceof Collection && ((Collection<?>) data).size() > threshold) {
                    int size = ((Collection<?>) data).size();
                    int result = JOptionPane.showConfirmDialog(null, 
                        "This collection contains " + size + " items. Expanding may cause a delay. Continue?",
                        "Large Collection Warning", JOptionPane.YES_NO_OPTION);

                    if (result != JOptionPane.YES_OPTION) {
                        // Throwing this exception cancels the expansion in Swing
                        throw new javax.swing.tree.ExpandVetoException(event);
                    }
                }
            }
        }

        @Override
        public void treeWillCollapse(javax.swing.event.TreeExpansionEvent event) {
        }

        public int getThreshold() {
            return threshold;
        }

        public void setThreshold(int threshold) {
            this.threshold = threshold;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
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
                // import org.jwellman.demo.smarttree.*;
                // Map testData = new HashMap (); // for beanshell
                Map<String, Object> testData = new HashMap<>();
                testData.put("ID", 101);
                testData.put("green", Color.green);
                testData.put("Status", "Active");
                testData.put("Meta", new String[]{"Internal", "Verified"});
//                test1 = object();
//                test1.stp = new SmartTreePanel(testData);
//                test1.frame = DesktopManager.get().createVApp(test1.stp, "demo1");

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

