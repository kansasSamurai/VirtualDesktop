package org.jwellman.lucene.ui;

import org.jwellman.lucene.engine.LuceneService;
import org.jwellman.lucene.model.DirectorySandboxConfig;
import org.jwellman.lucene.model.IndexRowItem;
import org.jwellman.swing.grid.ColumnDef;
import org.jwellman.swing.grid.DefaultGridModel;
import org.jwellman.swing.grid.GridRow;
import org.jwellman.swing.grid.SmartGrid;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

/**
 * Left sidebar for the Lucene Management UI, backed by a single-column SmartGrid.
 *
 * <p>Each row is a {@link LuceneSidebarRowComponent} showing the sandbox name,
 * document count, status dot, and (when scanning) a live progress bar.
 * A synthetic "Global" row is always first.</p>
 *
 * <p>The footer zone contains an "Add Sandbox" toolbar with a {@code +} button
 * added via {@link SmartGrid#addFooterRow(JComponent)}.</p>
 *
 * <p>A 1-second {@link Timer} calls {@link #reload()} so row components re-bind
 * against the latest {@link org.jwellman.lucene.model.SandboxRuntimeState} values
 * without the caller needing to push updates.</p>
 */
public class LuceneSidebarPanel extends JPanel {

    private static final String ROW_TYPE   = "lucene-sidebar-row";

    /** Sentinel for the synthetic Global row (config == null signals global). */
    static final IndexRowItem GLOBAL_ROW = new IndexRowItem(null, null);

    private final DefaultGridModel model;
    private final SmartGrid        grid;
    private Consumer<IndexRowItem> selectionCallback;

    public LuceneSidebarPanel() {
        super(new BorderLayout());

        model = new DefaultGridModel()
            .addColumn(new ColumnDef("name", "Sandbox", 200, false, false, null));

        grid = new SmartGrid(model, true);
        grid.setRowHeight(54);
        grid.setRowNumbersVisible(false);
        grid.setCheckboxColumnVisible(false);

        grid.registerRowRenderer(ROW_TYPE,
            () -> new LuceneSidebarRowComponent(grid.getSelectionModel()));

        grid.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && selectionCallback != null) {
                    int idx = grid.getSelectionModel().getMinSelectionIndex();
                    if (idx >= 0 && idx < model.getRowCount()) {
                        IndexRowItem item = (IndexRowItem) model.getRow(idx).get("item");
                        if (item != null) {
                            selectionCallback.accept(item);
                        }
                    }
                }
            }
        });

        grid.addFooterRow(buildAddSandboxToolbar());

        add(grid, BorderLayout.CENTER);

        new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reload();
            }
        }).start();
    }

    public void setSelectionCallback(Consumer<IndexRowItem> callback) {
        this.selectionCallback = callback;
    }

    /**
     * Rebuilds the grid rows from the current {@link LuceneService} state and
     * triggers a SmartGrid repaint. Safe to call from the EDT at any time.
     */
    public void reload() {
        model.clearRows();
        model.addRow(buildRow(GLOBAL_ROW));
        if (LuceneService.get().isInitialized()) {
            for (IndexRowItem item : LuceneService.get().buildRowItems()) {
                model.addRow(buildRow(item));
            }
        }
        model.notifyDataChanged();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static GridRow buildRow(IndexRowItem item) {
        return new GridRow()
            .put("item", item)
            .setTag("fnd-type", ROW_TYPE);
    }

    private JPanel buildAddSandboxToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        toolbar.setBackground(grid.getHeaderBackground());

        JButton addBtn = new JButton("+ Add Sandbox");
        addBtn.setFocusable(false);
        addBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onAddSandbox();
            }
        });
        toolbar.add(addBtn);
        return toolbar;
    }

    private void onAddSandbox() {
        DirectorySandboxConfig cfg = AddSandboxDialog.showDialog(this);
        if (cfg == null) {
            return;
        }
        if (!LuceneService.get().isInitialized()) {
            JOptionPane.showMessageDialog(this,
                "Lucene service is not initialized.", "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        LuceneService.get().addSandbox(cfg);
        reload();
    }
}
