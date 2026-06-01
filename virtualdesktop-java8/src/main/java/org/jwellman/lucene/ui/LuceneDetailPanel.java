package org.jwellman.lucene.ui;

import org.jwellman.lucene.engine.IndexSandboxManager;
import org.jwellman.lucene.engine.LuceneService;
import org.jwellman.lucene.model.AnalyzerType;
import org.jwellman.lucene.model.DirectorySandboxConfig;
import org.jwellman.lucene.model.IndexRowItem;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Right-hand panel in the Lucene Management UI.
 *
 * <p>Vertically split: upper half shows the selected sandbox's config form;
 * lower half shows a live activity log fed from background indexing threads.</p>
 *
 * <p>Call {@link #showSandbox(IndexRowItem)} or {@link #showGlobal()} to
 * hydrate the panel when the sidebar selection changes.</p>
 */
public class LuceneDetailPanel extends JPanel {

    private static final int LOG_CAP = 500;

    // ── config form fields ───────────────────────────────────────────────────
    private final JLabel titleLabel       = new JLabel("Global / System Controls");
    private final JTextField sourceField  = new JTextField();
    private final JTextField indexField   = new JTextField();
    private final JTextField filterField  = new JTextField();
    private final JComboBox<AnalyzerType> analyzerCombo = new JComboBox<AnalyzerType>(AnalyzerType.values());
    private final JButton reindexButton   = new JButton("Reindex Directory");
    private final JButton commitButton    = new JButton("Commit Active Transactions");
    private final JLabel docsLabel        = new JLabel("Documents: 0");

    // ── log area ─────────────────────────────────────────────────────────────
    private final List<LogEntry> logEntries = new ArrayList<LogEntry>();
    private final LogTableModel logModel    = new LogTableModel();
    private final JTable logTable           = new JTable(logModel);

    // Log entries arrive here from background threads; Timer drains on EDT
    private final LinkedBlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<LogEntry>();

    private IndexRowItem currentItem = null;

    public LuceneDetailPanel() {
        super(new BorderLayout());

        JPanel configPanel = buildConfigPanel();
        JPanel logPanel    = buildLogPanel();

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, configPanel, logPanel);
        split.setDividerLocation(220);
        split.setResizeWeight(0.4);
        add(split, BorderLayout.CENTER);

        // Drain log queue every 200 ms on the EDT
        new Timer(200, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                drainLogQueue();
            }
        }).start();
    }

    // ── public API ───────────────────────────────────────────────────────────

    /** Hydrate for a specific sandbox row. */
    public void showSandbox(IndexRowItem item) {
        this.currentItem = item;
        DirectorySandboxConfig cfg = item.getConfig();

        titleLabel.setText(cfg.getDisplayName());
        sourceField.setText(cfg.getSourcePath() != null ? cfg.getSourcePath() : "");
        LuceneService svc = LuceneService.get();
        IndexSandboxManager mgr = svc.getManager(cfg.getId());
        indexField.setText(mgr != null ? mgr.getIndexPath().toString() : "");
        filterField.setText(cfg.getFileInclusionFilter() != null ? cfg.getFileInclusionFilter() : "");
        analyzerCombo.setSelectedItem(cfg.getAnalyzerType());
        docsLabel.setText("Documents: " + item.getRuntimeState().getDocumentCount());

        setConfigFieldsVisible(true);
        setGlobalControlsVisible(false);
    }

    /** Hydrate for the Global row. */
    public void showGlobal() {
        this.currentItem = null;
        titleLabel.setText("Global / System Controls");
        LuceneService svc = LuceneService.get();
        if (svc.isInitialized()) {
            docsLabel.setText("Total Documents: " + svc.getTotalDocumentCount()
                + "  |  Active Indexers: " + svc.getActiveSandboxCount());
        } else {
            docsLabel.setText("Service not yet initialized");
        }
        setConfigFieldsVisible(false);
        setGlobalControlsVisible(true);
    }

    /**
     * Append a log entry from any thread. Safe to call from background threads.
     */
    public void log(LogEntry entry) {
        logQueue.offer(entry);
    }

    // ── construction helpers ─────────────────────────────────────────────────

    private JPanel buildConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Configuration"));

        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(3, 6, 3, 4);
        lc.gridx = 0;

        GridBagConstraints fc = new GridBagConstraints();
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1.0;
        fc.insets = new Insets(3, 0, 3, 6);
        fc.gridx = 1;

        // Title
        GridBagConstraints tc = new GridBagConstraints();
        tc.gridx = 0; tc.gridy = 0; tc.gridwidth = 2;
        tc.anchor = GridBagConstraints.WEST;
        tc.insets = new Insets(4, 6, 4, 6);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
        panel.add(titleLabel, tc);

        // Docs label
        GridBagConstraints dc = new GridBagConstraints();
        dc.gridx = 0; dc.gridy = 1; dc.gridwidth = 2;
        dc.anchor = GridBagConstraints.WEST;
        dc.insets = new Insets(0, 6, 6, 6);
        panel.add(docsLabel, dc);

        // Source path
        lc.gridy = 2; fc.gridy = 2;
        panel.add(new JLabel("Source Path:"), lc);
        sourceField.setEditable(false);
        sourceField.setBackground(UIManager.getColor("TextField.inactiveBackground"));
        panel.add(sourceField, fc);

        // Index path
        lc.gridy = 3; fc.gridy = 3;
        panel.add(new JLabel("Index Path:"), lc);
        indexField.setEditable(false);
        indexField.setBackground(UIManager.getColor("TextField.inactiveBackground"));
        panel.add(indexField, fc);

        // File filter
        lc.gridy = 4; fc.gridy = 4;
        panel.add(new JLabel("File Filter:"), lc);
        panel.add(filterField, fc);

        // Analyzer
        lc.gridy = 5; fc.gridy = 5;
        panel.add(new JLabel("Analyzer:"), lc);
        panel.add(analyzerCombo, fc);

        // Buttons
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridx = 0; bc.gridy = 6; bc.gridwidth = 2;
        bc.anchor = GridBagConstraints.WEST;
        bc.insets = new Insets(8, 6, 4, 6);
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttonRow.add(reindexButton);
        buttonRow.add(commitButton);
        panel.add(buttonRow, bc);

        // Spacer
        GridBagConstraints sc = new GridBagConstraints();
        sc.gridx = 0; sc.gridy = 7; sc.weighty = 1.0;
        panel.add(new JLabel(), sc);

        // Wire actions
        reindexButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onReindex();
            }
        });
        commitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCommit();
            }
        });

        return panel;
    }

    private JPanel buildLogPanel() {
        logTable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        logTable.setRowHeight(16);
        logTable.setShowGrid(false);
        logTable.getTableHeader().setVisible(false);
        logTable.setDefaultRenderer(Object.class, new LogCellRenderer());

        JScrollPane scroll = new JScrollPane(logTable);
        scroll.setBorder(BorderFactory.createTitledBorder("Activity Log"));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ── button actions ───────────────────────────────────────────────────────

    private void onReindex() {
        if (currentItem == null) {
            return;
        }
        final String id = currentItem.getConfig().getId();
        log(new LogEntry(LogEntry.Level.INFO, "Purging index: " + id));
        new Thread(new Runnable() {
            @Override
            public void run() {
                IndexSandboxManager mgr = LuceneService.get().getManager(id);
                if (mgr != null) {
                    mgr.purge();
                    log(new LogEntry(LogEntry.Level.SUCCESS, "Purge complete: " + id));
                }
            }
        }).start();
    }

    private void onCommit() {
        if (currentItem == null) {
            return;
        }
        final String id = currentItem.getConfig().getId();
        new Thread(new Runnable() {
            @Override
            public void run() {
                IndexSandboxManager mgr = LuceneService.get().getManager(id);
                if (mgr != null) {
                    try {
                        mgr.commit();
                        log(new LogEntry(LogEntry.Level.SUCCESS, "Commit complete: " + id));
                    } catch (IOException ex) {
                        log(new LogEntry(LogEntry.Level.ERROR, "Commit failed: " + ex.getMessage()));
                    }
                }
            }
        }).start();
    }

    // ── visibility helpers ───────────────────────────────────────────────────

    private void setConfigFieldsVisible(boolean visible) {
        sourceField.setVisible(visible);
        indexField.setVisible(visible);
        filterField.setVisible(visible);
        analyzerCombo.setVisible(visible);
        reindexButton.setVisible(visible);
        commitButton.setVisible(visible);
    }

    private void setGlobalControlsVisible(boolean visible) {
        // Placeholder for future global-only buttons (Reindex All, Clean Locks)
    }

    // ── log drain ────────────────────────────────────────────────────────────

    private void drainLogQueue() {
        List<LogEntry> batch = new ArrayList<LogEntry>();
        logQueue.drainTo(batch);
        if (batch.isEmpty()) {
            return;
        }
        for (LogEntry entry : batch) {
            if (logEntries.size() >= LOG_CAP) {
                logEntries.remove(0);
            }
            logEntries.add(entry);
        }
        logModel.fireTableDataChanged();
        // Auto-scroll to last row
        int last = logEntries.size() - 1;
        if (last >= 0) {
            logTable.scrollRectToVisible(logTable.getCellRect(last, 0, true));
        }
    }

    // ── inner models/renderers ───────────────────────────────────────────────

    private class LogTableModel extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return logEntries.size();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Object getValueAt(int row, int col) {
            return logEntries.get(row);
        }
    }

    private static class LogCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof LogEntry) {
                LogEntry entry = (LogEntry) value;
                setText(entry.toString());
                if (!isSelected) {
                    switch (entry.getLevel()) {
                        case ERROR:
                            setForeground(new Color(0xCC, 0x00, 0x00));
                            break;
                        case SUCCESS:
                            setForeground(new Color(0x00, 0x88, 0x00));
                            break;
                        default:
                            setForeground(table.getForeground());
                    }
                }
            }
            return this;
        }
    }
}
