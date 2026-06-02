package org.jwellman.lucene.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.jwellman.lucene.engine.IndexSandboxManager;
import org.jwellman.lucene.engine.LuceneConfigLoader;
import org.jwellman.lucene.engine.LuceneService;
import org.jwellman.lucene.model.AnalyzerType;
import org.jwellman.lucene.model.DirectorySandboxConfig;
import org.jwellman.lucene.model.IndexRowItem;
import org.jwellman.lucene.model.LogEntry;

/**
 * Right-hand panel in the Lucene Management UI.
 *
 * <p>Vertically split: upper half shows a configuration form whose content
 * switches between a sandbox-specific card and a global-info card via
 * {@link CardLayout}; lower half shows a live activity log.</p>
 *
 * <p>Call {@link #showSandbox(IndexRowItem)} or {@link #showGlobal()} to
 * hydrate the panel when the sidebar selection changes.</p>
 */
@SuppressWarnings("serial")
public class LuceneDetailPanel extends JPanel {

    private static final int LOG_CAP = 500;

    private static final String CARD_SANDBOX = "sandbox";
    private static final String CARD_GLOBAL  = "global";

    // ── shared header fields (visible in both cards) ─────────────────────────
    private final JLabel titleLabel = new JLabel("Global / System Controls");
    private final JLabel docsLabel  = new JLabel("Documents: 0");

    // ── sandbox card fields ───────────────────────────────────────────────────
    private final JTextField sourceField  = new JTextField();
    private final JTextField indexField   = new JTextField();
    private final JTextField filterField     = new JTextField();
    private final JTextField exclusionsField = new JTextField();
    private final JComboBox<AnalyzerType> analyzerCombo = new JComboBox<AnalyzerType>(AnalyzerType.values());
    private final JButton reindexButton = new JButton("Reindex Directory");
    private final JButton saveButton    = new JButton("Save Changes");

    // ── global card fields ────────────────────────────────────────────────────
    private final JTextField configPathField  = new JTextField();
    private final JTextField baseIndexField   = new JTextField();
    private final JLabel     threadsValueLabel = new JLabel();

    // ── card container ────────────────────────────────────────────────────────
    private final JPanel contentCards = new JPanel(new CardLayout());

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

