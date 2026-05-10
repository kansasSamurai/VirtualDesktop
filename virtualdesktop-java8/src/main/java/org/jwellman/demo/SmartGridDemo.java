package org.jwellman.demo;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jwellman.swing.grid.ColumnDef;
import org.jwellman.swing.grid.DefaultGridModel;
import org.jwellman.swing.grid.GridRow;
import org.jwellman.swing.grid.SmartGrid;

/**
 * Four-tab demo for SmartGrid:
 *
 *  "Table" — 1,000 flat rows; viewport virtualization; proportional column widths
 *  "Tree"  — departments + employees; expand/collapse; unified model
 *  "List"  — single-column SmartGrid; demonstrates list-is-a-table
 *  "Paged" — 1,000 rows / 50 per page; footer aggregates; pagination bar
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
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Table", buildTableTab());
            tabs.addTab("Tree",  buildTreeTab());
            tabs.addTab("List",  buildListTab());
            tabs.addTab("Paged", buildPagedTab());

            JFrame frame = new JFrame("SmartGrid MVP Demo");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(820, 570);
            frame.add(tabs);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // -------------------------------------------------------------------------
    // Tab 1: flat table — 1,000 rows, proportional column widths
    // -------------------------------------------------------------------------

    private static JPanel buildTableTab() {
        DefaultGridModel model = new DefaultGridModel()
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
            if (i % 7 == 0) row.setTag("fnd-style", "warning-glow");
            model.addRow(row);
        }

        final DefaultGridModel tableModel = model;
        SmartGrid grid = new SmartGrid(model);
        grid.registerFormatter("currency", v -> String.format("$%,d", ((Number) v).longValue()));

        // Filter bar
        final int totalRows = model.getRowCount();
        JLabel countLabel  = new JLabel("Showing " + totalRows + " of " + totalRows + " rows");
        JTextField filterField = new JTextField(20);
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            private void applyFilter() {
                String text = filterField.getText().trim().toLowerCase();
                if (text.isEmpty()) {
                    grid.clearFilter();
                } else {
                    grid.setFilter(row -> {
                        for (ColumnDef col : tableModel.getColumns()) {
                            Object val = row.get(col.getKey());
                            if (val != null && val.toString().toLowerCase().contains(text)) return true;
                        }
                        return false;
                    });
                }
                countLabel.setText("Showing " + tableModel.getRowCount() + " of " + totalRows + " rows");
            }
            @Override public void insertUpdate(DocumentEvent e)  { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e)  { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        filterBar.add(new JLabel("Filter:"));
        filterBar.add(filterField);
        filterBar.add(countLabel);

        JPanel north = new JPanel(new BorderLayout());
        north.add(selectionToolbar(grid), BorderLayout.NORTH);
        north.add(filterBar,              BorderLayout.SOUTH);

        JLabel desc = new JLabel(" 1,000 rows — filter field narrows results; sort and selection persist through filter");
        desc.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JPanel tab = new JPanel(new BorderLayout());
        tab.add(north, BorderLayout.NORTH);
        tab.add(grid,  BorderLayout.CENTER);
        tab.add(desc,  BorderLayout.SOUTH);
        return tab;
    }

    // -------------------------------------------------------------------------
    // Tab 2: tree / hierarchy
    // -------------------------------------------------------------------------

    private static JPanel buildTreeTab() {
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
                .setDepth(0).setHasChildren(true).setExpanded(false));

            for (int e = 0; e < count; e++) {
                model.addRow(new GridRow()
                    .put("name",   "Employee " + empId)
                    .put("role",   roles[empId % roles.length])
                    .put("salary", String.format("$%,d", 55_000 + (empId * 211 % 90_000)))
                    .setDepth(1).setHasChildren(false));
                empId++;
            }
        }

        SmartGrid grid = new SmartGrid(model);
        return wrap(grid, "Click a department row to expand / collapse its employees");
    }

    // -------------------------------------------------------------------------
    // Tab 3: single-column list
    // -------------------------------------------------------------------------

    private static JPanel buildListTab() {
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

        SmartGrid grid = new SmartGrid(model);
        return wrap(grid, "Single-column SmartGrid — a list is just a 1-column table sharing the unified model");
    }

    // -------------------------------------------------------------------------
    // Tab 4: explicit pagination with column-aligned footer aggregates
    // -------------------------------------------------------------------------

    private static JPanel buildPagedTab() {
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

        SmartGrid grid = new SmartGrid(model);
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
                    if (sm.isSelectedIndex(i)) count++;
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
}
