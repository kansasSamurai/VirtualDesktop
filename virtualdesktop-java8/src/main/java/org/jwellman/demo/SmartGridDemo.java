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
import javax.swing.JSplitPane;
import javax.swing.JTable;
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
import javax.swing.table.DefaultTableModel;

import java.util.List;
import org.jwellman.swing.grid.ColumnDef;
import org.jwellman.swing.grid.DefaultGridComponentFactory;
import org.jwellman.swing.grid.DefaultGridModel;
import org.jwellman.swing.grid.GridModelListener;
import org.jwellman.swing.grid.GridRow;
import org.jwellman.swing.grid.JTableAdapter;
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

    // -------------------------------------------------------------------------
    // Shared resource classes — instantiated once, referenced everywhere
    // -------------------------------------------------------------------------

    /** Semantic chip colours shared across all tabs. */
    private static final class ChipColors {
        static final Color PRIMARY = new Color(0x2980B9); // blue   — informational / feature
        static final Color SUCCESS = new Color(0x27AE60); // green  — capability / positive
        static final Color WARNING = new Color(0xD68910); // amber  — configuration / notable
        static final Color DANGER  = new Color(0xC0392B); // red    — powerful / scripting
        static final Color ACCENT  = new Color(0x8E44AD); // purple — advanced / special
        static final Color MUTED   = new Color(0x34495E); // slate  — neutral
        private ChipColors() {}
    }

    /** Theme-resolved page chrome colours and borders.  One instance per tab. */
    private static final class PagePalette {
        final Color  pageBg;
        final Color  headerBg;
        final Color  titleFg;
        final Color  subtitleFg;
        final Color  sectionFg;   // section labels (e.g. "JTable — before migration")
        final Color  border;      // raw colour — use to compose additional borders
        final javax.swing.border.Border cardBorder;
        final javax.swing.border.Border headerBorder;

        PagePalette(boolean darkTheme) {
            pageBg       = darkTheme ? new Color(0x2B2D30) : new Color(0xF0F2F5);
            headerBg     = darkTheme ? new Color(0x3C3F41) : Color.WHITE;
            titleFg      = darkTheme ? new Color(0xE8E8E8) : new Color(0x1A1A2E);
            subtitleFg   = darkTheme ? new Color(0x888888) : new Color(0x666677);
            sectionFg    = darkTheme ? new Color(0xA0A0A0) : new Color(0x555566);
            border       = darkTheme ? new Color(0x4A4D52) : new Color(0xC8CDD3);
            cardBorder   = BorderFactory.createLineBorder(border);
            headerBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, border),
                Borders.PAGE_HEADER);
        }
    }

    /** Cached empty / compound borders for repeated use throughout the demo. */
    private static final class Borders {
        static final javax.swing.border.Border CHIP         = BorderFactory.createEmptyBorder(3,  7,  3,  7);
        static final javax.swing.border.Border CONTENT      = BorderFactory.createEmptyBorder(12, 12, 12, 12);
        static final javax.swing.border.Border PAGE_HEADER  = BorderFactory.createEmptyBorder(12, 16, 12, 16);
        static final javax.swing.border.Border LABEL        = BorderFactory.createEmptyBorder(0,  8,  0,  8);
        static final javax.swing.border.Border CELL_RIGHT   = BorderFactory.createEmptyBorder(0,  4,  0,  8);
        static final javax.swing.border.Border FOOTER       = BorderFactory.createEmptyBorder(2,  8,  2,  8);
        static final javax.swing.border.Border FOOTER_RIGHT = BorderFactory.createEmptyBorder(2,  4,  2,  8);
        private Borders() {}
    }

    // -------------------------------------------------------------------------
    // Data arrays shared across tabs
    // -------------------------------------------------------------------------

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
     *   <li>"Codes"    — multi-column list; variable-length codes + descriptions; global search</li>
     *   <li>"Paged"    — 1,000 rows / 50 per page; footer aggregates; pagination bar</li>
     *   <li>"Scripted" — Phase 9b: XML blueprint + BeanShell bind scripts</li>
     *   <li>"Log"      — dark log viewer; plain text, JSON, stack traces; live search</li>
     *   <li>"Migrate"  — JTableAdapter demo; same 50 rows in SmartGrid (top) and JTable (bottom)</li>
     * </ul>
     *
     * @param darkTheme {@code true} to apply SmartGrid's dark colour palette
     */
    public static JTabbedPane createDemoTabs(boolean darkTheme) {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Table",    buildTableTab(darkTheme));
        tabs.addTab("Tree",     buildTreeTab(darkTheme));
        tabs.addTab("List",     buildListTab(darkTheme));
        tabs.addTab("Codes",    buildCodeListTab(darkTheme));
        tabs.addTab("Paged",    buildPagedTab(darkTheme));
        tabs.addTab("Scripted", buildScriptedTab(darkTheme));
        tabs.addTab("Log",      buildLogTab());
        tabs.addTab("Migrate",  buildMigrateTab(darkTheme));
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
        grid.registerCellRenderer("currency", (col, value, row, existing) -> {
            JLabel lbl = (existing instanceof JLabel) ? (JLabel) existing : new JLabel();
            lbl.setText(value != null ? String.format("$%,d", ((Number) value).longValue()) : "");
            lbl.setHorizontalAlignment(JLabel.RIGHT);
            lbl.setBorder(Borders.CELL_RIGHT);
            return lbl;
        });

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

        grid.setFooterRenderer((col, pageRows, fullModel) -> {
            JLabel lbl = new JLabel();
            lbl.setBorder(Borders.FOOTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
            switch (col.getKey()) {
                case "id":
                    lbl.setText("Totals");
                    break;
                case "salary":
                    long salarySum = 0;
                    for (GridRow r : pageRows) {
                        Object n = r.get("salary");
                        if (n instanceof Number) {
                            salarySum += ((Number) n).longValue();
                        }
                    }
                    lbl.setText(String.format("$%,d", salarySum));
                    lbl.setHorizontalAlignment(JLabel.RIGHT);
                    lbl.setBorder(Borders.FOOTER_RIGHT);
                    break;
                case "status":
                    int activeCount = 0;
                    for (GridRow r : pageRows) {
                        if ("Active".equals(r.get("status"))) {
                            activeCount++;
                        }
                    }
                    lbl.setText(activeCount + " active");
                    break;
                default:
                    break;
            }
            return lbl;
        });

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
        // toolbar.add(Box.createHorizontalStrut(grid.getCanvasLeadWidth()));
        toolbar.add(filterLabel);
        toolbar.add(filterField);
        toolbar.add(editToggle);

        return buildPage(grid,
            "Employee Directory",
            "SmartGrid demo  ·  1,000 rows  ·  live filter & sort"
                + "  ·  interactive cell renderers  ·  inline edit mode",
            darkTheme,
            buildChip("Virtualized", ChipColors.PRIMARY),
            buildChip("Sortable",    ChipColors.SUCCESS),
            buildChip("Filterable",  ChipColors.WARNING),
            buildChip("Interactive", ChipColors.ACCENT),
            buildChip("Edit Mode",   ChipColors.DANGER));
    }

    private static JLabel buildChip(String text, Color bg) {
        JLabel chip = new JLabel(text);
        chip.setFont(chip.getFont().deriveFont(Font.BOLD, 10f));
        chip.setForeground(Color.WHITE);
        chip.setBackground(bg);
        chip.setOpaque(true);
        chip.setBorder(Borders.CHIP);
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
        PagePalette p = new PagePalette(darkTheme);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setForeground(p.titleFg);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 11f));
        subtitleLabel.setForeground(p.subtitleFg);

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
        header.setBackground(p.headerBg);
        header.setBorder(p.headerBorder);
        header.add(titleArea,   BorderLayout.WEST);
        header.add(chipsPanel,  BorderLayout.EAST);

        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(p.cardBorder);
        card.add(grid, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(Borders.CONTENT);
        content.add(card, BorderLayout.CENTER);

        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(p.pageBg);
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
            buildChip("Tree View",    ChipColors.PRIMARY),
            buildChip("Expandable",   ChipColors.SUCCESS),
            buildChip("Grouped",      ChipColors.ACCENT),
            buildChip("Hierarchical", ChipColors.WARNING));
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
            buildChip("List Mode",     ChipColors.PRIMARY),
            buildChip("Single Column", ChipColors.SUCCESS),
            buildChip("Unified Model", ChipColors.ACCENT));
    }

    // -------------------------------------------------------------------------
    // Tab 4: multi-column list — variable-length code + description
    // -------------------------------------------------------------------------

    private static JPanel buildCodeListTab(boolean darkTheme) {
        final DefaultGridModel model = new DefaultGridModel()
            .addColumn(new ColumnDef("code",   "Code",        90, false, false, "mono-code"))
            .addColumn(new ColumnDef("action", "Description", 460, false, true,  null));

        // Business action codes with intentionally variable-length keys —
        // the original problem that motivated this demo: DOS programs often had
        // action codes of varying length alongside their descriptions.
        final String[][] entries = {
            {"GL",       "General Ledger Entry"},
            {"AP",       "Accounts Payable Invoice"},
            {"AR",       "Accounts Receivable"},
            {"PO",       "Purchase Order"},
            {"CR",       "Credit Memo"},
            {"DR",       "Debit Memo"},
            {"RECV",     "Goods Receipt"},
            {"INVT",     "Inventory Adjustment"},
            {"PYMT",     "Payment Processing"},
            {"ACCT",     "Account Reconciliation"},
            {"AUDIT",    "Internal Audit Review"},
            {"DEPR",     "Depreciation Schedule"},
            {"BUDGET",   "Budget Variance Report"},
            {"CAPEX",    "Capital Expenditure Request"},
            {"OPEX",     "Operating Expense Approval"},
            {"FOREX",    "Foreign Exchange Transaction"},
            {"INTERCO",  "Intercompany Transfer"},
            {"CONSOL",   "Consolidated Reporting"},
            {"CLOSE",    "Period Close Procedure"},
            {"RECON",    "Bank Reconciliation"},
            {"FIXED",    "Fixed Asset Register"},
            {"LEASE",    "Lease Accounting Entry"},
            {"REV",      "Revenue Recognition"},
            {"COGS",     "Cost of Goods Sold"},
            {"WIP",      "Work in Progress"},
            {"PAYROLL",  "Payroll Processing"},
            {"BENEFITS", "Employee Benefits Enrollment"},
            {"OVERHEAD", "Overhead Allocation"},
            {"TAX",      "Tax Calculation"},
            {"MGMT",     "Management Report"},
            {"RMA",      "Return Merchandise Authorization"},
            {"SLA",      "Service Level Agreement Review"},
            {"KPI",      "Key Performance Indicator Report"},
            {"ESG",      "Environmental Social Governance"},
            {"IMPL",     "System Implementation Request"},
        };

        for (String[] entry : entries) {
            model.addRow(new GridRow()
                .put("code",   entry[0])
                .put("action", entry[1]));
        }

        SmartGrid grid = new SmartGrid(model, darkTheme);
        grid.setRowNumbersVisible(true);
        grid.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Monospaced bold renderer for the code column — makes variable-length
        // codes visually distinct from the description without needing fixed width.
        grid.registerCellRenderer("mono-code", (col, value, row, existing) -> {
            JLabel lbl = (existing instanceof JLabel) ? (JLabel) existing : new JLabel();
            lbl.setText(value != null ? value.toString() : "");
            lbl.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 4));
            return lbl;
        });

        // Search field lives outside the renderer so it retains text across
        // rebuildHeaderView() calls triggered by viewport resize.
        final String[] searchHolder = {""};
        final JTextField codeSearch = new JTextField(16);
        codeSearch.getDocument().addDocumentListener(new DocumentListener() {
            private void applyFilter() {
                String term = codeSearch.getText().trim().toLowerCase();
                searchHolder[0] = term;
                if (term.isEmpty()) {
                    grid.clearFilter();
                } else {
                    grid.setFilter(row -> {
                        Object code   = row.get("code");
                        Object action = row.get("action");
                        return (code   != null && code.toString().toLowerCase().contains(term))
                            || (action != null && action.toString().toLowerCase().contains(term));
                    });
                }
            }
            @Override public void insertUpdate(DocumentEvent e)  { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e)  { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        // Custom header: code column gets a plain label; description column
        // gets the label + embedded search field (searches both columns globally).
        grid.setHeaderRenderer((col, sortOrder, rank) -> {
            if ("action".equals(col.getKey())) {
                JPanel cell = new JPanel(new BorderLayout(4, 0));
                cell.setOpaque(false);
                JLabel lbl = new JLabel(col.getHeader());
                lbl.setForeground(Color.WHITE);
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                lbl.setBorder(Borders.LABEL);
                JPanel fieldWrap = new JPanel(new BorderLayout());
                fieldWrap.setOpaque(false);
                fieldWrap.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 6));
                fieldWrap.add(codeSearch, BorderLayout.CENTER);
                cell.add(lbl,       BorderLayout.CENTER);
                cell.add(fieldWrap, BorderLayout.EAST);
                return cell;
            } else {
                JLabel lbl = new JLabel(col.getHeader());
                lbl.setForeground(Color.WHITE);
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                lbl.setBorder(Borders.LABEL);
                lbl.setOpaque(false);
                return lbl;
            }
        });

        return buildPage(grid,
            "Business Action Codes",
            "Multi-column list demo  ·  35 entries  ·  variable-length codes"
                + "  ·  global search across code and description",
            darkTheme,
            buildChip("Multi-Column",  ChipColors.PRIMARY),
            buildChip("Code List",     ChipColors.SUCCESS),
            buildChip("Variable Keys", ChipColors.WARNING),
            buildChip("Global Search", ChipColors.ACCENT));
    }

    // -------------------------------------------------------------------------
    // Tab 5: explicit pagination with column-aligned footer aggregates
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
            lbl.setBorder(Borders.FOOTER);
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
            buildChip("Paginated",   ChipColors.PRIMARY),
            buildChip("Aggregates",  ChipColors.SUCCESS),
            buildChip("Navigation",  ChipColors.WARNING),
            buildChip("Footer Row",  ChipColors.ACCENT));
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
            buildChip("BeanShell",     ChipColors.DANGER),
            buildChip("XML Blueprint", ChipColors.PRIMARY),
            buildChip("Scripted",      ChipColors.ACCENT),
            buildChip("Hot-Swap",      ChipColors.WARNING));
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
        JPanel logToolbar = grid.getToolbar();
        // logToolbar.add(Box.createHorizontalStrut(grid.getCanvasLeadWidth()));
        logToolbar.add(searchLabel);
        logToolbar.add(searchField);

        // Page is always dark — the grid is always dark and the surrounding chrome should match
        return buildPage(grid,
            "Log Viewer",
            "SmartGrid log mining  ·  150 entries  ·  plain text, JSON payloads, stack traces"
                + "  ·  live search filters rows and highlights matches",
            true,
            buildChip("Dark Theme",  ChipColors.MUTED),
            buildChip("JSON Syntax", ChipColors.WARNING),
            buildChip("Live Search", ChipColors.SUCCESS),
            buildChip("Log Mining",  ChipColors.ACCENT));
    }

    // -------------------------------------------------------------------------
    // Tab 9: migration demo — SmartGrid (top) vs JTable (bottom), same 50 rows
    // -------------------------------------------------------------------------

    private static JPanel buildMigrateTab(boolean darkTheme) {

        // Build the DefaultTableModel first — as a real migrator would have it
        DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Name", "Department", "Salary", "Status"}, 0);
        for (int i = 1; i <= 50; i++) {
            tableModel.addRow(new Object[]{
                String.valueOf(i),
                "Employee " + i,
                DEPTS[i % DEPTS.length],
                String.format("$%,d", 50_000 + (i * 173 % 100_000)),
                STATUSES[i % STATUSES.length]
            });
        }

        // Standard JTable setup — no filtering, no alternating rows, no summary
        JTable jtable = new JTable(tableModel);
        jtable.setFillsViewportHeight(true);
        jtable.setAutoCreateRowSorter(true);
        jtable.getColumnModel().getColumn(0).setPreferredWidth(50);
        jtable.getColumnModel().getColumn(1).setPreferredWidth(220);
        jtable.getColumnModel().getColumn(2).setPreferredWidth(180);
        jtable.getColumnModel().getColumn(3).setPreferredWidth(110);
        jtable.getColumnModel().getColumn(4).setPreferredWidth(80);
        JScrollPane jtableScroll = new JScrollPane(jtable);

        // This single call is the migration — hand the existing JTable to the adapter
        DefaultGridModel smartModel = JTableAdapter.toSmartGrid(jtable);

        SmartGrid grid = new SmartGrid(smartModel, darkTheme);
        grid.setRowNumbersVisible(true);

        JTextField filterField = new JTextField(18);
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            private void applyFilter() {
                String text = filterField.getText().trim().toLowerCase();
                if (text.isEmpty()) {
                    grid.clearFilter();
                } else {
                    grid.setFilter(row -> {
                        for (ColumnDef col : smartModel.getColumns()) {
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
        JLabel filterLabel = new JLabel("Filter:");
        filterLabel.setForeground(Color.WHITE);
        JPanel migrateToolbar = grid.getToolbar();
        // migrateToolbar.add(Box.createHorizontalStrut(grid.getCanvasLeadWidth()));
        migrateToolbar.add(filterLabel);
        migrateToolbar.add(filterField);

        // Page chrome — same style as buildPage() but hosting two components
        PagePalette p = new PagePalette(darkTheme);

        JLabel titleLabel = new JLabel("SmartGrid Migration Demo");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setForeground(p.titleFg);

        JLabel subtitleLabel = new JLabel(
            "JTableAdapter converts the existing JTable in one call"
                + "  ·  50 rows  ·  same data, different component");
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 11f));
        subtitleLabel.setForeground(p.subtitleFg);

        JPanel titleArea = new JPanel();
        titleArea.setLayout(new BoxLayout(titleArea, BoxLayout.Y_AXIS));
        titleArea.setOpaque(false);
        titleArea.add(titleLabel);
        titleArea.add(Box.createVerticalStrut(4));
        titleArea.add(subtitleLabel);

        JPanel chipsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        chipsPanel.setOpaque(false);
        chipsPanel.add(buildChip("Migration",    ChipColors.ACCENT));
        chipsPanel.add(buildChip("Adapter API",  ChipColors.PRIMARY));
        chipsPanel.add(buildChip("50 Rows",      ChipColors.SUCCESS));
        chipsPanel.add(buildChip("Side by Side", ChipColors.WARNING));

        JPanel pageHeader = new JPanel(new BorderLayout(16, 0));
        pageHeader.setBackground(p.headerBg);
        pageHeader.setBorder(p.headerBorder);
        pageHeader.add(titleArea,   BorderLayout.WEST);
        pageHeader.add(chipsPanel,  BorderLayout.EAST);

        JLabel sgLabel = new JLabel("  SmartGrid  —  after migration");
        sgLabel.setFont(sgLabel.getFont().deriveFont(Font.BOLD, 11f));
        sgLabel.setForeground(Color.WHITE);
        sgLabel.setBackground(new Color(0x3C4B64));
        sgLabel.setOpaque(true);
        sgLabel.setBorder(Borders.FOOTER);

        JPanel sgCard = new JPanel(new BorderLayout());
        sgCard.setBorder(p.cardBorder);
        sgCard.add(sgLabel, BorderLayout.NORTH);
        sgCard.add(grid,    BorderLayout.CENTER);

        JLabel jtLabel = new JLabel("  JTable  —  before migration");
        jtLabel.setFont(jtLabel.getFont().deriveFont(Font.BOLD, 11f));
        jtLabel.setForeground(p.sectionFg);
        jtLabel.setBorder(Borders.FOOTER);

        JPanel jtCard = new JPanel(new BorderLayout());
        jtCard.setBorder(p.cardBorder);
        jtCard.add(jtLabel,      BorderLayout.NORTH);
        jtCard.add(jtableScroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sgCard, jtCard);
        split.setResizeWeight(0.6);
        split.setDividerSize(6);
        split.setBorder(null);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(Borders.CONTENT);
        content.add(split, BorderLayout.CENTER);

        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(p.pageBg);
        page.add(pageHeader, BorderLayout.NORTH);
        page.add(content,    BorderLayout.CENTER);
        return page;
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
