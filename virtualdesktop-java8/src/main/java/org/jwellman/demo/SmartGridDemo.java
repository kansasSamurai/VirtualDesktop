package org.jwellman.demo;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.jwellman.swing.grid.ColumnDef;
import org.jwellman.swing.grid.DefaultGridModel;
import org.jwellman.swing.grid.GridRow;
import org.jwellman.swing.grid.SmartGrid;

/**
 * Three-tab demo for SmartGrid MVP:
 *
 *  Tab 1 "Table" — 1,000-row flat employee dataset; demonstrates viewport
 *                  virtualization (~20 live components regardless of row count).
 *
 *  Tab 2 "Tree"  — departments with child employees; demonstrates expand/collapse
 *                  on the same SmartGrid component with no extra code paths.
 *
 *  Tab 3 "List"  — single-column view; demonstrates that a list is simply a
 *                  1-column SmartGrid with the same unified model.
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

            JFrame frame = new JFrame("SmartGrid MVP Demo");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(800, 550);
            frame.add(tabs);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // -------------------------------------------------------------------------
    // Tab 1: flat table
    // -------------------------------------------------------------------------

    private static JPanel buildTableTab() {
        DefaultGridModel model = new DefaultGridModel()
            .addColumn(new ColumnDef("id",     "ID",         60,  false, true, null))
            .addColumn(new ColumnDef("name",   "Name",       180, false, true, null))
            .addColumn(new ColumnDef("dept",   "Department", 160, false, true, null))
            .addColumn(new ColumnDef("salary", "Salary",     100, false, true, null))
            .addColumn(new ColumnDef("status", "Status",      80, false, true, null));

        for (int i = 1; i <= 1000; i++) {
            GridRow row = new GridRow()
                .put("id",     String.valueOf(i))
                .put("name",   "Employee " + i)
                .put("dept",   DEPTS[i % DEPTS.length])
                .put("salary", String.format("$%,d", 50_000 + (i * 173 % 100_000)))
                .put("status", STATUSES[i % STATUSES.length]);
            if (i % 7 == 0) row.setTag("fnd-style", "warning-glow");
            model.addRow(row);
        }

        return wrap(new SmartGrid(model),
            "1,000 rows — only ~20 live components exist at any time (viewport virtualization)");
    }

    // -------------------------------------------------------------------------
    // Tab 2: tree / hierarchy
    // -------------------------------------------------------------------------

    private static JPanel buildTreeTab() {
        DefaultGridModel model = new DefaultGridModel()
            .addColumn(new ColumnDef("name",   "Name / Department", 220, false, true, null))
            .addColumn(new ColumnDef("role",   "Role",              160, false, true, null))
            .addColumn(new ColumnDef("salary", "Salary",            100, false, true, null));

        // { department name, employee count }
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
                .setDepth(0)
                .setHasChildren(true)
                .setExpanded(false));

            for (int e = 0; e < count; e++) {
                model.addRow(new GridRow()
                    .put("name",   "Employee " + empId)
                    .put("role",   roles[empId % roles.length])
                    .put("salary", String.format("$%,d", 55_000 + (empId * 211 % 90_000)))
                    .setDepth(1)
                    .setHasChildren(false));
                empId++;
            }
        }

        return wrap(new SmartGrid(model),
            "Click any department row to expand or collapse its employees");
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

        return wrap(new SmartGrid(model),
            "Single-column SmartGrid — a list is just a 1-column table sharing the same model");
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static JPanel wrap(SmartGrid grid, String description) {
        JLabel desc = new JLabel(" " + description);
        desc.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(grid, BorderLayout.CENTER);
        panel.add(desc, BorderLayout.SOUTH);
        return panel;
    }
}
