package org.katacode.agents.auditor.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.font.TextAttribute;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jwellman.swing.grid.ColumnDef;
import org.jwellman.swing.grid.DefaultGridModel;
import org.jwellman.swing.grid.GridModelFilter;
import org.jwellman.swing.grid.GridRow;
import org.jwellman.swing.grid.SmartGrid;
import org.katacode.agents.auditor.model.ProjectStatus;
import org.katacode.agents.auditor.model.RegisteredProject;

/**
 * The "Registered Projects" panel: header, status filters and a SmartGrid in
 * list mode with one {@link ProjectCell} per project.
 */
@SuppressWarnings("serial")
public class ProjectSidebar extends JPanel {

    private static final String ROW_TYPE = "project-cell";

    private final DefaultGridModel model;
    private final SmartGrid grid;

    /** Remembered across filtering, because SmartGrid selects by row index. */
    private RegisteredProject selectedProject;
    private Consumer<RegisteredProject> selectionCallback;

    public ProjectSidebar(List<RegisteredProject> projects) {
        super(new BorderLayout(0, 10));

        setBackground(AuditorTheme.sidebarBackground());
        // Side padding is 3px short of the design: each cell insets itself for its glow
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, AuditorTheme.lineSubtle()),
                BorderFactory.createEmptyBorder(20, 15, 20, 13)));

        model = new DefaultGridModel()
                .addColumn(new ColumnDef("name", "Project", 200, false, false, null));
        for (RegisteredProject project : projects) {
            GridRow row = new GridRow()
                    .put(ProjectCell.PROJECT_KEY, project)
                    .setTag("fnd-type", ROW_TYPE);
            model.addRow(row);
        }

        grid = createGrid();

        add(createHeader(projects.size()), BorderLayout.NORTH);
        add(grid, BorderLayout.CENTER);

        model.notifyDataChanged();
        if (model.getRowCount() > 0) {
            grid.getSelectionModel().setSelectionInterval(0, 0);
        }
    }

    /**
     * Registers the listener for project selection. It is called immediately with
     * the current selection (if any), then on every later selection. Filtering a
     * selected project out of view does not call it.
     */
    public void setSelectionCallback(Consumer<RegisteredProject> callback) {
        this.selectionCallback = callback;
        notifySelection();
    }

    private void notifySelection() {
        if (selectionCallback != null && selectedProject != null) {
            selectionCallback.accept(selectedProject);
        }
    }

    private SmartGrid createGrid() {
        final SmartGrid smartGrid = new SmartGrid(model, true);
        smartGrid.setRowHeight(ProjectCell.ROW_HEIGHT);
        smartGrid.setRowNumbersVisible(false);
        smartGrid.setCheckboxColumnVisible(false);
        smartGrid.setColumnHeaderVisible(false);
        smartGrid.setSummaryRowVisible(false);
        smartGrid.setCanvasBackground(AuditorTheme.sidebarBackground());
        smartGrid.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        smartGrid.registerRowRenderer(ROW_TYPE,
                () -> new ProjectCell(smartGrid.getSelectionModel()));

        smartGrid.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int index = smartGrid.getSelectionModel().getMinSelectionIndex();
                if (!e.getValueIsAdjusting() && index >= 0 && index < model.getRowCount()) {
                    selectedProject = projectAt(index);
                    notifySelection();
                }
            }
        });

        return smartGrid;
    }

    private JPanel createHeader(int projectCount) {
        JLabel title = new JLabel("REGISTERED PROJECTS (" + projectCount + ")");
        title.setFont(AuditorTheme.uiFont(Font.BOLD, 14f)
                .deriveFont(Collections.singletonMap(TextAttribute.TRACKING, 0.05f)));
        title.setForeground(AuditorTheme.textSecondary());
        title.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filters.setOpaque(false);

        ButtonGroup group = new ButtonGroup();
        JToggleButton all = createFilterButton("[All]", null, group);
        filters.add(all);
        filters.add(createFilterButton("[Needs Attention]", new GridModelFilter() {
            @Override
            public boolean accept(GridRow row) {
                return statusOf(row).needsAttention();
            }
        }, group));
        filters.add(createFilterButton("[Waived]", new GridModelFilter() {
            @Override
            public boolean accept(GridRow row) {
                return statusOf(row) == ProjectStatus.WAIVED;
            }
        }, group));
        all.setSelected(true);

        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        header.add(title, BorderLayout.NORTH);
        header.add(filters, BorderLayout.SOUTH);

        return header;
    }

    private JToggleButton createFilterButton(String text, final GridModelFilter filter,
            ButtonGroup group) {
        final JToggleButton button = new JToggleButton(text);
        button.setFont(AuditorTheme.uiFont(Font.PLAIN, 14f));
        button.setForeground(AuditorTheme.textSecondary());
        button.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                button.setForeground(button.isSelected()
                        ? AuditorTheme.accentText()
                        : AuditorTheme.textSecondary());
            }
        });
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyFilter(filter);
            }
        });
        group.add(button);

        return button;
    }

    /** Applies a filter (null shows all) and restores the selection if still visible. */
    private void applyFilter(GridModelFilter filter) {
        RegisteredProject previous = selectedProject;

        if (filter == null) {
            grid.clearFilter();
        } else {
            grid.setFilter(filter);
        }

        for (int i = 0; i < model.getRowCount(); i++) {
            if (projectAt(i) == previous) {
                grid.getSelectionModel().setSelectionInterval(i, i);
                break;
            }
        }
    }

    private RegisteredProject projectAt(int index) {
        return (RegisteredProject) model.getRow(index).get(ProjectCell.PROJECT_KEY);
    }

    private static ProjectStatus statusOf(GridRow row) {
        return ((RegisteredProject) row.get(ProjectCell.PROJECT_KEY)).getProjectStatus();
    }
}
