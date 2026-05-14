package org.jwellman.swing.grid;

import java.awt.Font;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Parses a Blueprint XML string into a configured JPanel component tree.
 *
 * Supported tags:
 *   &lt;row&gt;
 *   &lt;column weight="N"&gt; or &lt;column preferred-width="N"&gt;
 *   &lt;label id="..." text="" font-style="bold|italic"&gt;
 *   &lt;button id="..." text=""&gt;
 *   &lt;script name="bind|prepare"&gt;...&lt;/script&gt;
 *
 * Column metadata is attached to the panel via client property "blueprint-columns"
 * so that applyBounds() can reposition children without re-parsing.
 *
 * Re-parse per pool instance is intentional — Swing components can only have one
 * parent, and re-parsing a small XML string is cheaper than deep-cloning a JPanel.
 */
public class RowBlueprint {

    /** Describes one column region produced during XML parsing. */
    static class ColumnRegion {
        final boolean  fixedWidth;
        final int      preferredWidth;
        final double   weight;
        final List<JPanel> cellPanels = new ArrayList<>();

        ColumnRegion(int preferredWidth) {
            this.fixedWidth     = true;
            this.preferredWidth = preferredWidth;
            this.weight         = 0;
        }

        ColumnRegion(double weight) {
            this.fixedWidth     = false;
            this.preferredWidth = 0;
            this.weight         = weight;
        }
    }

    private RowBlueprint() {}

    /**
     * Parses {@code xml} and builds a JPanel with null layout containing one
     * sub-panel per column. Each child component has its {@code name} property
     * set from the XML {@code id} attribute.
     *
     * The returned panel is not yet positioned — call
     * {@link #applyBounds(JPanel, int, int)} from {@code bind()} once the
     * actual pixel dimensions are known.
     */
    public static JPanel buildPanel(String xml) throws Exception {
        Document doc = parseXml(xml);
        Element root = doc.getDocumentElement(); // <row>

        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(null);
        rowPanel.setOpaque(false);

        List<ColumnRegion> regions = new ArrayList<>();

        NodeList children = root.getChildNodes();

        // First pass: look for explicit <column> children (structured format)
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            Element el = (Element) node;
            if (!el.getTagName().equals("column")) {
                continue;
            }

            ColumnRegion region;
            if (el.hasAttribute("preferred-width")) {
                region = new ColumnRegion(Integer.parseInt(el.getAttribute("preferred-width")));
            } else {
                double weight = el.hasAttribute("weight") ? Double.parseDouble(el.getAttribute("weight")) : 1.0;
                region = new ColumnRegion(weight);
            }

            JPanel colPanel = new JPanel();
            colPanel.setLayout(null);
            colPanel.setOpaque(false);
            region.cellPanels.add(colPanel);
            rowPanel.add(colPanel);

            NodeList cellChildren = el.getChildNodes();
            for (int j = 0; j < cellChildren.getLength(); j++) {
                Node cNode = cellChildren.item(j);
                if (!(cNode instanceof Element)) {
                    continue;
                }
                Element cEl = (Element) cNode;
                String tag = cEl.getTagName();
                if (tag.equals("label")) {
                    colPanel.add(buildLabel(cEl));
                } else if (tag.equals("button")) {
                    colPanel.add(buildButton(cEl));
                }
            }

            regions.add(region);
        }

