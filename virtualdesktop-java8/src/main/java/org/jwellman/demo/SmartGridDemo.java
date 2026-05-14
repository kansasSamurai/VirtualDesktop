package org.jwellman.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JToggleButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.util.List;
import org.jwellman.swing.grid.ColumnDef;
import org.jwellman.swing.grid.DefaultGridComponentFactory;
import org.jwellman.swing.grid.DefaultGridModel;
import org.jwellman.swing.grid.GridModelListener;
import org.jwellman.swing.grid.GridRow;
import org.jwellman.swing.grid.Recyclable;
import org.jwellman.swing.grid.Selectable;
import org.jwellman.swing.grid.SmartGrid;

/**
 * Demo for SmartGrid — one tab per major feature phase:
 *
 *  "Table"    — 1,000 flat rows; viewport virtualization; proportional column widths
 *  "Tree"     — departments + employees; expand/collapse; unified model
 *  "List"     — single-column SmartGrid; demonstrates list-is-a-table
 *  "Paged"    — 1,000 rows / 50 per page; footer aggregates; pagination bar
 *  "Scripted" — Phase 9b: XML blueprint + BeanShell bind scripts; every 30th row rendered by ScriptableRecyclable
 */
public class SmartGridDemo {

    private static final String[] DEPTS = {
        "Engineering", "Marketing", "Sales", "Human Resources", "Finance"
    };
    private static final String[] STATUSES = {
        "Active", "Active", "Active", "Active", "Inactive"
    };
    private static final String[] LANGUAGES = {
        "Java", "Python", "JavaScript", "C++", "C#", "Rust", "Go", "Kotlin",
        "Swift", "TypeScript", "Ruby", "PHP", "Scala", "Haskell", "Clojure",
        "Elixir", "Erlang", "F#", "OCaml", "Lua", "R", "MATLAB", "Julia",
        "Dart", "Groovy", "Perl", "Cobol", "Fortran", "Pascal", "Ada",
        "Prolog", "Lisp", "Scheme", "Assembly", "VHDL", "Verilog"
    };

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("SmartGrid MVP Demo");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(820, 570);
            frame.add(createDemoTabs());
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    /**
     * Builds and returns the full four-tab demo pane using the light theme.
     * Public so that {@code SpecSmartGrid} (and any other host) can embed it
     * without duplicating the setup logic.
     */
    public static JTabbedPane createDemoTabs() {
        return createDemoTabs(false);
    }

    /**
     * Builds and returns the full four-tab demo pane.
     *
     * @param darkTheme {@code true} to apply SmartGrid's dark colour palette
     */
    public static JTabbedPane createDemoTabs(boolean darkTheme) {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Table",    buildTableTab(darkTheme));
        tabs.addTab("Tree",     buildTreeTab(darkTheme));
        tabs.addTab("List",     buildListTab(darkTheme));
        tabs.addTab("Paged",    buildPagedTab(darkTheme));
        tabs.addTab("Scripted", buildScriptedTab(darkTheme));
        return tabs;
    }

    // -------------------------------------------------------------------------
    // Tab 1: flat table — 1,000 rows, proportional column widths
    // -------------------------------------------------------------------------

