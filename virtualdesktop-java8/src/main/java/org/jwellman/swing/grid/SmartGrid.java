package org.jwellman.swing.grid;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
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
 * Column widths:
 *   SmartGrid owns a shared mutable {@code int[] columnWidths} array computed
 *   from the current viewport width and each ColumnDef's preferredWidth. Every
 *   StandardRowPanel in the pool holds a reference to the SAME array, so when
 *   the viewport is resized SmartGrid updates the array in-place and the next
 *   bind() call picks up the new widths — guaranteeing pixel-exact alignment
 *   across header, rows, and footer without independent layout managers.
 *
 * Layout:
 *   JScrollPane (vertical AS_NEEDED, horizontal AS_NEEDED)
 *     columnHeaderView → header JPanel  (null layout, absolute bounds)
 *     viewport         → VirtualCanvas  (null layout, virtual height)
 *                            slot[0..N]  (StandardRowPanel, absolute bounds)
 *   SOUTH panel (optional):
 *     footer JPanel    (null layout, absolute bounds)
 *     PaginationBar
 */
public class SmartGrid extends JPanel implements GridModelListener {

    private static final Color HEADER_BG = new Color(0x3C4B64);
    private static final Color FOOTER_BG = new Color(0xECEFF4);

    private final GridModel model;
    private final DefaultListSelectionModel selectionModel =
            new DefaultListSelectionModel();
    private int rowHeight = 32;

    // Shared column-width array — updated in-place by computeColumnWidths().
    // All StandardRowPanel instances in the pool reference this same object.
    private int[] columnWidths;
    private int   lastVpWidth = -1; // detect viewport-width changes in refresh()

    // Renderer delegates — replaceable at runtime
    private HeaderCellRenderer headerRenderer = new DefaultHeaderCellRenderer();
    private FooterCellRenderer footerRenderer = null;

    // Pagination state
    private int pageSize    = 0; // 0 = disabled
    private int currentPage = 0;

    // Sort state
    private String    currentSortKey   = null;
    private SortOrder currentSortOrder = SortOrder.NONE;

    // Cell renderer registry — keyed by ColumnDef.fndType
    private final Map<String, CellRenderer> cellRenderers = new HashMap<>();

    // Swing components
    private JScrollPane   scrollPane;
    private VirtualCanvas canvas;
    private JComponent[]  slots;
    private ComponentPool pool;
    private JPanel        southPanel    = null;
    private JPanel        footerPanel   = null;
    private PaginationBar paginationBar = null;

