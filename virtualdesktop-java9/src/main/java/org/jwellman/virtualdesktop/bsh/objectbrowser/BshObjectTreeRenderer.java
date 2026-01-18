package org.jwellman.virtualdesktop.bsh.objectbrowser;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * Custom cell renderer for the BeanShell object browser tree.
 *
 * <p>Displays nodes in the format: name .. fullPath  typeName</p>
 *
 * <p>Uses different styling for XThis objects (BeanShell scripted objects)
 * versus plain Java objects.</p>
 *
 * @author Rick Wellman
 */
public class BshObjectTreeRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = 1L;

    // Colors for different node types
    private Color xthisColor;
    private Color javaObjectColor;
    private Color primitiveColor;
    private Color pathColor;
    private Color typeColor;

    public BshObjectTreeRenderer() {
        initColors();
    }

    /**
     * Initialize colors based on current look and feel.
     */
    private void initColors() {
        // Try to get colors that work with the current LAF
        Color textColor = UIManager.getColor("Tree.textForeground");
        if (textColor == null) {
            textColor = Color.BLACK;
        }

        // XThis objects (BeanShell scripted) - bold blue
        xthisColor = new Color(0, 100, 180);

        // Java objects - dark gray
        javaObjectColor = textColor;

        // Primitives - green
        primitiveColor = new Color(0, 128, 0);

        // Path - lighter gray
        pathColor = Color.GRAY;

        // Type - dark magenta
        typeColor = new Color(128, 0, 128);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        if (value instanceof BshObjectTreeNode) {
            BshObjectTreeNode node = (BshObjectTreeNode) value;
            renderNode(node, selected);
        }

        return this;
    }

    /**
     * Render the node with appropriate formatting.
     */
    private void renderNode(BshObjectTreeNode node, boolean selected) {
        String name = node.getName();
        String fullPath = node.getFullPath();
        String typeName = node.getTypeName();
        boolean isXThis = node.isXThis();

        // Build the display text using HTML for formatting
        StringBuilder html = new StringBuilder("<html>");

        // Name - bold for XThis
        if (isXThis) {
            html.append("<b><font color='").append(colorToHex(xthisColor)).append("'>");
            html.append(escapeHtml(name));
            html.append("</font></b>");
        } else {
            html.append("<font color='").append(colorToHex(javaObjectColor)).append("'>");
            html.append(escapeHtml(name));
            html.append("</font>");
        }

        // Path separator and full path
        html.append(" <font color='").append(colorToHex(pathColor)).append("'>..</font> ");
        html.append("<font color='").append(colorToHex(pathColor)).append("'>");
        html.append(escapeHtml(fullPath));
        html.append("</font>");

        // Type name (space separator)
        html.append("  ");
        Color typeCol = isXThis ? xthisColor : (isPrimitive(typeName) ? primitiveColor : typeColor);
        html.append("<font color='").append(colorToHex(typeCol)).append("'>");
        html.append(escapeHtml(simplifyTypeName(typeName)));
        html.append("</font>");

        html.append("</html>");

        setText(html.toString());

        // Set font
        Font baseFont = getFont();
        if (baseFont != null) {
            setFont(baseFont.deriveFont(Font.PLAIN));
        }

        // Override colors when selected (for visibility)
        if (selected) {
            setForeground(getTextSelectionColor());
        }
    }

    /**
     * Convert a Color to HTML hex format.
     */
    private String colorToHex(Color color) {
        return String.format("#%02x%02x%02x",
            color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Escape HTML special characters.
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }

    /**
     * Simplify a type name for display.
     * E.g., "java.lang.String" becomes "String"
     */
    private String simplifyTypeName(String typeName) {
        if (typeName == null) {
            return "null";
        }
        // For common java.lang types, show simple name
        if (typeName.startsWith("java.lang.")) {
            return typeName.substring("java.lang.".length());
        }
        // For BeanShell types, show simple name
        if (typeName.startsWith("bsh.")) {
            return typeName.substring("bsh.".length());
        }
        return typeName;
    }

    /**
     * Check if a type name represents a primitive wrapper.
     */
    private boolean isPrimitive(String typeName) {
        return typeName != null && (
            typeName.equals("java.lang.Integer") ||
            typeName.equals("java.lang.Long") ||
            typeName.equals("java.lang.Double") ||
            typeName.equals("java.lang.Float") ||
            typeName.equals("java.lang.Boolean") ||
            typeName.equals("java.lang.Byte") ||
            typeName.equals("java.lang.Short") ||
            typeName.equals("java.lang.Character") ||
            typeName.equals("java.lang.String") ||
            typeName.equals("primitive")
        );
    }
}