    private static JPanel buildTableTab(boolean darkTheme) {
        final DefaultGridModel model = new DefaultGridModel()
            .addColumn(new ColumnDef("id",     "ID",          50, true,  true, null))
            .addColumn(new ColumnDef("name",   "Name",       220, true,  true, null))
            .addColumn(new ColumnDef("dept",   "Department", 180, true,  true, null))
            .addColumn(new ColumnDef("salary", "Salary",     110, true,  true, "currency"))
            .addColumn(new ColumnDef("status", "Status",      80, false, true, null));

        for (int i = 1; i <= 1000; i++) {
            GridRow row = new GridRow()
                .put("id",     String.valueOf(i))
                .put("name",   "Employee " + i)
                .put("dept",   DEPTS[i % DEPTS.length])
                .put("salary", 50_000 + (i * 173 % 100_000)) // raw int — formatted by CellRenderer
                .put("status", STATUSES[i % STATUSES.length]);
            if (i % 7 == 0) {
                row.setTag("fnd-style", "warning-glow");
            }
            if (i % 50 == 0) {
                row.setTag("fnd-type", "featured");
            }
            model.addRow(row);
        }

        SmartGrid grid = new SmartGrid(model, darkTheme);
        grid.registerFormatter("currency", v -> String.format("$%,d", ((Number) v).longValue()));
        final List<ColumnDef>    featuredCols   = model.getColumns();
        final int[]              featuredWidths = grid.getColumnWidths();
        final ListSelectionModel featuredSm     = grid.getSelectionModel();
        grid.registerRowRenderer("featured",
            () -> new FeaturedRowPanel(featuredCols, featuredWidths, featuredSm));
        grid.setColumnFiltersVisible(true);

        // Global filter field — wired to grid; lives in the grid's built-in toolbar
        JTextField filterField = new JTextField(20);
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            private void applyFilter() {
                String text = filterField.getText().trim().toLowerCase();
                if (text.isEmpty()) {
                    grid.clearFilter();
                } else {
                    grid.setFilter(row -> {
                        for (ColumnDef col : model.getColumns()) {
                            Object val = row.get(col.getKey());
                            if (val != null && val.toString().toLowerCase().contains(text)) {
                                return true;
                            }
                        }
                        return false;
                    });
                }
            }
            @Override public void insertUpdate(DocumentEvent e)  { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e)  { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        JToggleButton editToggle = new JToggleButton("Edit Mode");
        editToggle.addActionListener(e -> grid.setEditable(editToggle.isSelected()));

        JLabel filterLabel = new JLabel("Filter:");
        filterLabel.setForeground(java.awt.Color.WHITE);

        JPanel toolbar = grid.getToolbar();
        toolbar.add(filterLabel);
        toolbar.add(filterField);
        toolbar.add(editToggle);

        return grid;
    }

    // -------------------------------------------------------------------------
    // Tab 2: tree / hierarchy
    // -------------------------------------------------------------------------

    private static JPanel buildTreeTab(boolean darkTheme) {
        DefaultGridModel model = new DefaultGridModel()
            .addColumn(new ColumnDef("name",   "Name / Department", 240, false, true, null))
            .addColumn(new ColumnDef("role",   "Role",              180, false, true, null))
            .addColumn(new ColumnDef("salary", "Salary",            110, false, true, null));

        String[][] deptData = {
            {"Engineering",     "13"},
            {"Marketing",        "7"},
            {"Sales",           "10"},
            {"Human Resources",  "5"},
            {"Finance",          "8"}
        };
        String[] roles = {"Engineer", "Sr. Engineer", "Manager", "Director", "Analyst", "Tech Lead"};

        int empId = 1;
        for (String[] dept : deptData) {
            int count = Integer.parseInt(dept[1]);
            model.addRow(new GridRow()
                .put("name",   dept[0])
                .put("role",   count + " employees")
                .put("salary", "")
                .setDepth(0).setHasChildren(true).setExpanded(false)
                .setGroupHeader(true));

            for (int e = 0; e < count; e++) {
                model.addRow(new GridRow()
                    .put("name",   "Employee " + empId)
                    .put("role",   roles[empId % roles.length])
                    .put("salary", String.format("$%,d", 55_000 + (empId * 211 % 90_000)))
                    .setDepth(1).setHasChildren(false));
                empId++;
            }
        }

        SmartGrid grid = new SmartGrid(model, darkTheme);
        grid.setTreeZoneVisible(true);
        return wrap(grid, "Click the ▶ toggle to expand / collapse department employees");
    }

    // -------------------------------------------------------------------------
    // Tab 3: single-column list
    // -------------------------------------------------------------------------

    private static JPanel buildListTab(boolean darkTheme) {
        DefaultGridModel model = new DefaultGridModel()
            .addColumn(new ColumnDef("name", "Programming Languages", 400, false, true, null));

        for (String lang : LANGUAGES) {
            model.addRow(new GridRow().put("name", lang));
        }
        model.addRow(new GridRow()
            .put("name", "BeanShell  (used in this application)")
            .setTag("fnd-style", "warning-glow"));
        model.addRow(new GridRow()
            .put("name", "Groovy  (used in this application)")
            .setTag("fnd-style", "warning-glow"));

        SmartGrid grid = new SmartGrid(model, darkTheme);
        return wrap(grid, "Single-column SmartGrid — a list is just a 1-column table sharing the unified model");
    }

    // -------------------------------------------------------------------------
    // Tab 4: explicit pagination with column-aligned footer aggregates
    // -------------------------------------------------------------------------

    private static JPanel buildPagedTab(boolean darkTheme) {
        DefaultGridModel model = new DefaultGridModel()
            .addColumn(new ColumnDef("id",     "ID",          50, true,  true, null))
            .addColumn(new ColumnDef("name",   "Name",       220, true,  true, null))
            .addColumn(new ColumnDef("dept",   "Department", 180, true,  true, null))
            .addColumn(new ColumnDef("salary", "Salary",     110, true,  true, "currency"))
            .addColumn(new ColumnDef("status", "Status",      80, false, true, null));

        for (int i = 1; i <= 1000; i++) {
            model.addRow(new GridRow()
                .put("id",     String.valueOf(i))
                .put("name",   "Employee " + i)
                .put("dept",   DEPTS[i % DEPTS.length])
                .put("salary", 50_000 + (i * 173 % 100_000)) // raw int — formatted by CellRenderer
                .put("status", STATUSES[i % STATUSES.length]));
        }

        SmartGrid grid = new SmartGrid(model, darkTheme);
        grid.registerFormatter("currency", v -> String.format("$%,d", ((Number) v).longValue()));

        // Footer: row count on "name", salary sum (raw Number) on "salary", active count on "status"
        grid.setFooterRenderer((col, pageRows, fullModel) -> {
            JLabel lbl = new JLabel();
            lbl.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));

            switch (col.getKey()) {
                case "name":
                    lbl.setText(pageRows.size() + " rows");
                    break;
                case "salary":
                    long sum = 0;
                    for (GridRow r : pageRows) {
                        Object n = r.get("salary");
                        if (n instanceof Number) sum += ((Number) n).longValue();
                    }
                    lbl.setText(String.format("$%,d", sum));
                    break;
                case "status":
                    int active = 0;
                    for (GridRow r : pageRows) {
                        if ("Active".equals(r.get("status"))) active++;
                    }
                    lbl.setText(active + " active");
                    break;
                default:
                    break;
            }
            return lbl;
        });

