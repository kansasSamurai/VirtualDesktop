package org.jwellman.swing.grid;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
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
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
 * Filtering:
 *   Two independent filter slots — {@code globalFilter} (set via setFilter()) and
 *   {@code columnFilter} (built from per-column text fields) — are composed with AND
 *   before being passed to the model. Either can be null (inactive).
 *
 * Layout:
 *   JScrollPane (vertical AS_NEEDED, horizontal AS_NEEDED)
 *     columnHeaderView → 1-row or 2-row JPanel
 *                          row 1: label row  (sort indicators, click to sort)
 *                          row 2: filter row (JTextFields, opt-in via setColumnFiltersVisible)
 *     viewport         → VirtualCanvas (null layout, virtual height)
 *   SOUTH panel (optional): footer JPanel + PaginationBar
 */
public class SmartGrid extends JPanel implements GridModelListener {

    // Light theme palette
    private static final Color HEADER_BG_LIGHT     = new Color(0x3C4B64);
    private static final Color FOOTER_BG_LIGHT     = new Color(0xECEFF4);
    private static final Color FILTER_ROW_BG_LIGHT = new Color(0xE8EDF5);

    // Dark theme palette
    private static final Color HEADER_BG_DARK     = new Color(0x2D3548);
    private static final Color FOOTER_BG_DARK     = new Color(0x282A2D);
    private static final Color FILTER_ROW_BG_DARK = new Color(0x32363B);

    // Active palette — selected at construction time
    private final Color   headerBg;
    private final Color   footerBg;
    private final Color   filterRowBg;
    private final boolean darkTheme;

    private final GridModel model;
    private final DefaultListSelectionModel selectionModel =
            new DefaultListSelectionModel();
    private int rowHeight = 32;

    // Shared column-width array — updated in-place by computeColumnWidths().
    // All StandardRowPanel instances in the pool reference this same object.
    private int[] columnWidths;
    private int   lastVpWidth = -1;

    // Renderer delegates — replaceable at runtime
    private HeaderCellRenderer headerRenderer = new DefaultHeaderCellRenderer();
    private FooterCellRenderer footerRenderer = null;

    // Filter state — two independent slots composed with AND before passing to model
    private GridModelFilter globalFilter = null; // set via setFilter() / clearFilter()
    private GridModelFilter columnFilter = null; // built by reapplyColumnFilter()

    // Per-column filter row (opt-in)
    private boolean      columnFiltersVisible = false;
    private JTextField[] columnFilterFields   = null;

    // Sort state
    private String    currentSortKey   = null;
    private SortOrder currentSortOrder = SortOrder.NONE;

    // Cell renderer registry — keyed by ColumnDef.fndType
    private final Map<String, CellRenderer> cellRenderers = new HashMap<>();

    // Pagination state
    private int pageSize    = 0;
    private int currentPage = 0;

    // Swing components
    private JScrollPane   scrollPane;
    private VirtualCanvas canvas;
    private JComponent[]  slots;
    private String[]      slotTypes; // parallel to slots[]; null = default (StandardRowPanel) pool
    private ComponentPool pool;
    private final Map<String, ComponentPool> typedPools = new HashMap<>();
    private JPanel        southPanel    = null;
    private JPanel        footerPanel   = null;
    private PaginationBar paginationBar = null;

    /** Constructs a SmartGrid with the light (default) theme. */
    public SmartGrid(GridModel model) {
        this(model, false);
    }