        // Flat format: if no <column> elements found, treat each direct label/button
        // child as an equal-weight column — simpler syntax for console use:
        //   <row><label id='ts'/><label id='user'/><button id='act' text='Undo'/></row>
        if (regions.isEmpty()) {
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (!(node instanceof Element)) {
                    continue;
                }
                Element el = (Element) node;
                String tag = el.getTagName();
                if (!tag.equals("label") && !tag.equals("button") && !tag.equals("script")) {
                    continue;
                }
                if (tag.equals("script")) {
                    continue;
                }
                ColumnRegion region = new ColumnRegion(1.0);
                JPanel colPanel = new JPanel();
                colPanel.setLayout(null);
                colPanel.setOpaque(false);
                if (tag.equals("label")) {
                    colPanel.add(buildLabel(el));
                } else {
                    colPanel.add(buildButton(el));
                }
                region.cellPanels.add(colPanel);
                rowPanel.add(colPanel);
                regions.add(region);
            }
        }

        rowPanel.putClientProperty("blueprint-columns", regions);
        return rowPanel;
    }

    /**
     * Positions all column sub-panels and their children within {@code panel}
     * using the {@code columnRegions} client property set by {@link #buildPanel}.
     * Called from {@link ScriptableRecyclable#bind} on every bind cycle.
     */
    @SuppressWarnings("unchecked")
    public static void applyBounds(JPanel panel, int totalWidth, int rowHeight) {
        List<ColumnRegion> regions =
            (List<ColumnRegion>) panel.getClientProperty("blueprint-columns");
        if (regions == null || regions.isEmpty()) {
            return;
        }

        // Compute pixel widths using the same fixed-first, then weighted algorithm
        // that SmartGrid.computeColumnWidths() uses.
        int[] widths = new int[regions.size()];
        int fixedTotal = 0;
        double weightTotal = 0;
        for (int i = 0; i < regions.size(); i++) {
            ColumnRegion r = regions.get(i);
            if (r.fixedWidth) {
                widths[i] = r.preferredWidth;
                fixedTotal += r.preferredWidth;
            } else {
                weightTotal += r.weight;
            }
        }
        int remaining = totalWidth - fixedTotal;
        if (remaining < 0) {
            remaining = 0;
        }
        for (int i = 0; i < regions.size(); i++) {
            ColumnRegion r = regions.get(i);
            if (!r.fixedWidth && weightTotal > 0) {
                widths[i] = (int) Math.round(remaining * (r.weight / weightTotal));
            }
        }

        // Position each column sub-panel
        int x = 0;
        for (int i = 0; i < regions.size(); i++) {
            ColumnRegion region = regions.get(i);
            int w = widths[i];
            if (!region.cellPanels.isEmpty()) {
                JPanel colPanel = region.cellPanels.get(0);
                colPanel.setBounds(x, 0, w, rowHeight);
                // Position child components to fill the column
                int count = colPanel.getComponentCount();
                for (int k = 0; k < count; k++) {
                    colPanel.getComponent(k).setBounds(0, 0, w, rowHeight);
                }
            }
            x += w;
        }

        panel.setBounds(0, 0, totalWidth, rowHeight);
    }

    /**
     * Extracts the text content of the first {@code <script name="scriptName">}
     * element found in {@code xml}. Returns {@code null} if no matching element exists.
     */
    public static String extractScript(String xml, String scriptName) throws Exception {
        Document doc = parseXml(xml);
        NodeList scripts = doc.getElementsByTagName("script");
        for (int i = 0; i < scripts.getLength(); i++) {
            Element el = (Element) scripts.item(i);
            if (scriptName.equals(el.getAttribute("name"))) {
                return el.getTextContent();
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------

    private static JLabel buildLabel(Element el) {
        JLabel label = new JLabel();
        if (el.hasAttribute("id")) {
            label.setName(el.getAttribute("id"));
        }
        if (el.hasAttribute("text")) {
            label.setText(el.getAttribute("text"));
        }
        label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        if (el.hasAttribute("font-style")) {
            String style = el.getAttribute("font-style");
            Font base = label.getFont();
            if ("bold".equals(style)) {
                label.setFont(base.deriveFont(Font.BOLD));
            } else if ("italic".equals(style)) {
                label.setFont(base.deriveFont(Font.ITALIC));
            } else if ("bold-italic".equals(style)) {
                label.setFont(base.deriveFont(Font.BOLD | Font.ITALIC));
            }
        }
        return label;
    }

    private static JButton buildButton(Element el) {
        JButton button = new JButton();
        if (el.hasAttribute("id")) {
            button.setName(el.getAttribute("id"));
        }
        if (el.hasAttribute("text")) {
            button.setText(el.getAttribute("text"));
        }
        return button;
    }

    private static Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }
}