    public SmartGrid(GridModel model) {
        this.model = model;
        this.model.addGridModelListener(this);

        selectionModel.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) refresh();
        });

        List<ColumnDef> cols = model.getColumns();

        // Initialise column widths from preferredWidth defaults (refined on first refresh)
        columnWidths = new int[cols.size()];
        for (int i = 0; i < cols.size(); i++) {
            columnWidths[i] = cols.get(i).getPreferredWidth();
        }

        final GridModel capturedModel = model;
        final DefaultListSelectionModel sm = selectionModel;
        final int[] widths = columnWidths;
        final Map<String, CellRenderer> renderers = cellRenderers;
        this.pool = new ComponentPool(() -> new StandardRowPanel(cols,
            () -> {
                if (capturedModel instanceof DefaultGridModel) {
                    ((DefaultGridModel) capturedModel).notifyDataChanged();
                }
            },
            sm,
            widths,
            renderers));

        setLayout(new BorderLayout());

        canvas = new VirtualCanvas();
        scrollPane = new JScrollPane(canvas,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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
    // Public API — cell renderer registry
    // -------------------------------------------------------------------------

    /**
     * Registers a full {@link CellRenderer} for columns whose {@code fndType} matches
     * {@code fndType}. The renderer receives the existing cell component and may reuse
     * or replace it. Takes effect on the next bind cycle (next scroll or refresh).
     */
    public void registerCellRenderer(String fndType, CellRenderer renderer) {
        cellRenderers.put(fndType, renderer);
    }

    /**
     * Convenience overload: registers a formatter function that produces display text.
     * The cell is always rendered as a left-padded {@code JLabel}; the function converts
     * the raw {@code Object} value to the string to display.
     *
     * Example: {@code grid.registerFormatter("currency",
     *     v -> String.format("$%,d", ((Number) v).longValue()))}
     */
    public void registerFormatter(String fndType, Function<Object, String> formatter) {
        cellRenderers.put(fndType, (col, value, row, existing) -> {
            JLabel lbl = (existing instanceof JLabel) ? (JLabel) existing : new JLabel();
            lbl.setText(value != null ? formatter.apply(value) : "");
            lbl.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            return lbl;
        });
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
    // Internal — column width computation
    // -------------------------------------------------------------------------

    /** Updates columnWidths[] in-place based on viewport width and preferredWidths. */
    private void computeColumnWidths(int vpWidth, List<ColumnDef> cols) {
        int totalPref = 0;
        for (ColumnDef col : cols) totalPref += col.getPreferredWidth();
        if (totalPref <= 0) totalPref = 1;

        if (vpWidth >= totalPref) {
            // Scale up proportionally; last column absorbs integer-rounding remainder
            int remaining = vpWidth;
            for (int i = 0; i < cols.size() - 1; i++) {
                columnWidths[i] = (int) Math.round(
                    (double) cols.get(i).getPreferredWidth() / totalPref * vpWidth);
                remaining -= columnWidths[i];
            }
            columnWidths[cols.size() - 1] = Math.max(1, remaining);
        } else {
            // Preferred widths exceed viewport — use them as-is; horizontal scroll appears
            for (int i = 0; i < cols.size(); i++) {
                columnWidths[i] = cols.get(i).getPreferredWidth();
            }
        }
    }

    private int totalColumnWidth() {
        int t = 0;
        for (int w : columnWidths) t += w;
        return t;
    }

    // -------------------------------------------------------------------------
    // Internal — refresh
    // -------------------------------------------------------------------------

    private void refresh() {
        if (!isShowing()) return;

        int vpHeight = scrollPane.getViewport().getHeight();
        int vpWidth  = scrollPane.getViewport().getWidth();
        int scrollY  = scrollPane.getViewport().getViewPosition().y;

        if (vpHeight <= 0 || vpWidth <= 0) return;

        // Recompute column widths when viewport width changes, then rebuild header/footer
        if (vpWidth != lastVpWidth) {
            lastVpWidth = vpWidth;
            computeColumnWidths(vpWidth, model.getColumns());
            scrollPane.setColumnHeaderView(buildHeader(model.getColumns()));
            if (footerRenderer != null) refreshFooter();
        }

        int totalColWidth = totalColumnWidth();
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
            int rowIdx   = firstRow + i;
            int modelIdx = pageOffset + rowIdx;
            if (rowIdx < effectiveRows && modelIdx < model.getRowCount()) {
                GridRow row = model.getRow(modelIdx);
                // Bounds set BEFORE bind() so getHeight() returns rowHeight inside bind()
                slots[i].setBounds(0, rowIdx * rowHeight, totalColWidth, rowHeight);
                ((Recyclable) slots[i]).bind(row, modelIdx);
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
    // Internal — header / footer builders (null layout, absolute bounds)
    // -------------------------------------------------------------------------

    private JPanel buildHeader(List<ColumnDef> cols) {
        int totalColWidth = totalColumnWidth();
        JPanel header = new JPanel(null);
        header.setBackground(HEADER_BG);
        header.setPreferredSize(new Dimension(totalColWidth, rowHeight));
        int x = 0;
        for (int i = 0; i < cols.size(); i++) {
            ColumnDef col = cols.get(i);
            int w = columnWidths[i];
            SortOrder colSort = col.getKey().equals(currentSortKey)
                                ? currentSortOrder : SortOrder.NONE;
            JComponent cell = headerRenderer.render(col, colSort);
            cell.setBounds(x, 0, w, rowHeight);
            if (col.isSortable()) {
                final String sortKey = col.getKey();
                cell.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        cycleSortFor(sortKey);
                    }
                });
            }
            header.add(cell);
            x += w;
        }
        return header;
    }

    private void cycleSortFor(String key) {
        if (key.equals(currentSortKey)) {
            if (currentSortOrder == SortOrder.ASCENDING) {
                currentSortOrder = SortOrder.DESCENDING;
            } else {
                currentSortOrder = SortOrder.NONE;
                currentSortKey   = null;
            }
        } else {
            currentSortKey   = key;
            currentSortOrder = SortOrder.ASCENDING;
        }
        // Capture selected rows by object identity before the sort reorders them
        Set<GridRow> selectedRows = captureSelectedRows();
        if (model instanceof DefaultGridModel) {
            ((DefaultGridModel) model).sort(currentSortKey, currentSortOrder);
            // sort() → notifyDataChanged() → modelReset() → invokeLater(refresh)
        }
        // Restore selection using the new indices of the same GridRow objects
        restoreSelectedRows(selectedRows);
        // Rebuild header immediately so the indicator updates before refresh() runs
        scrollPane.setColumnHeaderView(buildHeader(model.getColumns()));
    }

    private Set<GridRow> captureSelectedRows() {
        Set<GridRow> selected = new HashSet<>();
        int min = selectionModel.getMinSelectionIndex();
        int max = selectionModel.getMaxSelectionIndex();
        for (int i = min; i <= max && min >= 0; i++) {
            if (selectionModel.isSelectedIndex(i) && i < model.getRowCount()) {
                selected.add(model.getRow(i));
            }
        }
        return selected;
    }

    private void restoreSelectedRows(Set<GridRow> selectedRows) {
        selectionModel.clearSelection();
        if (selectedRows.isEmpty()) return;
        int count = model.getRowCount();
        for (int i = 0; i < count; i++) {
            if (selectedRows.contains(model.getRow(i))) {
                selectionModel.addSelectionInterval(i, i);
            }
        }
    }

    private JPanel buildFooter(List<ColumnDef> cols) {
        int totalColWidth = totalColumnWidth();
        JPanel footer = new JPanel(null);
        footer.setBackground(FOOTER_BG);
        footer.setPreferredSize(new Dimension(totalColWidth, rowHeight));
        List<GridRow> pageRows = getPageRows();
        int x = 0;
        for (int i = 0; i < cols.size(); i++) {
            int w = columnWidths[i];
            JComponent cell = footerRenderer.render(cols.get(i), pageRows, model);
            cell.setBounds(x, 0, w, rowHeight);
            footer.add(cell);
            x += w;
        }
        return footer;
    }

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

    private void refreshFooter() {
        if (footerRenderer == null || southPanel == null || footerPanel == null) return;
        southPanel.remove(footerPanel);
        footerPanel = buildFooter(model.getColumns());
        southPanel.add(footerPanel, 0);
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
            int totalColWidth = totalColumnWidth();
            int vpWidth = scrollPane.getViewport().getWidth();
            int w = Math.max(totalColWidth, vpWidth > 0 ? vpWidth : 400);

            int pageOffset    = (pageSize > 0) ? currentPage * pageSize : 0;
            int effectiveRows = (pageSize > 0)
                ? Math.min(pageSize, model.getRowCount() - pageOffset)
                : model.getRowCount();
            return new Dimension(w, Math.max(0, effectiveRows) * rowHeight);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle v, int orientation, int dir) {
            return rowHeight;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle v, int orientation, int dir) {
            return v.height;
        }

        // Tracks viewport width only when all columns fit — triggers horizontal scroll otherwise
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return totalColumnWidth() <= scrollPane.getViewport().getWidth();
        }

        @Override
        public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