    /** Constructs a SmartGrid with the specified theme. */
    public SmartGrid(GridModel model, boolean darkTheme) {
        this.model     = model;
        this.darkTheme = darkTheme;
        this.headerBg    = darkTheme ? HEADER_BG_DARK     : HEADER_BG_LIGHT;
        this.footerBg    = darkTheme ? FOOTER_BG_DARK     : FOOTER_BG_LIGHT;
        this.filterRowBg = darkTheme ? FILTER_ROW_BG_DARK : FILTER_ROW_BG_LIGHT;

        this.model.addGridModelListener(this);

        selectionModel.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refresh();
            }
        });

        List<ColumnDef> cols = model.getColumns();

        columnWidths = new int[cols.size()];
        for (int i = 0; i < cols.size(); i++) {
            columnWidths[i] = cols.get(i).getPreferredWidth();
        }

        final GridModel capturedModel = model;
        final DefaultListSelectionModel sm = selectionModel;
        final int[] widths = columnWidths;
        final Map<String, CellRenderer> renderers = cellRenderers;
        final boolean dark = darkTheme;
        this.pool = new ComponentPool(() -> new StandardRowPanel(cols,
            () -> {
                if (capturedModel instanceof DefaultGridModel) {
                    ((DefaultGridModel) capturedModel).notifyDataChanged();
                }
            },
            sm,
            widths,
            renderers,
            dark));

        setLayout(new BorderLayout());

        canvas = new VirtualCanvas();
        scrollPane = new JScrollPane(canvas,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setColumnHeaderView(buildHeader(cols));

        scrollPane.getViewport().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                refresh();
            }
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

    public GridModel getModel() {
        return model;
    }

    public int getRowHeight() {
        return rowHeight;
    }

    public void setRowHeight(int rowHeight) {
        this.rowHeight = rowHeight;
        canvas.revalidate();
        refresh();
    }

    // -------------------------------------------------------------------------
    // Public API — selection
    // -------------------------------------------------------------------------

    public ListSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public void addListSelectionListener(ListSelectionListener l) {
        selectionModel.addListSelectionListener(l);
    }

    public void selectAll() {
        int n = model.getRowCount();
        if (n > 0) {
            selectionModel.setSelectionInterval(0, n - 1);
        }
    }

    public void clearSelection() {
        selectionModel.clearSelection();
    }

    // -------------------------------------------------------------------------
    // Public API — filtering
    // -------------------------------------------------------------------------

    /**
     * Sets the global filter (tests whether ANY column matches — OR semantics).
     * Composes with the column filter row if that is also active.
     * Clears the current selection.
     */
    public void setFilter(GridModelFilter f) {
        selectionModel.clearSelection();
        globalFilter = f;
        applyComposedFilter();
    }

    /**
     * Clears the global filter. Column filter row (if visible) remains active.
     * Clears the current selection.
     */
    public void clearFilter() {
        selectionModel.clearSelection();
        globalFilter = null;
        applyComposedFilter();
    }

    /**
     * Shows or hides the per-column filter row beneath the header labels.
     * When shown, each column gets a text field; typing ANDs with the global filter.
     */
    public void setColumnFiltersVisible(boolean visible) {
        if (visible == columnFiltersVisible) {
            return;
        }
        columnFiltersVisible = visible;

        if (visible) {
            List<ColumnDef> cols = model.getColumns();
            columnFilterFields = new JTextField[cols.size()];
            for (int i = 0; i < cols.size(); i++) {
                JTextField field = new JTextField();
                field.getDocument().addDocumentListener(new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        reapplyColumnFilter();
                    }
                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        reapplyColumnFilter();
                    }
                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        reapplyColumnFilter();
                    }
                });
                columnFilterFields[i] = field;
            }
        } else {
            columnFilterFields = null;
            columnFilter = null;
            applyComposedFilter();
        }

        scrollPane.setColumnHeaderView(buildHeader(model.getColumns()));
    }

    /**
     * Clears all per-column filter fields, which removes the column filter predicate.
     * The global filter (if set) remains active.
     */
    public void clearColumnFilters() {
        if (columnFilterFields == null) {
            return;
        }
        for (JTextField field : columnFilterFields) {
            field.setText("");
        }
        // DocumentListeners fire reapplyColumnFilter() per field, which is harmless
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
    // Public API — row renderer registry (GridComponentFactory)
    // -------------------------------------------------------------------------

    /**
     * Registers a row component supplier for rows whose {@code fnd-type} tag
     * matches {@code fndType}. The component must implement {@link Recyclable}.
     * Rows with no {@code fnd-type} tag continue to use {@link StandardRowPanel}.
     */
    public void registerRowRenderer(String fndType, Supplier<JComponent> supplier) {
        typedPools.put(fndType, new ComponentPool(supplier));
    }

    // -------------------------------------------------------------------------
    // Public API — cell renderer registry (column-level)
    // -------------------------------------------------------------------------

    /**
     * Registers a {@link CellRenderer} for columns whose {@code fndType} matches the key.
     * The renderer receives the existing cell component and may reuse or replace it.
     */
    public void registerCellRenderer(String fndType, CellRenderer renderer) {
        cellRenderers.put(fndType, renderer);
    }

    /**
     * Convenience: registers a formatter function that displays a value as a styled JLabel.
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

    public int getPageSize() {
        return pageSize;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int totalPages() {
        if (pageSize <= 0) {
            return 1;
        }
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
        if (paginationBar != null) {
            paginationBar.update(currentPage, totalPages());
        }
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
            if (paginationBar != null) {
                paginationBar.update(currentPage, totalPages());
            }
        });
    }

    // -------------------------------------------------------------------------
    // Internal — filter composition
    // -------------------------------------------------------------------------

    /** Composes globalFilter and columnFilter with AND and passes result to model. */
    private void applyComposedFilter() {
        if (globalFilter == null && columnFilter == null) {
            if (model instanceof DefaultGridModel) {
                ((DefaultGridModel) model).clearFilter();
            }
            return;
        }
        final GridModelFilter g = globalFilter;
        final GridModelFilter c = columnFilter;
        GridModelFilter composed = row -> {
            if (g != null && !g.accept(row)) {
                return false;
            }
            if (c != null && !c.accept(row)) {
                return false;
            }
            return true;
        };
        if (model instanceof DefaultGridModel) {
            ((DefaultGridModel) model).setFilter(composed);
        }
    }

    /** Rebuilds columnFilter from current field text and calls applyComposedFilter(). */
    private void reapplyColumnFilter() {
        if (columnFilterFields == null) {
            return;
        }
        List<ColumnDef> cols = model.getColumns();
        boolean anyActive = false;
        for (JTextField field : columnFilterFields) {
            if (!field.getText().trim().isEmpty()) {
                anyActive = true;
                break;
            }
        }
        if (!anyActive) {
            columnFilter = null;
        } else {
            final JTextField[] fields = columnFilterFields;
            columnFilter = row -> {
                for (int i = 0; i < cols.size() && i < fields.length; i++) {
                    String term = fields[i].getText().trim().toLowerCase();
                    if (term.isEmpty()) {
                        continue;
                    }
                    Object val = row.get(cols.get(i).getKey());
                    if (val == null || !val.toString().toLowerCase().contains(term)) {
                        return false;
                    }
                }
                return true;
            };
        }
        applyComposedFilter();
    }

    // -------------------------------------------------------------------------
    // Internal — column width computation
    // -------------------------------------------------------------------------

    private void computeColumnWidths(int vpWidth, List<ColumnDef> cols) {
        int totalPref = 0;
        for (ColumnDef col : cols) {
            totalPref += col.getPreferredWidth();
        }
        if (totalPref <= 0) {
            totalPref = 1;
        }

        if (vpWidth >= totalPref) {
            int remaining = vpWidth;
            for (int i = 0; i < cols.size() - 1; i++) {
                columnWidths[i] = (int) Math.round(
                    (double) cols.get(i).getPreferredWidth() / totalPref * vpWidth);
                remaining -= columnWidths[i];
            }
            columnWidths[cols.size() - 1] = Math.max(1, remaining);
        } else {
            for (int i = 0; i < cols.size(); i++) {
                columnWidths[i] = cols.get(i).getPreferredWidth();
            }
        }
    }

    private int totalColumnWidth() {
        int t = 0;
        for (int w : columnWidths) {
            t += w;
        }
        return t;
    }

    /**
     * Sets a new columnHeaderView while preserving focus if the focus owner is one
     * of the per-column filter fields. When a JTextField is reparented (moved from
     * the old filter-row panel to the newly built one) Swing drops focus to the next
     * focusable sibling; this method restores it via invokeLater.
     */
    private void rebuildHeaderView(List<ColumnDef> cols) {
        int focusedFieldIdx = -1;
        if (columnFilterFields != null) {
            Component focused = KeyboardFocusManager
                    .getCurrentKeyboardFocusManager().getFocusOwner();
            for (int i = 0; i < columnFilterFields.length; i++) {
                if (columnFilterFields[i] == focused) {
                    focusedFieldIdx = i;
                    break;
                }
            }
        }

        scrollPane.setColumnHeaderView(buildHeader(cols));

        if (focusedFieldIdx >= 0) {
            final int idx = focusedFieldIdx;
            SwingUtilities.invokeLater(() -> columnFilterFields[idx].requestFocusInWindow());
        }
    }

    // -------------------------------------------------------------------------
    // Internal — refresh
    // -------------------------------------------------------------------------

    private void refresh() {
        if (!isShowing()) {
            return;
        }

        int vpHeight = scrollPane.getViewport().getHeight();
        int vpWidth  = scrollPane.getViewport().getWidth();
        int scrollY  = scrollPane.getViewport().getViewPosition().y;

        if (vpHeight <= 0 || vpWidth <= 0) {
            return;
        }

        if (vpWidth != lastVpWidth) {
            lastVpWidth = vpWidth;
            computeColumnWidths(vpWidth, model.getColumns());
            rebuildHeaderView(model.getColumns());
            if (footerRenderer != null) {
                refreshFooter();
            }
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
                String requiredType = row.getTag("fnd-type");

                // Swap component only when the required row type changes
                if (!Objects.equals(requiredType, slotTypes[i])) {
                    getPoolForType(slotTypes[i]).release(slots[i]);
                    canvas.remove(slots[i]);
                    slots[i]     = getPoolForType(requiredType).checkout();
                    slotTypes[i] = requiredType;
                    canvas.add(slots[i]);
                }

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
            for (int i = 0; i < slots.length; i++) {
                canvas.remove(slots[i]);
                getPoolForType(slotTypes[i]).release(slots[i]);
            }
        }
        slots     = new JComponent[count];
        slotTypes = new String[count]; // all null = default pool
        for (int i = 0; i < count; i++) {
            slots[i] = pool.checkout();
            canvas.add(slots[i]);
        }
    }

    private ComponentPool getPoolForType(String fndType) {
        if (fndType != null && typedPools.containsKey(fndType)) {
            return typedPools.get(fndType);
        }
        return pool;
    }

    // -------------------------------------------------------------------------
    // Internal — header / footer builders (null layout, absolute bounds)
    // -------------------------------------------------------------------------

    /**
     * Builds the columnHeaderView: a single label row, or a 2-row panel (label + filter)
     * when column filters are visible.
     */
    private JPanel buildHeader(List<ColumnDef> cols) {
        JPanel labelRow = buildHeaderLabelRow(cols);
        if (!columnFiltersVisible || columnFilterFields == null) {
            return labelRow;
        }
        JPanel filterRow = buildHeaderFilterRow(cols);
        JPanel combined  = new JPanel();
        combined.setLayout(new BoxLayout(combined, BoxLayout.Y_AXIS));
        combined.add(labelRow);
        combined.add(filterRow);
        return combined;
    }

    private JPanel buildHeaderLabelRow(List<ColumnDef> cols) {
        int totalColWidth = totalColumnWidth();
        JPanel header = new JPanel(null);
        header.setBackground(headerBg);
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
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        cycleSortFor(sortKey);
                    }
                });
            }
            header.add(cell);
            x += w;
        }
        return header;
    }

    private JPanel buildHeaderFilterRow(List<ColumnDef> cols) {
        int totalColWidth = totalColumnWidth();
        int filterHeight  = rowHeight - 2;
        JPanel filterRow  = new JPanel(null);
        filterRow.setBackground(filterRowBg);
        filterRow.setPreferredSize(new Dimension(totalColWidth, filterHeight));
        int x = 0;
        for (int i = 0; i < cols.size() && i < columnFilterFields.length; i++) {
            int w = columnWidths[i];
            JTextField field = columnFilterFields[i];
            field.setBounds(x + 2, 2, w - 4, filterHeight - 4);
            filterRow.add(field);
            x += w;
        }
        return filterRow;
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
        Set<GridRow> selectedRows = captureSelectedRows();
        if (model instanceof DefaultGridModel) {
            ((DefaultGridModel) model).sort(currentSortKey, currentSortOrder);
        }
        restoreSelectedRows(selectedRows);
        rebuildHeaderView(model.getColumns());
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
        if (selectedRows.isEmpty()) {
            return;
        }
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
        footer.setBackground(footerBg);
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
        if (old != null) {
            remove(old);
        }

        southPanel    = null;
        footerPanel   = null;
        paginationBar = null;

        boolean hasFooter = footerRenderer != null;
        boolean hasPaging = pageSize > 0;

        if (!hasFooter && !hasPaging) {
            revalidate();
            return;
        }

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
        if (footerRenderer == null || southPanel == null || footerPanel == null) {
            return;
        }
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
            // Return a compact viewport hint used by JScrollPane when pack() is called.
            // getPreferredSize() (the full virtual height) is still used for scrollbar math;
            // this method mirrors the JTable pattern of keeping the two concerns separate.
            int totalColWidth = totalColumnWidth();
            int vpWidth = scrollPane.getViewport().getWidth();
            int w = Math.max(totalColWidth, vpWidth > 0 ? vpWidth : 400);
            return new Dimension(w, 400);
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle v, int orientation, int dir) {
            return rowHeight;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle v, int orientation, int dir) {
            return v.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return totalColumnWidth() <= scrollPane.getViewport().getWidth();
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
