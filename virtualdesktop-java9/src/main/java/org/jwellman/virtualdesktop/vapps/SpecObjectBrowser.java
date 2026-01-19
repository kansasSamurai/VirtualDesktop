package org.jwellman.virtualdesktop.vapps;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import bsh.BshMethod;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.NameSpace;
import bsh.XThis;

import org.jwellman.virtualdesktop.bsh.BeanShellService;
import org.jwellman.virtualdesktop.bsh.objectbrowser.BshObjectTreeModel;
import org.jwellman.virtualdesktop.bsh.objectbrowser.BshObjectTreeNode;
import org.jwellman.virtualdesktop.bsh.objectbrowser.BshObjectTreeRenderer;

/**
 * BeanShell Object Browser - visual equivalent to the tree() console command.
 *
 * <p>Provides a JTree-based UI for visualizing the BeanShell namespace object graph.
 * Uses lazy loading to efficiently handle large namespaces.</p>
 *
 * <p>Features a split pane with the object tree on the left and a methods
 * panel on the right that updates when a node is selected.</p>
 *
 * @author Rick Wellman
 */
public class SpecObjectBrowser extends VirtualAppSpec {

    private JTree tree;
    private BshObjectTreeModel treeModel;
    private Interpreter interpreter;
    private JList<String> methodsList;
    private DefaultListModel<String> methodsListModel;
    private JLabel methodsHeader;

    public SpecObjectBrowser() {
        this.height = 500;
        this.width = 800;

        // Get the shared BeanShell interpreter
        interpreter = BeanShellService.get().getInterpreter();

        // Create the tree model and tree
        treeModel = new BshObjectTreeModel(interpreter);
        tree = new JTree(treeModel);

        // Register lazy-loading listener
        tree.addTreeWillExpandListener(treeModel);

        // Set custom renderer
        tree.setCellRenderer(new BshObjectTreeRenderer());

        // Tree configuration
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);

