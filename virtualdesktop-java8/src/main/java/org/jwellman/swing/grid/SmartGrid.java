package org.jwellman.swing.grid;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JComponent;
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
 *     columnHeaderView → header JPanel  (GridBagLayout, proportional widths)
 *     viewport         → VirtualCanvas  (null layout, virtual height)
 *                            slot[0..N]  (StandardRowPanel, absolute bounds)
 *   SOUTH panel (optional):
 *     footer JPanel    (GridBagLayout, same proportional widths)
 *     PaginationBar
 *
 * Only ~N+2 row components exist at any time regardless of row count.
 * On scroll, slots are repositioned and rebound — not recreated.
 */
public class SmartGrid extends JPanel implements GridModelListener {

    private static final Color HEADER_BG = new Color(0x3C4B64);
    private static final Color FOOTER_BG = new Color(0xECEFF4);

    private final GridModel model;
    private final DefaultListSelectionModel selectionModel =
            new DefaultListSelectionModel();
    private int rowHeight = 32;

    // Renderer delegates — replaceable at runtime
    private HeaderCellRenderer headerRenderer = new DefaultHeaderCellRenderer();
    private FooterCellRenderer footerRenderer = null; // null = no footer

    // Pagination state
    private int pageSize    = 0; // 0 = disabled (show all rows)
    private int currentPage = 0;

    // Swing components
    private JScrollPane  scrollPane;
    private VirtualCanvas canvas;
    private JComponent[] slots;
    private ComponentPool pool;
    private JPanel        southPanel    = null; // footer + pagination bar container
    private JPanel        footerPanel   = null;
    private PaginationBar paginationBar = null;

