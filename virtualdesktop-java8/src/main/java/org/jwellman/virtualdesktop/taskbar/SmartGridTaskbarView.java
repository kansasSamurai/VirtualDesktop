package org.jwellman.virtualdesktop.taskbar;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jwellman.swing.grid.ColumnDef;
import org.jwellman.swing.grid.DefaultGridModel;
import org.jwellman.swing.grid.GridRow;
import org.jwellman.swing.grid.SmartGrid;

/**
 * SmartGrid-based implementation of TaskbarView.
 *
 * Renders taskbar tool entries as compact 34px rows using TaskbarRowComponent,
 * with inline group expansion: when grouping is enabled the controller passes
 * group TaskbarItems and this view expands each group into a bold group header
 * row followed by indented child rows — all within the same SmartGrid canvas.
 *
 * Theme colors are propagated to the row component pool via a shared mutable
 * TaskbarTheme[] reference (the same pattern as CalendarDemo's highlightOn[]).
 * Calling applyTheme() updates the holder and fires notifyDataChanged() so the
 * next bind() cycle picks up the new colors without recreating pool instances.
 */
public class SmartGridTaskbarView implements TaskbarView {

    private static final String ROW_TYPE = "taskbar-tool";

    private final SmartGrid        grid;
    private final DefaultGridModel model;
    private       TaskbarViewListener listener;

    // Shared mutable reference — all pool instances read themeHolder[0].
    private final TaskbarTheme[] themeHolder = { TaskbarTheme.darkGold() };

    // Prevents selection-change events from setSelectedId() looping back.
    private boolean updatingSelection = false;

    public SmartGridTaskbarView() {
        model = new DefaultGridModel()
            .addColumn(new ColumnDef("item", "Tool", 200, false, false, null));

        grid = new SmartGrid(model, true /* dark */);
        grid.setRowHeight(34);
        grid.setCheckboxColumnVisible(false);

        final int[] columnWidths = grid.getColumnWidths();
        grid.registerRowRenderer(ROW_TYPE,
            () -> new TaskbarRowComponent(
                toolId -> { if (listener != null) listener.onItemCloseRequested(toolId); },
                grid.getSelectionModel(),
                columnWidths,
                themeHolder));

        wireSelectionListener();
    }

    // -------------------------------------------------------------------------
    // TaskbarView
    // -------------------------------------------------------------------------

    @Override
    public JComponent getComponent() {
        return grid;
    }

    @Override
    public void setItems(List<TaskbarItem> items) {
        model.clearRows();

        for (TaskbarItem item : items) {
            if (item.isGroup()) {
                // Group header — rendered by the built-in GroupHeaderRowPanel
                model.addRow(new GridRow()
                    .put("name", item.getToolType() + "  (" + item.getGroupSize() + ")")
                    .setGroupHeader(true));

                // Inline children — indented visually via left padding in
                // TaskbarRowComponent; each child is a full tool row.
                for (TaskbarItem child : item.getGroupedItems()) {
                    model.addRow(buildToolRow(child));
                }
            } else {
                model.addRow(buildToolRow(item));
            }
        }

        model.notifyDataChanged();
    }

    @Override
    public void setSelectedId(String toolId) {
        if (toolId == null) {
            if (!grid.getSelectionModel().isSelectionEmpty()) {
                updatingSelection = true;
                grid.getSelectionModel().clearSelection();
                updatingSelection = false;
            }
            return;
        }

        int count = model.getRowCount();
        for (int i = 0; i < count; i++) {
            GridRow row = model.getRow(i);
            if (row.isGroupHeader()) {
                continue;
            }
            Object obj = row.get("item");
            if (obj instanceof TaskbarItem) {
                if (toolId.equals(((TaskbarItem) obj).getId())) {
                    if (!grid.getSelectionModel().isSelectedIndex(i)) {
                        updatingSelection = true;
                        grid.getSelectionModel().setSelectionInterval(i, i);
                        updatingSelection = false;
                    }
                    return;
                }
            }
        }
    }

    @Override
    public void setListener(TaskbarViewListener listener) {
        this.listener = listener;
    }

    @Override
    public void applyTheme(TaskbarTheme theme) {
        themeHolder[0] = theme;
        // Trigger re-bind so pool instances pick up new colors immediately.
        model.notifyDataChanged();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private GridRow buildToolRow(TaskbarItem item) {
        return new GridRow()
            .put("item", item)
            .setTag("fnd-type", ROW_TYPE);
    }

    private void wireSelectionListener() {
        grid.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() || updatingSelection || listener == null) {
                    return;
                }
                int idx = grid.getSelectionModel().getMinSelectionIndex();
                if (idx < 0 || idx >= model.getRowCount()) {
                    return;
                }
                GridRow row = model.getRow(idx);
                if (row.isGroupHeader()) {
                    return;
                }
                Object obj = row.get("item");
                if (obj instanceof TaskbarItem) {
                    TaskbarItem item = (TaskbarItem) obj;
                    listener.onItemSelected(item.getId(), false);
                }
            }
        });
    }

}
