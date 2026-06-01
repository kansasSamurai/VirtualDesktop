package org.jwellman.lucene.ui;

import org.jwellman.lucene.engine.LuceneService;
import org.jwellman.lucene.model.IndexRowItem;
import org.jwellman.lucene.model.SandboxRuntimeState.IndexStatus;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Left sidebar for the Lucene Management UI.
 *
 * <p>Displays a single-column list implemented as a {@link JTable} (no SmartGrid
 * dependency here — the management UI predates any SmartGrid row data binding
 * for Lucene data). Each row shows the sandbox display name, document count,
 * status dot, and — when actively scanning — an embedded progress bar.</p>
 *
 * <p>A 1-second {@link Timer} triggers {@link #refresh()} to repaint live
 * progress from background indexing threads.</p>
 */
public class LuceneSidebarPanel extends JPanel {

    private static final String GLOBAL_LABEL = "Global / System Controls";

    /** Sentinel IndexRowItem for the Global row. */
    private static final IndexRowItem GLOBAL_ROW = new IndexRowItem(null, null);

    private final List<IndexRowItem> rows = new ArrayList<IndexRowItem>();
    private final SidebarTableModel tableModel = new SidebarTableModel();
    private final JTable table = new JTable(tableModel);
    private Consumer<IndexRowItem> selectionCallback;

    public LuceneSidebarPanel() {
        super(new BorderLayout());

        table.setRowHeight(54);
        table.setShowGrid(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setVisible(false);
        table.setDefaultRenderer(Object.class, new SidebarCellRenderer());
        table.setFillsViewportHeight(true);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && selectionCallback != null) {
                    int row = table.getSelectedRow();
                    if (row >= 0 && row < rows.size()) {
                        selectionCallback.accept(rows.get(row));
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0xD0, 0xD0, 0xD0)));
        add(scroll, BorderLayout.CENTER);

        new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        }).start();
    }

    public void setSelectionCallback(Consumer<IndexRowItem> callback) {
        this.selectionCallback = callback;
    }

    /**
     * Reload sandbox rows from {@link LuceneService} and repaint.
     */
    public void refresh() {
        rows.clear();
        rows.add(GLOBAL_ROW);
        if (LuceneService.get().isInitialized()) {
            rows.addAll(LuceneService.get().buildRowItems());
        }
        tableModel.fireTableDataChanged();
    }

    // ── inner model ──────────────────────────────────────────────────────────

    private class SidebarTableModel extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Object getValueAt(int row, int col) {
            return rows.get(row);
        }
    }

    // ── cell renderer ─────────────────────────────────────────────────────────

    private static class SidebarCellRenderer extends DefaultTableCellRenderer {

        // Reusable component: a panel composed of a status dot, text labels, and a progress bar.
        private final CellComponent cell = new CellComponent();

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            IndexRowItem item = (IndexRowItem) value;
            Color bg = isSelected ? table.getSelectionBackground() : table.getBackground();
            Color fg = isSelected ? table.getSelectionForeground() : table.getForeground();
            cell.update(item, bg, fg);
            return cell;
        }
    }

    private static class CellComponent extends JPanel {

        private final JLabel dotLabel    = new JLabel("●");
        private final JLabel nameLabel   = new JLabel();
        private final JLabel statsLabel  = new JLabel();
        private final JProgressBar bar   = new JProgressBar(0, 100);

        CellComponent() {
            setLayout(new BorderLayout(6, 0));
            setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

            dotLabel.setFont(dotLabel.getFont().deriveFont(10f));
            dotLabel.setPreferredSize(new Dimension(14, 14));
            add(dotLabel, BorderLayout.WEST);

            JPanel center = new JPanel(new GridLayout(3, 1, 0, 1));
            center.setOpaque(false);
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));
            statsLabel.setFont(statsLabel.getFont().deriveFont(10f));
            bar.setPreferredSize(new Dimension(0, 6));
            bar.setBorderPainted(false);
            bar.setStringPainted(false);
            center.add(nameLabel);
            center.add(statsLabel);
            center.add(bar);
            add(center, BorderLayout.CENTER);
        }

        void update(IndexRowItem item, Color bg, Color fg) {
            setBackground(bg);
            nameLabel.setForeground(fg);
            statsLabel.setForeground(fg);
            dotLabel.setForeground(fg);

            if (item == GLOBAL_ROW) {
                nameLabel.setText(GLOBAL_LABEL);
                statsLabel.setText(LuceneService.get().isInitialized()
                    ? "Sandboxes: " + LuceneService.get().buildRowItems().size()
                    : "Not initialized");
                dotLabel.setForeground(new Color(0x00, 0x88, 0x00));
                bar.setVisible(false);
                return;
            }

            nameLabel.setText(item.getConfig().getDisplayName());
            int docs = item.getRuntimeState().getDocumentCount();
            IndexStatus status = item.getRuntimeState().getStatus();

            switch (status) {
                case SCANNING:
                    dotLabel.setForeground(new Color(0x00, 0x70, 0xCC));
                    statsLabel.setText("Scanning... Docs: " + docs);
                    bar.setValue(item.getRuntimeState().getProgress());
                    bar.setVisible(true);
                    break;
                case WATCHING:
                    dotLabel.setForeground(new Color(0x00, 0x88, 0x00));
                    statsLabel.setText("Watching  |  Docs: " + docs);
                    bar.setVisible(false);
                    break;
                case ERROR:
                    dotLabel.setForeground(new Color(0xCC, 0x88, 0x00));
                    String err = item.getRuntimeState().getErrorMessage();
                    statsLabel.setText(err != null ? err : "Error");
                    bar.setVisible(false);
                    break;
                default:
                    dotLabel.setForeground(new Color(0x99, 0x99, 0x99));
                    statsLabel.setText("Idle  |  Docs: " + docs);
                    bar.setVisible(false);
            }
        }
    }
}
