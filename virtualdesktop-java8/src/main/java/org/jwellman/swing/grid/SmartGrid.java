package org.jwellman.swing.grid;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionListener;

/**
 * SmartGrid: a Swing table component where every row is a live JPanel.
 *
 * Architecture (the "stunt double" recycler):
 *   JScrollPane
 *     columnHeaderView → header JPanel (outside the scrolled area)
 *     viewport         → VirtualCanvas (null layout, virtual height)
 *                            slot[0..N]  (StandardRowPanel, absolute bounds)
 *
 * Only ~N+2 row components exist at any time regardless of row count.
 * On scroll, slots are repositioned and rebound — not recreated.
 */
public class SmartGrid extends JPanel implements GridModelListener {

    private static final Color HEADER_BG = new Color(0x3C4B64);
    private static final Color HEADER_FG = Color.WHITE;

    private final GridModel model;
    private final DefaultListSelectionModel selectionModel =
            new DefaultListSelectionModel();
    private int rowHeight = 32;
    private JScrollPane scrollPane;
    private VirtualCanvas canvas;
    private JComponent[] slots;
    private ComponentPool pool;

    public SmartGrid(GridModel model) {
        this.model = model;
        this.model.addGridModelListener(this);

        // Repaint visible rows whenever selection changes.
        selectionModel.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) refresh();
        });

        List<ColumnDef> cols = model.getColumns();
        final GridModel capturedModel = model;
        final DefaultListSelectionModel sm = selectionModel;
        this.pool = new ComponentPool(() -> new StandardRowPanel(cols,
            () -> {
                if (capturedModel instanceof DefaultGridModel) {
                    ((DefaultGridModel) capturedModel).notifyDataChanged();
                }
            },
            sm));

        setLayout(new BorderLayout());

        canvas = new VirtualCanvas();
        scrollPane = new JScrollPane(canvas,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // Header sits in the scroll pane's column header so its width always
        // matches the viewport (no scrollbar-width misalignment).
        scrollPane.setColumnHeaderView(buildHeader(cols));

        // ChangeListener fires on both scroll and viewport resize.
        scrollPane.getViewport().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                refresh();
            }
        });

        add(scrollPane, BorderLayout.CENTER);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public int getRowHeight() { return rowHeight; }

    public void setRowHeight(int rowHeight) {
        this.rowHeight = rowHeight;
        canvas.revalidate();
        refresh();
    }

    public ListSelectionModel getSelectionModel() { return selectionModel; }

    public void addListSelectionListener(ListSelectionListener l) {
        selectionModel.addListSelectionListener(l);
    }

    public void selectAll() {
        int n = model.getRowCount();
        if (n > 0) selectionModel.setSelectionInterval(0, n - 1);
    }

    public void clearSelection() {
        selectionModel.clearSelection();
    }

    // -------------------------------------------------------------------------
    // GridModelListener
    // -------------------------------------------------------------------------

    @Override
    public void rowsChanged(int firstRow, int lastRow) {
        SwingUtilities.invokeLater(this::refresh);
    }

    @Override
    public void modelReset() {
        SwingUtilities.invokeLater(() -> {
            canvas.revalidate();
            refresh();
        });
    }

    // -------------------------------------------------------------------------
    // Internal refresh logic
    // -------------------------------------------------------------------------

    private void refresh() {
        if (!isShowing()) return;

        int vpHeight = scrollPane.getViewport().getHeight();
        int vpWidth  = scrollPane.getViewport().getWidth();
        int scrollY  = scrollPane.getViewport().getViewPosition().y;

        if (vpHeight <= 0 || vpWidth <= 0) return;

        int firstRow     = scrollY / rowHeight;
        int visibleCount = (vpHeight / rowHeight) + 2; // +2 covers partial top/bottom rows

        if (slots == null || slots.length != visibleCount) {
            reallocateSlots(visibleCount);
        }

        for (int i = 0; i < slots.length; i++) {
            int rowIdx = firstRow + i;
            if (rowIdx < model.getRowCount()) {
                GridRow row = model.getRow(rowIdx);
                ((Recyclable) slots[i]).bind(row, rowIdx);
                slots[i].setBounds(0, rowIdx * rowHeight, vpWidth, rowHeight);
                slots[i].setVisible(true);
            } else {
                slots[i].setVisible(false);
            }
        }

        canvas.repaint();
    }

    private void reallocateSlots(int count) {
        if (slots != null) {
            for (JComponent slot : slots) {
                canvas.remove(slot);
                pool.release(slot);
            }
        }
        slots = new JComponent[count];
        for (int i = 0; i < count; i++) {
            slots[i] = pool.checkout();
            canvas.add(slots[i]);
        }
    }

    private JPanel buildHeader(List<ColumnDef> cols) {
        JPanel header = new JPanel(new GridLayout(1, cols.size()));
        header.setBackground(HEADER_BG);
        header.setPreferredSize(new Dimension(0, rowHeight));
        for (ColumnDef col : cols) {
            JLabel lbl = new JLabel(col.getHeader());
            lbl.setForeground(HEADER_FG);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
            lbl.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            header.add(lbl);
        }
        return header;
    }

    // -------------------------------------------------------------------------
    // Inner class: VirtualCanvas
    // -------------------------------------------------------------------------

    class VirtualCanvas extends JPanel implements Scrollable {

        VirtualCanvas() {
            setLayout(null); // absolute positioning for slot components
        }

        @Override
        public void addNotify() {
            super.addNotify();
            // Guarantee an initial population even if the ChangeListener
            // doesn't fire before the first paint.
            SwingUtilities.invokeLater(SmartGrid.this::refresh);
        }

        // Total virtual height drives the scrollbar size.
        @Override
        public Dimension getPreferredSize() {
            int w = scrollPane.getViewport().getWidth();
            if (w == 0) w = 400;
            return new Dimension(w, model.getRowCount() * rowHeight);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int dir) {
            return rowHeight;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int dir) {
            return visible.height;
        }

        // Force canvas to fill viewport width → no horizontal scrollbar.
        @Override
        public boolean getScrollableTracksViewportWidth() { return true; }

        @Override
        public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