        // Add selection listener for methods panel
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                onNodeSelected(e.getPath());
            }
        });

        // Build the UI
        JPanel panel = new JPanel(new BorderLayout());

        // Toolbar
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });
        toolbar.add(refreshButton);

        JButton expandRootButton = new JButton("Expand Root");
        expandRootButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                expandRoot();
            }
        });
        toolbar.add(expandRootButton);

        JButton expandAllButton = new JButton("Expand All");
        expandAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                expandAll();
            }
        });
        toolbar.add(expandAllButton);

        JButton collapseAllButton = new JButton("Collapse All");
        collapseAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                collapseAll();
            }
        });
        toolbar.add(collapseAllButton);

        panel.add(toolbar, BorderLayout.NORTH);

        // Left side: Tree in scroll pane
        JScrollPane treeScrollPane = new JScrollPane(tree);

        // Right side: Methods panel
        JPanel methodsPanel = createMethodsPanel();

        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            treeScrollPane, methodsPanel);
        splitPane.setDividerLocation(450);
        splitPane.setResizeWeight(0.6);

        panel.add(splitPane, BorderLayout.CENTER);

        this.setTitle("Object Browser");
        this.setContent(this.createDefaultContent(panel));
    }

    /**
     * Create the methods panel for the right side of the split pane.
     */
    private JPanel createMethodsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Header label
        methodsHeader = new JLabel("Methods");
        methodsHeader.setFont(methodsHeader.getFont().deriveFont(Font.BOLD));
        methodsHeader.setBorder(new EmptyBorder(0, 0, 5, 0));
        panel.add(methodsHeader, BorderLayout.NORTH);

        // Methods list
        methodsListModel = new DefaultListModel<String>();
        methodsList = new JList<String>(methodsListModel);
        methodsList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(methodsList);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Handle node selection - update the methods panel.
     */
    private void onNodeSelected(TreePath path) {
        methodsListModel.clear();

        if (path == null) {
            methodsHeader.setText("Methods");
            return;
        }

        Object lastComponent = path.getLastPathComponent();
        if (!(lastComponent instanceof BshObjectTreeNode)) {
            return;
        }

        BshObjectTreeNode node = (BshObjectTreeNode) lastComponent;
        String fullPath = node.getFullPath();
        methodsHeader.setText("Methods: " + node.getName());

        try {
            Object obj = interpreter.get(fullPath);
            if (obj == null) {
                methodsListModel.addElement("(null)");
                return;
            }

            List<String> methods = getMethods(obj);
            for (String method : methods) {
                methodsListModel.addElement(method);
            }

            if (methods.isEmpty()) {
                methodsListModel.addElement("(no methods)");
            }

        } catch (EvalError e) {
            methodsListModel.addElement("Error: " + e.getMessage());
        }
    }

    /**
     * Get methods from an object - handles both XThis and Java objects.
     */
    private List<String> getMethods(Object obj) {
        List<String> result = new ArrayList<String>();

        if (obj instanceof XThis) {
            // BeanShell scripted object - get methods from namespace
            XThis xthis = (XThis) obj;
            NameSpace ns = xthis.getNameSpace();
            BshMethod[] bshMethods = ns.getMethods();
            for (BshMethod m : bshMethods) {
                try {
                    StringBuilder sb = new StringBuilder();
                    sb.append(m.getName()).append("(");
                    Class<?>[] paramTypes = m.getParameterTypes();
                    if (paramTypes != null) {
                        for (int i = 0; i < paramTypes.length; i++) {
                            if (i > 0) sb.append(", ");
                            sb.append(simplifyTypeName(paramTypes[i].getName()));
                        }
                    }
                    sb.append(")");
                    // Add return type if available
                    Class<?> retType = m.getReturnType();
                    if (retType != null && retType != Void.TYPE) {
                        sb.append(" : ").append(simplifyTypeName(retType.getName()));
                    }
                    result.add(sb.toString());
                } catch (Exception e) {
                    // Method type info unavailable - show name with indicator
                    result.add(m.getName() + "(...) [type info unavailable]");
                }
            }
        } else {
            // Java object - use reflection
            Class<?> clazz = obj.getClass();
            Method[] methods = clazz.getMethods();

            // Sort by name
            Arrays.sort(methods, (m1, m2) -> m1.getName().compareTo(m2.getName()));

            for (Method m : methods) {
                // Skip Object methods for cleaner display
                if (m.getDeclaringClass() == Object.class) {
                    continue;
                }

                StringBuilder sb = new StringBuilder();

                // Add modifiers (public, static, etc.)
                int mods = m.getModifiers();
                if (Modifier.isStatic(mods)) {
                    sb.append("static ");
                }

                sb.append(m.getName()).append("(");
                Class<?>[] paramTypes = m.getParameterTypes();
                for (int i = 0; i < paramTypes.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(simplifyTypeName(paramTypes[i].getName()));
                }
                sb.append(")");

                // Return type
                Class<?> retType = m.getReturnType();
                if (retType != Void.TYPE) {
                    sb.append(" : ").append(simplifyTypeName(retType.getName()));
                }

                result.add(sb.toString());
            }
        }

        Collections.sort(result);
        return result;
    }

    /**
     * Simplify a type name for display.
     */
    private String simplifyTypeName(String typeName) {
        if (typeName == null) return "void";

        // Handle arrays
        if (typeName.startsWith("[L") && typeName.endsWith(";")) {
            return simplifyTypeName(typeName.substring(2, typeName.length() - 1)) + "[]";
        }
        if (typeName.startsWith("[")) {
            // Primitive arrays
            switch (typeName.charAt(1)) {
                case 'I': return "int[]";
                case 'J': return "long[]";
                case 'D': return "double[]";
                case 'F': return "float[]";
                case 'Z': return "boolean[]";
                case 'B': return "byte[]";
                case 'C': return "char[]";
                case 'S': return "short[]";
            }
        }

        // Common packages to simplify
        if (typeName.startsWith("java.lang.")) {
            return typeName.substring("java.lang.".length());
        }
        if (typeName.startsWith("java.util.")) {
            return typeName.substring("java.util.".length());
        }
        if (typeName.startsWith("javax.swing.")) {
            return typeName.substring("javax.swing.".length());
        }

        return typeName;
    }

    /**
     * Refresh the tree to reflect current namespace state.
     */
    private void refresh() {
        treeModel.refresh();
        tree.updateUI();
    }

    /**
     * Expand only root-level nodes.
     */
    private void expandRoot() {
        int rowCount = tree.getRowCount();
        for (int i = rowCount - 1; i >= 0; i--) {
            tree.expandRow(i);
        }
    }

    /**
     * Recursively expand all nodes in the tree.
     */
    private void expandAll() {
        int row = 0;
        while (row < tree.getRowCount()) {
            tree.expandRow(row);
            row++;
        }
    }

    /**
     * Collapse all nodes.
     */
    private void collapseAll() {
        int rowCount = tree.getRowCount();
        for (int i = rowCount - 1; i >= 1; i--) {
            tree.collapseRow(i);
        }
    }
}