    public SmartGrid(GridModel model) {
        this.model = model;
        this.model.addGridModelListener(this);

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
        scrollPane.setColumnHeaderView(buildHeader(cols));

        scrollPane.getViewport().addChangeListener(new ChangeListener() {
            @Override public void stateChanged(ChangeEvent e) { refresh(); }
        });

        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
                    && isShowing()) {
                SwingUtilities.invokeLater(SmartGrid.this::refresh);
            }
        });

        add(scrollPane, BorderLayout.CENTER);
    }

    // -------------------------------------------------------------------------
    // Public API — basic
    // -------------------------------------------------------------------------

    public GridModel getModel() { return model; }

    public int getRowHeight() { return rowHeight; }

    public void setRowHeight(int rowHeight) {
        this.rowHeight = rowHeight;
        canvas.revalidate();
        refresh();
    }

    // -------------------------------------------------------------------------
    // Public API — selection
    // -------------------------------------------------------------------------

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
    // Public API — header / footer renderers
    // -------------------------------------------------------------------------

    public void setHeaderRenderer(HeaderCellRenderer r) {
        this.headerRenderer = (r != null) ? r : new DefaultHeaderCellRenderer();
        scrollPane.setColumnHeaderView(buildHeader(model.getColumns()));
    }

    public void setFooterRenderer(FooterCellRenderer r) {
        this.footerRenderer = r;
        rebuildSouthPanel();
    }

    // -------------------------------------------------------------------------
    // Public API — pagination
    // -------------------------------------------------------------------------

    public int getPageSize()    { return pageSize; }
    public int getCurrentPage() { return currentPage; }

    public int totalPages() {
        if (pageSize <= 0) return 1;
        int rows = model.getRowCount();
        return (rows == 0) ? 1 : (int) Math.ceil((double) rows / pageSize);
    }

    /** Sets rows per page. Pass 0 to disable pagination (show all rows). */
    public void setPageSize(int size) {
        this.pageSize    = Math.max(0, size);
        this.currentPage = 0;
        rebuildSouthPanel();
        canvas.revalidate();
        refresh();
    }

    public void goToPage(int page) {
        int total = totalPages();
        this.currentPage = Math.max(0, Math.min(page, total - 1));
        scrollPane.getViewport().setViewPosition(new Point(0, 0));
        canvas.revalidate();
        refresh();
        refreshFooter();
        if (paginationBar != null) paginationBar.update(currentPage, totalPages());
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
            refreshFooter();
            if (paginationBar != null) paginationBar.update(currentPage, totalPages());
        });
    }

    // -------------------------------------------------------------------------
    // Internal — refresh (recycle + rebind visible slots)
    // -------------------------------------------------------------------------

    private void refresh() {
        if (!isShowing()) return;

        int vpHeight = scrollPane.getViewport().getHeight();
        int vpWidth  = scrollPane.getViewport().getWidth();
        int scrollY  = scrollPane.getViewport().getViewPosition().y;

        if (vpHeight <= 0 || vpWidth <= 0) return;

        int pageOffset    = (pageSize > 0) ? currentPage * pageSize : 0;
        int effectiveRows = (pageSize > 0)
            ? Math.min(pageSize, model.getRowCount() - pageOffset)
            : model.getRowCount();
        effectiveRows = Math.max(0, effectiveRows);

        int firstRow     = scrollY / rowHeight;
        int visibleCount = (vpHeight / rowHeight) + 2;

        if (slots == null || slots.length != visibleCount) {
            reallocateSlots(visibleCount);
        }

        for (int i = 0; i < slots.length; i++) {
            int rowIdx   = firstRow + i;          // position within current page
            int modelIdx = pageOffset + rowIdx;   // position in the full model
            if (rowIdx < effectiveRows && modelIdx < model.getRowCount()) {
                GridRow row = model.getRow(modelIdx);
                // Use model-absolute index so selection persists across page navigation.
                ((Recyclable) slots[i]).bind(row, modelIdx);
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

    // -------------------------------------------------------------------------
    // Internal — header / footer builders (GridBagLayout, proportional widths)
    // -------------------------------------------------------------------------

    private JPanel buildHeader(List<ColumnDef> cols) {
        JPanel header = new JPanel(new GridBagLayout());
        header.setBackground(HEADER_BG);
        header.setPreferredSize(new Dimension(0, rowHeight));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0; gbc.weighty = 1.0;
        int total = columnWidthTotal(cols);
        for (int i = 0; i < cols.size(); i++) {
            gbc.gridx   = i;
            gbc.weightx = (double) cols.get(i).getPreferredWidth() / total;
            header.add(headerRenderer.render(cols.get(i)), gbc);
        }
        return header;
    }

    private JPanel buildFooter(List<ColumnDef> cols) {
        JPanel footer = new JPanel(new GridBagLayout());
        footer.setBackground(FOOTER_BG);
        footer.setPreferredSize(new Dimension(0, rowHeight));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0; gbc.weighty = 1.0;
        int total = columnWidthTotal(cols);
        List<GridRow> pageRows = getPageRows();
        for (int i = 0; i < cols.size(); i++) {
            gbc.gridx   = i;
            gbc.weightx = (double) cols.get(i).getPreferredWidth() / total;
            footer.add(footerRenderer.render(cols.get(i), pageRows, model), gbc);
        }
        return footer;
    }

    private static int columnWidthTotal(List<ColumnDef> cols) {
        int total = 0;
        for (ColumnDef col : cols) total += col.getPreferredWidth();
        return total == 0 ? 1 : total;
    }

    /** Returns the rows for the current page (or all rows when pagination is off). */
    List<GridRow> getPageRows() {
        int offset = (pageSize > 0) ? currentPage * pageSize : 0;
        int count  = (pageSize > 0)
            ? Math.min(pageSize, model.getRowCount() - offset)
            : model.getRowCount();
        count = Math.max(0, count);
        List<GridRow> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(model.getRow(offset + i));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Internal — south panel (footer + pagination bar)
    // -------------------------------------------------------------------------

    private void rebuildSouthPanel() {
        // Remove any existing south component
        BorderLayout bl = (BorderLayout) getLayout();
        Component old = bl.getLayoutComponent(BorderLayout.SOUTH);
        if (old != null) remove(old);

        southPanel    = null;
        footerPanel   = null;
        paginationBar = null;

        boolean hasFooter = footerRenderer != null;
        boolean hasPaging = pageSize > 0;

        if (!hasFooter && !hasPaging) { revalidate(); return; }

        southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));

        if (hasFooter) {
            footerPanel = buildFooter(model.getColumns());
            southPanel.add(footerPanel);
        }
        if (hasPaging) {
            paginationBar = new PaginationBar(this);
            southPanel.add(paginationBar);
        }
        add(southPanel, BorderLayout.SOUTH);
        revalidate();
    }

    /** Replaces the footer panel in-place without touching the pagination bar. */
    private void refreshFooter() {
        if (footerRenderer == null || southPanel == null || footerPanel == null) return;
        southPanel.remove(footerPanel);
        footerPanel = buildFooter(model.getColumns());
        southPanel.add(footerPanel, 0); // index 0 = above pagination bar
        southPanel.revalidate();
        southPanel.repaint();
    }

    // -------------------------------------------------------------------------
    // Inner class: VirtualCanvas
    // -------------------------------------------------------------------------

    class VirtualCanvas extends JPanel implements Scrollable {

        VirtualCanvas() {
            setLayout(null);
        }

        @Override
        public void addNotify() {
            super.addNotify();
            SwingUtilities.invokeLater(SmartGrid.this::refresh);
        }

        @Override
        public Dimension getPreferredSize() {
            int w = scrollPane.getViewport().getWidth();
            if (w == 0) w = 400;
            int effectiveRows = (pageSize > 0)
                ? Math.min(pageSize, model.getRowCount() - currentPage * pageSize)
                : model.getRowCount();
            return new Dimension(w, Math.max(0, effectiveRows) * rowHeight);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

        @Override
        public int getScrollableUnitIncrement(Rectangle v, int orientation, int dir) {
            return rowHeight;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle v, int orientation, int dir) {
            return v.height;
        }

        @Override public boolean getScrollableTracksViewportWidth()  { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