        grid.setPageSize(50);

        return wrap(grid, "50 rows per page — footer shows per-page aggregates; navigation bar below");
    }

    // -------------------------------------------------------------------------
    // Tab 5: Phase 9b — ScriptableRecyclable + Blueprint DSL + BeanShell
    // -------------------------------------------------------------------------

    /**
     * Blueprint XML for the "scripted" fnd-type row renderer.
     * Three columns: ID (fixed 50px), main spanning label (weighted), Status (fixed 80px).
     * Bind and prepare scripts are embedded as CDATA in &lt;script&gt; elements.
     */
    private static final String SCRIPTED_BLUEPRINT_XML = ""
        + "<row>"
        + "  <column preferred-width=\"50\">"
        + "    <label id=\"idLabel\" text=\"\"/>"
        + "  </column>"
        + "  <column weight=\"1\">"
        + "    <label id=\"mainLabel\" text=\"\"/>"
        + "  </column>"
        + "  <column preferred-width=\"80\">"
        + "    <label id=\"statusLabel\" text=\"\"/>"
        + "  </column>"
        + "  <script name=\"bind\"><![CDATA["
        + "    import org.jwellman.swing.grid.ComponentFinder;"
        + "    import javax.swing.JLabel;"
        + "    import java.awt.Color;"
        + "    import java.awt.Font;"
        + "    self.setBackground(new Color(0x1A3A5C));"
        + "    JLabel idLbl   = (JLabel) ComponentFinder.find(panel, \"idLabel\");"
        + "    JLabel mainLbl = (JLabel) ComponentFinder.find(panel, \"mainLabel\");"
        + "    JLabel statLbl = (JLabel) ComponentFinder.find(panel, \"statusLabel\");"
        + "    if (idLbl   != null) { idLbl.setForeground(Color.WHITE); idLbl.setText(row.get(\"id\").toString()); }"
        + "    if (mainLbl != null) { mainLbl.setForeground(new Color(0x7EC8E3)); mainLbl.setFont(mainLbl.getFont().deriveFont(Font.BOLD)); mainLbl.setText(\"\\u00BB  \" + row.get(\"name\") + \"  /  \" + row.get(\"dept\")); }"
        + "    if (statLbl != null) { statLbl.setForeground(Color.WHITE); statLbl.setText(row.get(\"status\").toString()); }"
        + "  ]]></script>"
        + "  <script name=\"prepare\"><![CDATA["
        + "    import org.jwellman.swing.grid.ComponentFinder;"
        + "    import javax.swing.JLabel;"
        + "    self.setBackground(java.awt.Color.WHITE);"
        + "    JLabel idLbl   = (JLabel) ComponentFinder.find(panel, \"idLabel\");"
        + "    JLabel mainLbl = (JLabel) ComponentFinder.find(panel, \"mainLabel\");"
        + "    JLabel statLbl = (JLabel) ComponentFinder.find(panel, \"statusLabel\");"
        + "    if (idLbl   != null) { idLbl.setText(\"\"); }"
        + "    if (mainLbl != null) { mainLbl.setText(\"\"); }"
        + "    if (statLbl != null) { statLbl.setText(\"\"); }"
        + "  ]]></script>"
        + "</row>";

    private static JPanel buildScriptedTab(boolean darkTheme) {
        DefaultGridModel model = new DefaultGridModel()
            .addColumn(new ColumnDef("id",     "ID",          50,  true,  true, null))
            .addColumn(new ColumnDef("name",   "Name",       220,  true,  true, null))
            .addColumn(new ColumnDef("dept",   "Department", 180,  true,  true, null))
            .addColumn(new ColumnDef("salary", "Salary",     110,  true,  true, "currency"))
            .addColumn(new ColumnDef("status", "Status",      80,  false, true, null));

        for (int i = 1; i <= 1000; i++) {
            GridRow row = new GridRow()
                .put("id",     String.valueOf(i))
                .put("name",   "Employee " + i)
                .put("dept",   DEPTS[i % DEPTS.length])
                .put("salary", 50_000 + (i * 173 % 100_000))
                .put("status", STATUSES[i % STATUSES.length]);
            if (i % 30 == 0) {
                row.setTag("fnd-type", "scripted");
            }
            model.addRow(row);
        }

        SmartGrid grid = new SmartGrid(model, darkTheme);
        grid.registerFormatter("currency", v -> String.format("$%,d", ((Number) v).longValue()));

        DefaultGridComponentFactory factory = new DefaultGridComponentFactory(grid);
        factory.create("scripted", SCRIPTED_BLUEPRINT_XML);

        return wrap(grid, "Phase 9b: every 30th row uses an XML blueprint + BeanShell bind script (ScriptableRecyclable)");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static JPanel wrap(SmartGrid grid, String description) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(selectionToolbar(grid), BorderLayout.NORTH);
        panel.add(grid, BorderLayout.CENTER);
        JLabel desc = new JLabel(" " + description);
        desc.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        panel.add(desc, BorderLayout.SOUTH);
        return panel;
    }

    private static JPanel selectionToolbar(SmartGrid grid) {
        JLabel status = new JLabel(" 0 rows selected");

        grid.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ListSelectionModel sm = grid.getSelectionModel();
                int count = 0;
                int min = sm.getMinSelectionIndex();
                int max = sm.getMaxSelectionIndex();
                for (int i = min; i <= max && min >= 0; i++) {
                    if (sm.isSelectedIndex(i)) {
                        count++;
                    }
                }
                status.setText(" " + count + " row(s) selected");
            }
        });

        JButton selectAll = new JButton("Select All");
        selectAll.addActionListener(e -> grid.selectAll());

        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> grid.clearSelection());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        toolbar.add(selectAll);
        toolbar.add(clear);
        toolbar.add(status);
        return toolbar;
    }

    /**
     * Selection toolbar variant that also shows a live "Showing N of totalRows rows"
     * count label, updated via a GridModelListener whenever the filter changes.
     */
    private static JPanel selectionToolbarWithCount(SmartGrid grid, int totalRows) {
        JLabel status = new JLabel(" 0 rows selected");
        JLabel count  = new JLabel("  |  Showing " + totalRows + " of " + totalRows + " rows");

        grid.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ListSelectionModel sm = grid.getSelectionModel();
                int selected = 0;
                int min = sm.getMinSelectionIndex();
                int max = sm.getMaxSelectionIndex();
                for (int i = min; i <= max && min >= 0; i++) {
                    if (sm.isSelectedIndex(i)) {
                        selected++;
                    }
                }
                status.setText(" " + selected + " row(s) selected");
            }
        });

        grid.getModel().addGridModelListener(new GridModelListener() {
            @Override
            public void rowsChanged(int firstRow, int lastRow) {
                updateCount();
            }
            @Override
            public void modelReset() {
                updateCount();
            }
            private void updateCount() {
                count.setText("  |  Showing " + grid.getModel().getRowCount()
                              + " of " + totalRows + " rows");
            }
        });

        JButton selectAll = new JButton("Select All");
        selectAll.addActionListener(e -> grid.selectAll());

        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> grid.clearSelection());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        toolbar.add(selectAll);
        toolbar.add(clear);
        toolbar.add(status);
        toolbar.add(count);
        return toolbar;
    }

    // -------------------------------------------------------------------------
    // Phase 9a demo: custom row renderer registered for fnd-type = "featured"
    // -------------------------------------------------------------------------

    /**
     * Demonstrates Phase 9a GridComponentFactory dispatch. 
     * Every 50th row in the Table tab carries fnd-type="featured" and is rendered 
     * by this panel instead of StandardRowPanel — proving the type-based pool swap works correctly.
     */
    /**
     * Demonstrates mixing normal column cells with a spanning custom label.
     * ID (col 0) and Status (last col) render as plain cells; the middle three
     * columns (Name, Dept, Salary) are spanned by a single star-decorated label.
     *
     * Implements {@link Selectable} so SmartGrid pushes selection state after
     * each bind() rather than this panel needing to poll the model directly.
     */
    @SuppressWarnings("serial")
    static class FeaturedRowPanel extends JPanel implements Recyclable, Selectable {

        private static final Color BG       = new Color(0x1A4A8A);
        private static final Color BG_LIGHT = UIManager.getColor("Table.selectionBackground");
                //new Color(0x2E5FAA); // lighter blue when selected

        private final List<ColumnDef>    columns;
        private final int[]              columnWidths;
        private final ListSelectionModel selectionModel;
        private final JLabel             idLabel     = new JLabel();
        private final JLabel             mainLabel   = new JLabel();
        private final JLabel             statusLabel = new JLabel();
        private MouseAdapter             rowListener;

        FeaturedRowPanel(List<ColumnDef> columns, int[] columnWidths, ListSelectionModel selectionModel) {
            this.columns = columns;
            this.columnWidths = columnWidths;
            this.selectionModel = selectionModel;
            setLayout(null);
            setBackground(BG);
            setOpaque(true);
            for (JLabel lbl : new JLabel[] { idLabel, mainLabel, statusLabel }) {
                lbl.setForeground(Color.WHITE);
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                add(lbl);
            }
        }

        @Override
        public void prepareForReuse() {
            if (rowListener != null) {
                removeMouseListener(rowListener);
                rowListener = null;
            }
            idLabel.setText("");
            mainLabel.setText("");
            statusLabel.setText("");
            setBackground(BG);
        }

        @Override
        public void bind(GridRow row, int rowIndex) {
            if (rowListener != null) {
                removeMouseListener(rowListener);
                rowListener = null;
            }

            setBackground(BG); // setSelected() overrides this if selected

            int h = getHeight();

            // Column 0 — ID: normal cell width, left-padded
            int x0 = 0;
            int w0 = columnWidths[0];
            idLabel.setBounds(x0 + 8, 0, w0 - 8, h);
            idLabel.setText(str(row.get(columns.get(0).getKey())));

            // Columns 1 … n-2 — spanning custom label
            int x1    = w0;
            int spanW = 0;
            for (int i = 1; i < columns.size() - 1; i++) {
                spanW += columnWidths[i];
            }
            mainLabel.setBounds(x1 + 8, 0, spanW - 8, h);
            mainLabel.setText(
                    "★  " + row.get("name") 
                  + "  —  " + row.get("dept") 
                  + "  —  " + String.format("$%,d", row.get("salary"))
                  + " ★")
            ;

            // Last column — Status: normal cell width, left-padded
            int xLast = x1 + spanW;
            int wLast = columnWidths[columns.size() - 1];
            statusLabel.setBounds(xLast + 8, 0, wLast - 8, h);
            statusLabel.setText(str(row.get(columns.get(columns.size() - 1).getKey())));

            final int capturedIdx = rowIndex;
            final ListSelectionModel sm = selectionModel;
            rowListener = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (sm == null) {
                        return;
                    }
                    if (e.isControlDown() || e.isMetaDown()) {
                        if (sm.isSelectedIndex(capturedIdx)) {
                            sm.removeSelectionInterval(capturedIdx, capturedIdx);
                        } else {
                            sm.addSelectionInterval(capturedIdx, capturedIdx);
                        }
                    } else if (e.isShiftDown()) {
                        int anchor = sm.getAnchorSelectionIndex();
                        if (anchor < 0) {
                            anchor = capturedIdx;
                        }
                        sm.setSelectionInterval(anchor, capturedIdx);
                    } else {
                        boolean soleSelection = sm.isSelectedIndex(capturedIdx)
                                && sm.getMinSelectionIndex() == capturedIdx
                                && sm.getMaxSelectionIndex() == capturedIdx;
                        if (soleSelection) {
                            sm.clearSelection();
                        } else {
                            sm.setSelectionInterval(capturedIdx, capturedIdx);
                        }
                    }
                }
            };
            addMouseListener(rowListener);
        }

        @Override
        public void setSelected(boolean selected) {
            setBackground(selected ? BG_LIGHT : BG);
        }

        private static String str(Object v) {
            return v != null ? v.toString() : "";
        }

    }
}
