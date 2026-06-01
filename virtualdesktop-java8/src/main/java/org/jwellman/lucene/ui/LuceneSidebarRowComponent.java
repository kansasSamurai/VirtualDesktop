package org.jwellman.lucene.ui;

import org.jwellman.lucene.model.IndexRowItem;
import org.jwellman.lucene.model.SandboxRuntimeState.IndexStatus;
import org.jwellman.swing.grid.GridRow;
import org.jwellman.swing.grid.Recyclable;
import org.jwellman.swing.grid.Selectable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * SmartGrid row panel for one Lucene index sandbox entry.
 *
 * <p>Layout (left to right):</p>
 * <ul>
 *   <li>8px left inset</li>
 *   <li>12px colored status dot (painted, not a label)</li>
 *   <li>6px gap</li>
 *   <li>Text column: bold name label + stats label + optional progress bar</li>
 *   <li>6px right inset</li>
 * </ul>
 *
 * <p>Status dot colors: IDLE=gray, SCANNING=blue, WATCHING=green, ERROR=amber.</p>
 *
 * <p>Implements {@link Recyclable} and {@link Selectable} as required by SmartGrid's
 * typed-pool mechanism. Rows are tagged {@code fnd-type="lucene-sidebar-row"} in the
 * model so SmartGrid routes them to this component class.</p>
 */
public class LuceneSidebarRowComponent extends JPanel implements Recyclable, Selectable {

    private static final Color COLOR_IDLE        = new Color(0x99, 0x99, 0x99);
    private static final Color COLOR_SCANNING    = new Color(0x00, 0x70, 0xCC);
    private static final Color COLOR_WATCHING    = new Color(0x00, 0x88, 0x00);
    private static final Color COLOR_ERROR       = new Color(0xCC, 0x88, 0x00);
    private static final Color COLOR_SELECTED_BG = new Color(0x3C, 0x4B, 0x64, 60);
    private static final Color COLOR_INDICATOR   = new Color(0x3C, 0x4B, 0x64);
    private static final Color COLOR_PROGRESS_BG = new Color(0xDD, 0xDD, 0xDD);

    private static final Font FONT_NAME  = UIManager.getFont("Label.font").deriveFont(Font.BOLD,  12f);
    private static final Font FONT_STATS = UIManager.getFont("Label.font").deriveFont(Font.PLAIN, 10f);

    private static final Border    BORDER_ROW    = BorderFactory.createEmptyBorder(5, 22, 5, 6);
    private static final Dimension DIM_PROGRESS  = new Dimension(0, 5);

    private static final int DOT_DIAM = 8;
    private static final int DOT_X    = 8;

    private final JLabel       nameLabel  = new JLabel();
    private final JLabel       statsLabel = new JLabel();
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final ListSelectionModel selectionModel;

    private boolean      isSelected  = false;
    private Color        dotColor    = COLOR_IDLE;
    private MouseAdapter rowListener = null;

    public LuceneSidebarRowComponent(ListSelectionModel selectionModel) {
        this.selectionModel = selectionModel;

        setOpaque(false);
        setLayout(new BorderLayout(0, 0));
        setBorder(BORDER_ROW);

        nameLabel.setFont(FONT_NAME);
        statsLabel.setFont(FONT_STATS);

        progressBar.setBorderPainted(false);
        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(DIM_PROGRESS);
        progressBar.setBackground(COLOR_PROGRESS_BG);
        progressBar.setForeground(COLOR_SCANNING);

        JPanel textColumn = new JPanel();
        textColumn.setLayout(new BoxLayout(textColumn, BoxLayout.Y_AXIS));
        textColumn.setOpaque(false);
        textColumn.add(nameLabel);
        textColumn.add(statsLabel);
        textColumn.add(progressBar);

        add(textColumn, BorderLayout.CENTER);
    }

    // ── Recyclable ───────────────────────────────────────────────────────────

    @Override
    public void prepareForReuse() {
        if (rowListener != null) {
            removeMouseListener(rowListener);
            rowListener = null;
        }
        nameLabel.setText("");
        statsLabel.setText("");
        progressBar.setVisible(false);
        progressBar.setValue(0);
        dotColor   = COLOR_IDLE;
        isSelected = false;
    }

    @Override
    public void bind(GridRow row, int rowIndex) {
        if (rowListener != null) {
            removeMouseListener(rowListener);
            rowListener = null;
        }

        IndexRowItem item = (IndexRowItem) row.get("item");

        if (item == null || item.getConfig() == null) {
            // Global row
            nameLabel.setText("Global / System Controls");
            statsLabel.setText(buildGlobalStats());
            dotColor = COLOR_WATCHING;
            progressBar.setVisible(false);
        } else {
            IndexStatus status = item.getRuntimeState().getStatus();
            nameLabel.setText(item.getConfig().getDisplayName());
            statsLabel.setText(buildStats(item, status));
            dotColor = dotColorFor(status);
            if (status == IndexStatus.SCANNING) {
                progressBar.setValue(item.getRuntimeState().getProgress());
                progressBar.setVisible(true);
            } else {
                progressBar.setVisible(false);
            }
        }

        final int capturedIdx = rowIndex;
        rowListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                selectionModel.setSelectionInterval(capturedIdx, capturedIdx);
            }
        };
        addMouseListener(rowListener);

        repaint();
    }

    // ── Selectable ───────────────────────────────────────────────────────────

    @Override
    public void setSelected(boolean selected) {
        this.isSelected = selected;
        repaint();
    }

    // ── Painting ─────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (isSelected) {
            g2.setColor(COLOR_SELECTED_BG);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(COLOR_INDICATOR);
            g2.fillRect(0, 0, 2, getHeight());
        }

        // Status dot — centered vertically in the left margin (8px from left edge)
        g2.setColor(dotColor);
        int dotY = (getHeight() - DOT_DIAM) / 2;
        g2.fillOval(DOT_X, dotY, DOT_DIAM, DOT_DIAM);

        g2.dispose();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static Color dotColorFor(IndexStatus status) {
        switch (status) {
            case SCANNING: return COLOR_SCANNING;
            case WATCHING: return COLOR_WATCHING;
            case ERROR:    return COLOR_ERROR;
            default:       return COLOR_IDLE;
        }
    }

    private static String buildStats(IndexRowItem item, IndexStatus status) {
        int docs = item.getRuntimeState().getDocumentCount();
        switch (status) {
            case SCANNING: return "Scanning...  Docs: " + docs;
            case WATCHING: return "Watching  |  Docs: " + docs;
            case ERROR:
                String err = item.getRuntimeState().getErrorMessage();
                return err != null ? err : "Error";
            default: return "Idle  |  Docs: " + docs;
        }
    }

    private static String buildGlobalStats() {
        org.jwellman.lucene.engine.LuceneService svc =
            org.jwellman.lucene.engine.LuceneService.get();
        if (!svc.isInitialized()) {
            return "Not initialized";
        }
        return "Sandboxes: " + svc.buildRowItems().size()
            + "  |  Docs: " + svc.getTotalDocumentCount();
    }
}