        final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, configPanel, logPanel);
        split.setResizeWeight(0.5);
        split.addHierarchyListener(new java.awt.event.HierarchyListener() {
            @Override
            public void hierarchyChanged(java.awt.event.HierarchyEvent e) {
                if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0
                        && split.isShowing()) {
                    split.setDividerLocation(0.5);
                    split.removeHierarchyListener(this);
                }
            }
        });
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
        docsLabel.setText("Documents: " + item.getRuntimeState().getDocumentCount());
        sourceField.setText(cfg.getSourcePath() != null ? cfg.getSourcePath() : "");
        LuceneService svc = LuceneService.get();
        IndexSandboxManager mgr = svc.getManager(cfg.getId());
        indexField.setText(mgr != null ? mgr.getIndexPath().toString() : "");
        filterField.setText(cfg.getFileInclusionFilter() != null ? cfg.getFileInclusionFilter() : "");
        exclusionsField.setText(cfg.getDirectoryExclusions() != null
                ? String.join(",", cfg.getDirectoryExclusions()) : "");
        analyzerCombo.setSelectedItem(cfg.getAnalyzerType());

        ((CardLayout) contentCards.getLayout()).show(contentCards, CARD_SANDBOX);
    }

    /** Hydrate for the Global row. */
    public void showGlobal() {
        this.currentItem = null;
        titleLabel.setText("Global / System Controls");

        LuceneService svc = LuceneService.get();
        if (svc.isInitialized()) {
            docsLabel.setText("Total Documents: " + svc.getTotalDocumentCount()
                + "  |  Active Indexers: " + svc.getActiveSandboxCount());
            configPathField.setText(new File(LuceneConfigLoader.CONFIG_PATH).getAbsolutePath());
            baseIndexField.setText(new File(svc.getGlobalConfig().getBaseIndexDirectory()).getAbsolutePath());
            threadsValueLabel.setText(String.valueOf(svc.getGlobalConfig().getMaxBackgroundThreads()));
        } else {
            docsLabel.setText("Service not yet initialized");
            configPathField.setText("");
            baseIndexField.setText("");
            threadsValueLabel.setText("");
        }

        ((CardLayout) contentCards.getLayout()).show(contentCards, CARD_GLOBAL);
    }

    /**
     * Append a log entry from any thread. Safe to call from background threads.
     */
    public void log(LogEntry entry) {
        logQueue.offer(entry);
    }

    // ── construction helpers ─────────────────────────────────────────────────

    private JPanel buildConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Configuration"));

        // Shared header: title + docs summary
        JPanel header = new JPanel(new GridBagLayout());
        GridBagConstraints tc = new GridBagConstraints();
        tc.gridx = 0; tc.gridy = 0; tc.gridwidth = 2;
        tc.anchor = GridBagConstraints.WEST;
        tc.insets = new Insets(4, 6, 2, 6);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
        header.add(titleLabel, tc);

        GridBagConstraints dc = new GridBagConstraints();
        dc.gridx = 0; dc.gridy = 1; dc.gridwidth = 2;
        dc.anchor = GridBagConstraints.WEST;
        dc.insets = new Insets(0, 6, 6, 6);
        header.add(docsLabel, dc);

        panel.add(header, BorderLayout.NORTH);

        // Switching content area
        contentCards.add(buildSandboxCard(), CARD_SANDBOX);
        contentCards.add(buildGlobalCard(), CARD_GLOBAL);
        panel.add(contentCards, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildSandboxCard() {
        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(3, 6, 3, 4);
        lc.gridx = 0;

        GridBagConstraints fc = new GridBagConstraints();
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1.0;
        fc.insets = new Insets(3, 0, 3, 6);
        fc.gridx = 1;

        // Source path
        lc.gridy = 0; fc.gridy = 0;
        panel.add(new JLabel("Source Path:"), lc);
        sourceField.setEditable(false);
        sourceField.setBackground(UIManager.getColor("TextField.inactiveBackground"));
        panel.add(sourceField, fc);

        // Index path
        lc.gridy = 1; fc.gridy = 1;
        panel.add(new JLabel("Index Path:"), lc);
        indexField.setEditable(false);
        indexField.setBackground(UIManager.getColor("TextField.inactiveBackground"));
        panel.add(indexField, fc);

        // File filter
        lc.gridy = 2; fc.gridy = 2;
        panel.add(new JLabel("File Filter:"), lc);
        panel.add(filterField, fc);

        // Directory exclusions
        lc.gridy = 3; fc.gridy = 3;
        panel.add(new JLabel("Dir Exclusions:"), lc);
        panel.add(exclusionsField, fc);

        // Analyzer
        lc.gridy = 4; fc.gridy = 4;
        panel.add(new JLabel("Analyzer:"), lc);
        panel.add(analyzerCombo, fc);

        // Buttons
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridx = 0; bc.gridy = 5; bc.gridwidth = 2;
        bc.anchor = GridBagConstraints.WEST;
        bc.insets = new Insets(8, 6, 4, 6);
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttonRow.add(reindexButton);
        buttonRow.add(saveButton);
        panel.add(buttonRow, bc);

        // Spacer
        GridBagConstraints sc = new GridBagConstraints();
        sc.gridx = 0; sc.gridy = 6; sc.weighty = 1.0;
        panel.add(new JLabel(), sc);

        // Wire actions
        reindexButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onReindex();
            }
        });
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSaveChanges();
            }
        });

        return panel;
    }

    private JPanel buildGlobalCard() {
        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(3, 6, 3, 4);
        lc.gridx = 0;

        GridBagConstraints fc = new GridBagConstraints();
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1.0;
        fc.insets = new Insets(3, 0, 3, 6);
        fc.gridx = 1;

        // Config file
        lc.gridy = 0; fc.gridy = 0;
        panel.add(new JLabel("Config File:"), lc);
        configPathField.setEditable(false);
        configPathField.setBackground(UIManager.getColor("TextField.inactiveBackground"));
        panel.add(configPathField, fc);

        // Base index directory
        lc.gridy = 1; fc.gridy = 1;
        panel.add(new JLabel("Index Directory:"), lc);
        baseIndexField.setEditable(false);
        baseIndexField.setBackground(UIManager.getColor("TextField.inactiveBackground"));
        panel.add(baseIndexField, fc);

        // Thread pool size
        lc.gridy = 2; fc.gridy = 2;
        panel.add(new JLabel("Indexing Threads:"), lc);
        panel.add(threadsValueLabel, fc);

        // Spacer
        GridBagConstraints sc = new GridBagConstraints();
        sc.gridx = 0; sc.gridy = 3; sc.weighty = 1.0;
        panel.add(new JLabel(), sc);

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
                    LuceneService.get().startIndexing(id);
                    log(new LogEntry(LogEntry.Level.INFO, "Reindex started: " + id));
                }
            }
        }).start();
    }

    private void onSaveChanges() {
        if (currentItem == null) {
            return;
        }
        DirectorySandboxConfig cfg = currentItem.getConfig();
        cfg.setFileInclusionFilter(filterField.getText().trim());
        cfg.setDirectoryExclusions(parseExclusions(exclusionsField.getText()));
        cfg.setAnalyzerType((AnalyzerType) analyzerCombo.getSelectedItem());
        LuceneConfigLoader.save(LuceneService.get().getGlobalConfig());
        log(new LogEntry(LogEntry.Level.SUCCESS,
            "Config saved for: " + cfg.getDisplayName()
            + " (analyzer change takes effect on next app start)"));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private List<String> parseExclusions(String text) {
        List<String> result = new ArrayList<String>();
        for (String part : text.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    // ── log drain ────────────────────────────────────────────────────────────

    private void drainLogQueue() {
        List<LogEntry> batch = new ArrayList<LogEntry>();
        logQueue.drainTo(batch);
        if (currentItem != null) {
            currentItem.getRuntimeState().drainLogEntries(batch);
        }
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
