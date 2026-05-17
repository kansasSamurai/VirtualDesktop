package org.jwellman.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
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
import org.jwellman.swing.grid.LogRowPanel;
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
     * Builds and returns the full demo pane.
     *
     * <ul>
     *   <li>"Table"    — 1,000 flat rows; viewport virtualization; proportional column widths</li>
     *   <li>"Tree"     — departments + employees; expand/collapse; unified model</li>
     *   <li>"List"     — single-column SmartGrid; demonstrates list-is-a-table</li>
     *   <li>"Paged"    — 1,000 rows / 50 per page; footer aggregates; pagination bar</li>
     *   <li>"Scripted" — Phase 9b: XML blueprint + BeanShell bind scripts</li>
     *   <li>"Log"      — dark log viewer; plain text, JSON, stack traces; live search</li>
     * </ul>
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
        tabs.addTab("Log",      buildLogTab());
        return tabs;
    }

    // -------------------------------------------------------------------------
    // Tab 1: flat table — 1,000 rows, proportional column widths
    // -------------------------------------------------------------------------

    private static JPanel buildTableTab(boolean darkTheme) {
        final DefaultGridModel model = new DefaultGridModel()
            .addColumn(new ColumnDef("id",     "ID",          50, true,  true, null))
            .addColumn(new ColumnDef("name",   "Name",       220, true,  true, "name-with-msg"))
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

        // CellRenderer for the name column: demonstrates a real interactive JButton
        // embedded in a cell — something JTable's stamp-based renderer cannot do.
        grid.registerCellRenderer("name-with-msg", (col, value, row, existing) -> {
            JPanel cell;
            JLabel nameLabel;
            JButton msgButton;

            if (existing instanceof JPanel) {
                // Recycle the existing panel — just update its contents
                cell      = (JPanel) existing;
                nameLabel = (JLabel)  cell.getClientProperty("nameLabel");
                msgButton = (JButton) cell.getClientProperty("msgButton");
            } else {
                cell = new JPanel(new BorderLayout(4, 0));
                cell.setOpaque(false);
                nameLabel = new JLabel();
                msgButton = new JButton("Msg");
                msgButton.setFont(msgButton.getFont().deriveFont(10f));
                msgButton.setMargin(new Insets(1, 4, 1, 4));
                msgButton.setFocusable(false);
                cell.add(nameLabel, BorderLayout.CENTER);
                cell.add(msgButton, BorderLayout.EAST);
                cell.putClientProperty("nameLabel", nameLabel);
                cell.putClientProperty("msgButton", msgButton);
            }

            nameLabel.setText(value != null ? value.toString() : "");

            // Remove stale listener and attach a fresh one bound to this row's data
            for (java.awt.event.ActionListener al : msgButton.getActionListeners()) {
                msgButton.removeActionListener(al);
            }
            final String employeeName = value != null ? value.toString() : "Unknown";
            final String employeeId   = row.get("id") != null ? row.get("id").toString() : "";
            msgButton.addActionListener(e -> {
                JPanel dialogPanel = new JPanel(new BorderLayout(0, 6));
                dialogPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
                dialogPanel.add(new JLabel("To: " + employeeName + "  (ID " + employeeId + ")"),
                        BorderLayout.NORTH);
                JTextArea messageArea = new JTextArea(4, 32);
                messageArea.setLineWrap(true);
                messageArea.setWrapStyleWord(true);
                dialogPanel.add(new JScrollPane(messageArea), BorderLayout.CENTER);

                int choice = JOptionPane.showOptionDialog(
                        SwingUtilities.getWindowAncestor(msgButton),
                        dialogPanel,
                        "Send Message",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        new Object[]{"Send", "Cancel"},
                        "Send");

                if (choice == 0) {
                    String text = messageArea.getText().trim();
                    JOptionPane.showMessageDialog(
                            SwingUtilities.getWindowAncestor(msgButton),
                            text.isEmpty()
                                ? "Message sent to " + employeeName + "."
                                : "Message sent to " + employeeName + ":\n“" + text + "”",
                            "Sent",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            });

            return cell;
        });

        final List<ColumnDef>    featuredCols   = model.getColumns();
        final int[]              featuredWidths = grid.getColumnWidths();
        final ListSelectionModel featuredSm     = grid.getSelectionModel();
        grid.registerRowRenderer("featured",
            () -> new FeaturedRowPanel(featuredCols, featuredWidths, featuredSm));
        grid.setColumnFiltersVisible(true);
        grid.setRowNumbersVisible(true);

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

        return buildPage(grid,
            "Employee Directory",
            "SmartGrid demo  ·  1,000 rows  ·  live filter & sort"
                + "  ·  interactive cell renderers  ·  inline edit mode",
            darkTheme,
            buildChip("Virtualized", new Color(0x2980B9)),
            buildChip("Sortable",    new Color(0x27AE60)),
            buildChip("Filterable",  new Color(0xD68910)),
            buildChip("Interactive", new Color(0x8E44AD)),
            buildChip("Edit Mode",   new Color(0xC0392B)));
    }

    private static JLabel buildChip(String text, Color bg) {
        JLabel chip = new JLabel(text);
        chip.setFont(chip.getFont().deriveFont(Font.BOLD, 10f));
        chip.setForeground(Color.WHITE);
        chip.setBackground(bg);
        chip.setOpaque(true);
        chip.setBorder(BorderFactory.createEmptyBorder(3, 7, 3, 7));
        return chip;
    }

    /**
     * Wraps a SmartGrid in a full page layout: a header band with title, subtitle,
     * and feature chips, followed by a bordered card containing the grid.  The
     * SmartGrid's intrinsic summary row (always at the bottom of the grid) provides
     * the "Showing X of Y rows / N selected" status — no external footer needed.
     */
    private static JPanel buildPage(SmartGrid grid, String title, String subtitle,
                                    boolean darkTheme,
                                    JLabel... featureChips) {
        Color pageBg     = darkTheme ? new Color(0x2B2D30) : new Color(0xF0F2F5);
        Color headerBg   = darkTheme ? new Color(0x3C3F41) : Color.WHITE;
        Color titleFg    = darkTheme ? new Color(0xE8E8E8) : new Color(0x1A1A2E);
        Color subtitleFg = darkTheme ? new Color(0x888888) : new Color(0x666677);
        Color borderClr  = darkTheme ? new Color(0x4A4D52) : new Color(0xC8CDD3);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setForeground(titleFg);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 11f));
        subtitleLabel.setForeground(subtitleFg);

        JPanel titleArea = new JPanel();
        titleArea.setLayout(new BoxLayout(titleArea, BoxLayout.Y_AXIS));
        titleArea.setOpaque(false);
        titleArea.add(titleLabel);
        titleArea.add(Box.createVerticalStrut(4));
        titleArea.add(subtitleLabel);

        JPanel chipsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        chipsPanel.setOpaque(false);
        for (JLabel chip : featureChips) {
            chipsPanel.add(chip);
        }

        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(headerBg);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, borderClr),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        header.add(titleArea,   BorderLayout.WEST);
        header.add(chipsPanel,  BorderLayout.EAST);

        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(borderClr));
        card.add(grid, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(card, BorderLayout.CENTER);

        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(pageBg);
        page.add(header,  BorderLayout.NORTH);
        page.add(content, BorderLayout.CENTER);
        return page;
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
        return buildPage(grid,
            "Department Hierarchy",
            "SmartGrid tree mode  ·  5 departments  ·  43 employees"
                + "  ·  expand/collapse  ·  group headers  ·  unified model",
            darkTheme,
            buildChip("Tree View",   new Color(0x2C7BB6)),
            buildChip("Expandable",  new Color(0x1A9850)),
            buildChip("Grouped",     new Color(0x756BB1)),
            buildChip("Hierarchical",new Color(0xE08214)));
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

        // 36 languages + 2 highlighted scripting language entries
        SmartGrid grid = new SmartGrid(model, darkTheme);

        // Search field lives here so it retains its text across rebuildHeaderView()
        // calls (which fire on resize).  The renderer re-parents it each time —
        // Swing moves a component to a new parent automatically.
        final JTextField listSearch = new JTextField(15);
        listSearch.getDocument().addDocumentListener(new DocumentListener() {
            private void applyFilter() {
                String text = listSearch.getText().trim().toLowerCase();
                if (text.isEmpty()) {
                    grid.clearFilter();
                } else {
                    grid.setFilter(row -> {
                        Object val = row.get("name");
                        return val != null && val.toString().toLowerCase().contains(text);
                    });
                }
            }
            @Override public void insertUpdate(DocumentEvent e)  { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e)  { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        grid.setHeaderRenderer((col, sortOrder, rank) -> {
            JPanel cell = new JPanel(new BorderLayout(4, 0));
            cell.setOpaque(false);

            JLabel nameLabel = new JLabel(col.getHeader());
            nameLabel.setForeground(Color.WHITE);
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
            nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

            // Wrap the field to add vertical breathing room within the header cell
            JPanel fieldWrap = new JPanel(new BorderLayout());
            fieldWrap.setOpaque(false);
            fieldWrap.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 6));
            fieldWrap.add(listSearch, BorderLayout.CENTER);

            cell.add(nameLabel, BorderLayout.CENTER);
            cell.add(fieldWrap, BorderLayout.EAST);
            return cell;
        });

        return buildPage(grid,
            "Language Catalog",
            "SmartGrid list mode  ·  38 entries  ·  demonstrates a list is a single-column table"
                + "  ·  same model, filter, and sort as the full table",
            darkTheme,
            buildChip("List Mode",     new Color(0x2980B9)),
            buildChip("Single Column", new Color(0x27AE60)),
            buildChip("Unified Model", new Color(0x8E44AD)));
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

        return buildPage(grid,
            "Paginated Employee Data",
            "SmartGrid pagination  ·  1,000 rows  ·  50 per page"
                + "  ·  per-page footer aggregates (count, salary sum, active count)  ·  page navigation",
            darkTheme,
            buildChip("Paginated",   new Color(0x2980B9)),
            buildChip("Aggregates",  new Color(0x27AE60)),
            buildChip("Navigation",  new Color(0xD68910)),
            buildChip("Footer Row",  new Color(0x8E44AD)));
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

        return buildPage(grid,
            "Scripted Row Renderers",
            "SmartGrid BeanShell integration  ·  1,000 rows  ·  XML blueprint DSL"
                + "  ·  every 30th row scripted  ·  live hot-swap from BeanShell console",
            darkTheme,
            buildChip("BeanShell",     new Color(0xC0392B)),
            buildChip("XML Blueprint", new Color(0x2980B9)),
            buildChip("Scripted",      new Color(0x8E44AD)),
            buildChip("Hot-Swap",      new Color(0xD68910)));
    }

    // -------------------------------------------------------------------------
    // Tab 6: Log viewer — dark theme, JSON/stack-trace highlighting, live search
    // -------------------------------------------------------------------------

    private static JPanel buildLogTab() {
        final String[] searchHolder = {""};

        final DefaultGridModel model = new DefaultGridModel()
            .addColumn(new ColumnDef("level",   "Severity",  80,  false, false, null))
            .addColumn(new ColumnDef("content", "Log Entry", 550, false, true,  null));

        final String[] PLAIN_INFO = {
            "Application started on port 8080",
            "Loaded configuration from classpath:application.yml",
            "Connected to database at jdbc:postgresql://localhost:5432/appdb",
            "User 'admin' authenticated successfully from 192.168.1.42",
            "Cache primed: 2,048 entries loaded in 312 ms",
            "Scheduled task 'metrics-flush' completed in 45 ms",
            "HTTP GET /api/v2/users returned 200 in 18 ms",
            "Session token refreshed for user_id=7712",
            "Batch import completed: 500 records written, 0 rejected",
            "Health check passed: db=UP, cache=UP, queue=UP"
        };
        final String[] JSON_PAYLOADS = {
            "{\"event\": \"order.created\", \"orderId\": 9921, \"userId\": 4401, \"total\": 129.95}",
            "{\"event\": \"user.login\", \"userId\": 7712, \"ip\": \"10.0.0.5\", \"success\": true}",
            "{\"level\": \"INFO\", \"service\": \"payment-svc\", \"duration_ms\": 203, \"status\": \"ok\"}",
            "{\"metric\": \"heap_used_mb\", \"value\": 412, \"threshold\": 1024, \"ts\": \"2026-05-15T10:22:00Z\"}",
            "{\"request\": {\"method\": \"POST\", \"path\": \"/api/checkout\"}, \"response\": {\"status\": 201}}"
        };
        final String[] WARN_MSGS = {
            "Connection pool approaching limit: 45/50 active connections",
            "Slow query detected (892 ms): SELECT * FROM audit_log WHERE created_at > ?",
            "Retry attempt 2/3 for downstream service 'inventory-api'",
            "Disk usage on /var/data at 78%; threshold is 80%"
        };
        final String[] ERROR_MSGS = {
            "NullPointerException in OrderController.createOrder(OrderController.java:88)",
            "Failed to acquire lock on resource 'tx-7821' after 30000 ms",
            "Circuit breaker OPEN for service 'notification-svc'",
            "Database connection lost; attempting reconnect..."
        };
        final String[] STACK_TRACES = {
            "java.lang.NullPointerException: Cannot read field \"id\" because \"order\" is null\n"
                + "  at com.example.OrderController.createOrder(OrderController.java:88)\n"
                + "  at com.example.api.ApiDispatcher.dispatch(ApiDispatcher.java:204)\n"
                + "  ... 14 more",
            "java.util.concurrent.TimeoutException: Timed out after 30000 ms\n"
                + "  at com.example.lock.DistributedLock.acquire(DistributedLock.java:51)\n"
                + "  at com.example.tx.TxManager.begin(TxManager.java:137)\n"
                + "  ... 8 more"
        };
        final String[] DEBUG_MSGS = {
            "Entering method UserService.findById() with id=4401",
            "Cache miss for key 'user:4401'; fetching from DB",
            "SQL: SELECT id, name, email FROM users WHERE id = ?  [params: 4401]",
            "Exiting UserService.findById() with result: User{id=4401, name='Alice'}"
        };

        for (int i = 1; i <= 150; i++) {
            String level;
            String content;
            int bucket = i % 8;
            switch (bucket) {
                case 0: case 1: case 2:
                    level   = "INFO";
                    content = PLAIN_INFO[i % PLAIN_INFO.length];
                    break;
                case 3:
                    level   = "INFO";
                    content = JSON_PAYLOADS[i % JSON_PAYLOADS.length];
                    break;
                case 4:
                    level   = "WARN";
                    content = WARN_MSGS[i % WARN_MSGS.length];
                    break;
                case 5:
                    level   = "ERROR";
                    content = (i % 20 == 5)
                            ? STACK_TRACES[i % STACK_TRACES.length]
                            : ERROR_MSGS[i % ERROR_MSGS.length];
                    break;
                case 6:
                    level   = "DEBUG";
                    content = DEBUG_MSGS[i % DEBUG_MSGS.length];
                    break;
                default:
                    level   = "ERROR";
                    content = STACK_TRACES[i % STACK_TRACES.length];
                    break;
            }
            model.addRow(new GridRow()
                .put("level",   level)
                .put("content", content)
                .setTag("fnd-type",  "log-row")
                .setTag("log-level", level.toLowerCase()));
        }

        SmartGrid grid = new SmartGrid(model, true);
        grid.setRowHeight(64);
        grid.setRowNumbersVisible(true);

        final int[] columnWidths = grid.getColumnWidths();
        grid.registerRowRenderer("log-row",
            () -> new LogRowPanel(columnWidths, searchHolder, grid.getSelectionModel()));

        JTextField searchField = new JTextField(28);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void onChanged() {
                String term = searchField.getText().trim();
                searchHolder[0] = term;
                if (term.isEmpty()) {
                    grid.clearFilter();
                } else {
                    final String lower = term.toLowerCase();
                    grid.setFilter(row -> {
                        Object c = row.get("content");
                        Object l = row.get("level");
                        return (c != null && c.toString().toLowerCase().contains(lower))
                            || (l != null && l.toString().toLowerCase().contains(lower));
                    });
                }
            }
            @Override public void insertUpdate(DocumentEvent e)  { onChanged(); }
            @Override public void removeUpdate(DocumentEvent e)  { onChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onChanged(); }
        });

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setForeground(java.awt.Color.WHITE);
        grid.getToolbar().add(searchLabel);
        grid.getToolbar().add(searchField);

        // Page is always dark — the grid is always dark and the surrounding chrome should match
        return buildPage(grid,
            "Log Viewer",
            "SmartGrid log mining  ·  150 entries  ·  plain text, JSON payloads, stack traces"
                + "  ·  live search filters rows and highlights matches",
            true,
            buildChip("Dark Theme",  new Color(0x34495E)),
            buildChip("JSON Syntax", new Color(0xD68910)),
            buildChip("Live Search", new Color(0x27AE60)),
            buildChip("Log Mining",  new Color(0x8E44AD)));
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
