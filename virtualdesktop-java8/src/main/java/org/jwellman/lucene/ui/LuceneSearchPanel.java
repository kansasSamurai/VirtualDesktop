package org.jwellman.lucene.ui;

import org.jwellman.lucene.engine.LuceneService;
import org.jwellman.lucene.engine.SearchResult;
import org.jwellman.lucene.model.IndexRowItem;
import org.jwellman.swing.grid.ColumnDef;
import org.jwellman.swing.grid.DefaultGridModel;
import org.jwellman.swing.grid.GridRow;
import org.jwellman.swing.grid.SmartGrid;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Phase 4 search panel: debounced omni-search box driving async Lucene queries
 * with results displayed in a {@link SmartGrid}.
 *
 * <p>Typing is debounced at 150 ms; queries fire only when the input is at least
 * three characters. Results are fetched on a background thread and hydrated into
 * the model on the EDT via {@link SwingUtilities#invokeLater}.</p>
 */
@SuppressWarnings("serial")
public class LuceneSearchPanel extends JPanel {

    private static final int DEBOUNCE_MS  = 150;
    private static final int MIN_CHARS    = 3;
    private static final int MAX_RESULTS  = 200;
    private static final String ALL_SANDBOXES = "All Sandboxes";

    private final JComboBox<String> sandboxCombo;
    private final JTextField searchBox;
    private final JLabel statusLabel;
    private final DefaultGridModel model;
    private final Timer debounceTimer;

    public LuceneSearchPanel() {
        super(new BorderLayout(0, 4));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        // --- Toolbar ---
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));

        sandboxCombo = buildSandboxCombo();
        toolbar.add(new JLabel("Sandbox:"));
        toolbar.add(sandboxCombo);

        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(new JLabel("Search:"));

        searchBox = new JTextField();
        searchBox.setPreferredSize(new Dimension(300, searchBox.getPreferredSize().height));
        toolbar.add(searchBox);

        statusLabel = new JLabel(" ");
        toolbar.add(statusLabel);

        add(toolbar, BorderLayout.NORTH);

        // --- SmartGrid ---
        model = new DefaultGridModel()
            .addColumn(new ColumnDef("title",    "Title",    200, true, true, null))
            .addColumn(new ColumnDef("path",     "Path",     350, true, true, null))
            .addColumn(new ColumnDef("modified", "Modified", 120, true, true, "date"));

        SmartGrid grid = new SmartGrid(model, true);
        grid.setToolbarVisible(false);
        grid.setCheckboxColumnVisible(false);
        grid.setRowNumbersVisible(false);
        grid.registerCellRenderer("date", (col, value, row, existing) -> {
            JLabel lbl = (existing instanceof JLabel) ? (JLabel) existing : new JLabel();
            if (value instanceof Long) {
                long millis = (Long) value;
                if (millis > 0) {
                    lbl.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(millis)));
                } else {
                    lbl.setText("");
                }
            } else {
                lbl.setText("");
            }
            lbl.setHorizontalAlignment(JLabel.CENTER);
            return lbl;
        });

        add(grid, BorderLayout.CENTER);

        // --- Debounce timer ---
        debounceTimer = new Timer(DEBOUNCE_MS, e -> triggerSearch());
        debounceTimer.setRepeats(false);

        searchBox.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                debounceTimer.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                debounceTimer.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                debounceTimer.restart();
            }
        });
    }

    private JComboBox<String> buildSandboxCombo() {
        DefaultComboBoxModel<String> comboModel = new DefaultComboBoxModel<String>();
        comboModel.addElement(ALL_SANDBOXES);
        if (LuceneService.get().isInitialized()) {
            for (IndexRowItem item : LuceneService.get().buildRowItems()) {
                if (item.getConfig() != null) {
                    comboModel.addElement(item.getConfig().getDisplayName());
                }
            }
        }
        return new JComboBox<String>(comboModel);
    }

    private void triggerSearch() {
        String text = searchBox.getText().trim();
        if (text.length() < MIN_CHARS) {
            clearResults();
            return;
        }
        final String queryString = text;
        final String sandboxId = resolveSelectedSandboxId();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<SearchResult> results =
                    LuceneService.get().search(sandboxId, queryString, MAX_RESULTS);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        populateGrid(results);
                    }
                });
            }
        }).start();
    }

    private String resolveSelectedSandboxId() {
        Object selected = sandboxCombo.getSelectedItem();
        if (selected == null || ALL_SANDBOXES.equals(selected)) {
            return null;
        }
        String displayName = selected.toString();
        if (LuceneService.get().isInitialized()) {
            for (IndexRowItem item : LuceneService.get().buildRowItems()) {
                if (item.getConfig() != null &&
                        displayName.equals(item.getConfig().getDisplayName())) {
                    return item.getConfig().getId();
                }
            }
        }
        return null;
    }

    private void populateGrid(List<SearchResult> results) {
        model.clearRows();
        for (SearchResult r : results) {
            model.addRow(new GridRow()
                .put("title",    r.getTitle() != null ? r.getTitle() : "")
                .put("path",     r.getPath() != null  ? r.getPath()  : "")
                .put("modified", r.getLastModifiedMillis()));
        }
        model.notifyDataChanged();
        int count = results.size();
        statusLabel.setText(count == 0 ? "No results" : count + " result" + (count == 1 ? "" : "s"));
    }

    private void clearResults() {
        model.clearRows();
        model.notifyDataChanged();
        statusLabel.setText(" ");
    }
}
