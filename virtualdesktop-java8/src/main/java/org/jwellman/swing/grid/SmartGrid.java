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
import javax.swing.JViewport;
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
@SuppressWarnings("serial")
public class SmartGrid extends JPanel implements GridModelListener {

    // Light theme palette
    private static final Color HEADER_BG_LIGHT     = new Color(0x3C4B64);
    private static final Color FOOTER_BG_LIGHT     = new Color(0xECEFF4);
    private static final Color FILTER_ROW_BG_LIGHT = new Color(0xE8EDF5);
    private static final Color CANVAS_BG_LIGHT     = Color.WHITE;

    // Dark theme palette
    private static final Color HEADER_BG_DARK     = new Color(0x2D3548);
    private static final Color FOOTER_BG_DARK     = new Color(0x282A2D);
    private static final Color FILTER_ROW_BG_DARK = new Color(0x32363B);
    private static final Color CANVAS_BG_DARK     = new Color(0x1E1E1E);

    // Active palette — selected at construction time
    private final Color   headerBg;
    private final Color   footerBg;
    private final Color   filterRowBg;
    private final boolean darkTheme;

    private final GridModel model;
    private final DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
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

    // Multi-column sort state; index 0 = primary sort
    private final List<SortSpec> currentSortSpecs = new ArrayList<>();

    // Cell renderer registry — keyed by ColumnDef.fndType
    private final Map<String, CellRenderer> cellRenderers = new HashMap<>();

    // Pagination state
    private int pageSize    = 0;
    private int currentPage = 0;

    // Strip list — ordered left to right; each contributes to canvasLeadWidth()
    private final List<Strip> strips       = new ArrayList<>();
    private RowNumberStrip    rowNumberStrip;
    private CheckboxStrip     checkboxStrip;
    private TreeZoneStrip     treeZoneStrip;

    // Swing components
    private JPanel        toolbarPanel  = null; // created lazily by getToolbar()
    private JPanel        northPanel    = null; // BoxLayout.Y wrapper; created lazily
    private JScrollPane   scrollPane;
    private VirtualCanvas canvas;
    private JComponent[]  slots;
    private String[]      slotTypes; // parallel to slots[]; null = default (StandardRowPanel) pool
    private ComponentPool pool;
    private final Map<String, ComponentPool> typedPools = new HashMap<>();
    private boolean editable   = false;
    private boolean refreshing = false; // re-entrancy guard
    private JPanel        southPanel    = null;
    private JPanel        footerPanel   = null;
    private PaginationBar paginationBar = null;

    // User-defined footer zone — persisted across rebuildSouthPanel() calls
    private final java.util.List<JComponent> extraFooterRows = new java.util.ArrayList<>();

    // Summary row — always last, always present
    private JPanel summaryRow;
    private JLabel summaryRowCount;
    private JLabel summarySelCount;

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

        rowNumberStrip = new RowNumberStrip(darkTheme, headerBg);
        checkboxStrip  = new CheckboxStrip(selectionModel, model,
                this::selectAll, this::clearSelection);
        treeZoneStrip  = new TreeZoneStrip(() -> {
            if (model instanceof DefaultGridModel) {
                ((DefaultGridModel) model).notifyDataChanged();
            }
        });
        treeZoneStrip.setVisible(false);
        strips.add(rowNumberStrip);  // index 0 — far left
        strips.add(checkboxStrip);   // index 1
        strips.add(treeZoneStrip);   // index 2

        registerRowRenderer("group-header", GroupHeaderRowPanel::new);

        setLayout(new BorderLayout());

        canvas = new VirtualCanvas();
        canvas.setBackground(darkTheme ? CANVAS_BG_DARK : CANVAS_BG_LIGHT);
        scrollPane = new JScrollPane(canvas,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
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

        // Fill the upper-right corner so the header band spans the full width.
        // When the column filter row is visible the corner splits: top rowHeight px
        // in headerBg, remainder in filterRowBg, matching the two-row header layout.
        JPanel cornerFill = new JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                if (columnFiltersVisible) {
                    g.setColor(headerBg);
                    g.fillRect(0, 0, getWidth(), rowHeight);
                    g.setColor(filterRowBg);
                    g.fillRect(0, rowHeight, getWidth(), Math.max(0, getHeight() - rowHeight));
                } else {
                    g.setColor(headerBg);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        cornerFill.setBackground(headerBg);
        scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, cornerFill);

        add(scrollPane, BorderLayout.CENTER);

        summaryRow = buildSummaryRow();
        rebuildSouthPanel();
        updateSummaryCount();
    }

    // -------------------------------------------------------------------------
    // Public API — basic
    // -------------------------------------------------------------------------

    public GridModel getModel() {
        return model;
    }

    /**
     * Returns the shared mutable column-width array. Callers that hold this
     * reference will automatically see width updates when the viewport is resized,
     * because SmartGrid updates the array in-place — the same guarantee given to
     * {@link StandardRowPanel} instances in the pool.
     */
    public int[] getColumnWidths() {
        return columnWidths;
    }

    public int getRowHeight() {
        return rowHeight;
    }

    public void setRowHeight(int rowHeight) {
        this.rowHeight = rowHeight;
        canvas.revalidate();
        refresh();
    }

    /**
     * Returns the grid's built-in toolbar panel, creating it on first call.
     * The toolbar sits above the column header row and shares the same background
     * colour as the headers. Add any components ({@code JLabel}, {@code JTextField},
     * {@code JToggleButton}, etc.) directly to this panel.
     */
    public JPanel getToolbar() {
        if (toolbarPanel == null) {
            ensureNorthPanel();
            toolbarPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 4));
            toolbarPanel.setBackground(headerBg);
            northPanel.add(toolbarPanel, 0); // always topmost in the header zone
            revalidate();
        }
        return toolbarPanel;
    }

    /**
     * Adds a custom row to the header zone, stacked below the toolbar and above
     * the column header.  Rows appear in the order they are added.
     */
    public void addHeaderRow(JComponent row) {
        ensureNorthPanel();
        northPanel.add(row);
        revalidate();
    }

    /**
     * Adds a custom row to the footer zone, stacked above the summary row and below
     * the pagination bar.  Rows appear in the order they are added and survive
     * theme/pagination rebuilds.
     */
    public void addFooterRow(JComponent row) {
        extraFooterRows.add(row);
        rebuildSouthPanel();
    }

    public boolean isRowNumbersVisible() {
        return rowNumberStrip.isVisible();
    }

    public void setRowNumbersVisible(boolean visible) {
        if (rowNumberStrip.isVisible() == visible) {
            return;
        }
        rowNumberStrip.setVisible(visible);
        onStripVisibilityChanged();
    }

    public boolean isCheckboxColumnVisible() {
        return checkboxStrip.isVisible();
    }

    public void setCheckboxColumnVisible(boolean visible) {
        if (checkboxStrip.isVisible() == visible) {
            return;
        }
        checkboxStrip.setVisible(visible);
        onStripVisibilityChanged();
    }

    public boolean isTreeZoneVisible() {
        return treeZoneStrip.isVisible();
    }

    public void setTreeZoneVisible(boolean visible) {
        if (treeZoneStrip.isVisible() == visible) {
            return;
        }
        treeZoneStrip.setVisible(visible);
        onStripVisibilityChanged();
    }

    private void onStripVisibilityChanged() {
        if (slots != null) {
            reallocateSlots(slots.length);
        }
        lastVpWidth = -1;
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

    /**
     * Enables or disables global inline edit mode. When enabled, all rows switch
     * to {@link EditableRowPanel} (JTextField per column); edits are written back
     * to the {@link GridRow} on focus-lost. When disabled, rows revert to the
     * normal type-dispatch pool.
     */
    public void setEditable(boolean editable) {
        if (this.editable == editable) {
            return;
        }
        this.editable = editable;
        if (editable && !typedPools.containsKey("edit")) {
            List<ColumnDef> cols = model.getColumns();
            registerRowRenderer("edit",
                () -> new EditableRowPanel(cols, selectionModel, columnWidths, darkTheme));
        }
        if (slots != null) {
            reallocateSlots(slots.length);
        }
        refresh();
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
    // Internal — strip helpers
    // -------------------------------------------------------------------------

    private int canvasLeadWidth() {
        int total = 0;
        for (Strip strip : strips) {
            if (strip.isVisible()) {
                total += strip.getWidth();
            }
        }
        return total;
    }

    private int totalCanvasWidth() {
        return canvasLeadWidth() + totalColumnWidth();
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
        // Preserve focus across header rebuild.  The header panel is replaced
        // entirely on viewport width changes (scroll bar appearing/disappearing),
        // which re-parents any embedded interactive components and loses their
        // focus.  Save the focused component if it lives anywhere in the column
        // header — covers both column filter fields and custom HeaderCellRenderer
        // components such as embedded search fields.
        Component focused = KeyboardFocusManager
                .getCurrentKeyboardFocusManager().getFocusOwner();
        JViewport columnHeader = scrollPane.getColumnHeader();
        boolean headerHadFocus = focused != null && columnHeader != null
                && SwingUtilities.isDescendingFrom(focused, columnHeader);

        scrollPane.setColumnHeaderView(buildHeader(cols));

        // The saved component is the same object — it was re-parented into the
        // new header by the HeaderCellRenderer.  Restore focus on the next EDT
        // pass once the new hierarchy is fully wired.
        if (headerHadFocus) {
            final Component toFocus = focused;
            SwingUtilities.invokeLater(() -> toFocus.requestFocusInWindow());
        }
    }

    // -------------------------------------------------------------------------
    // Internal — refresh
    // -------------------------------------------------------------------------

    private void refresh() {
        if (refreshing || !isShowing()) {
            return;
        }
        refreshing = true;
        try {
            doRefresh();
        } finally {
            refreshing = false;
        }
    }

    private void doRefresh() {
        int vpHeight = scrollPane.getViewport().getHeight();
        int vpWidth  = scrollPane.getViewport().getWidth();
        int scrollY  = scrollPane.getViewport().getViewPosition().y;

        if (vpHeight <= 0 || vpWidth <= 0) {
            return;
        }

        if (vpWidth != lastVpWidth) {
            lastVpWidth = vpWidth;
            computeColumnWidths(vpWidth - canvasLeadWidth(), model.getColumns());
            rebuildHeaderView(model.getColumns());
            if (footerRenderer != null) {
                refreshFooter();
            }
            // Re-read height: FlatLaf's synchronous layout inside setColumnHeaderView
            // can change the viewport height between the initial capture and slot sizing.
            vpHeight = scrollPane.getViewport().getHeight();
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
                String requiredType = resolveRowType(row);

                // Swap component only when the required row type changes
                if (!Objects.equals(requiredType, slotTypes[i])) {
                    releaseSlot(getPoolForType(slotTypes[i]), slots[i]);
                    slots[i]     = checkoutSlot(getPoolForType(requiredType));
                    slotTypes[i] = requiredType;
                }

                int leadX = 0;
                for (Strip strip : strips) {
                    if (strip.isVisible()) {
                        strip.bind(i, leadX, rowIdx * rowHeight, rowHeight, modelIdx, row);
                        leadX += strip.getWidth();
                    }
                }
                slots[i].setBounds(leadX, rowIdx * rowHeight, totalColWidth, rowHeight);
                ((Recyclable) slots[i]).bind(row, modelIdx);
                if (slots[i] instanceof Selectable) {
                    ((Selectable) slots[i]).setSelected(selectionModel.isSelectedIndex(modelIdx));
                }
                slots[i].setVisible(true);
            } else {
                slots[i].setVisible(false);
                for (Strip strip : strips) {
                    if (strip.isVisible()) {
                        strip.hide(i);
                    }
                }
            }
        }

        canvas.repaint();
    }


    private void reallocateSlots(int count) {
        if (slots != null) {
            for (int i = 0; i < slots.length; i++) {
                releaseSlot(getPoolForType(slotTypes[i]), slots[i]);
            }
            for (Strip strip : strips) {
                if (strip.isVisible()) {
                    strip.reallocate(0, canvas);
                }
            }
        }
        slots     = new JComponent[count];
        slotTypes = new String[count];
        for (Strip strip : strips) {
            if (strip.isVisible()) {
                strip.reallocate(count, canvas);
            }
        }
        for (int i = 0; i < count; i++) {
            slots[i] = checkoutSlot(pool);
        }
    }

    private ComponentPool getPoolForType(String fndType) {
        if (fndType != null && typedPools.containsKey(fndType)) {
            return typedPools.get(fndType);
        }
        return pool;
    }

    // Checks out a slot component and ensures it is attached to the canvas exactly
    // once.  Fresh components (parent == null, bounds 0,0,0,0) are added with a
    // zero-pixel dirty region — no visible flash.  Recycled components already have
    // canvas as their parent and are never re-added.
    private JComponent checkoutSlot(ComponentPool p) {
        JComponent c = p.checkout();
        if (c.getParent() != canvas) {
            canvas.add(c);
        }
        return c;
    }

    // Hides a slot component and returns it to its pool without removing it from
    // the canvas.  Keeping it attached means no dirty-region gap is created at its
    // former position.
    private void releaseSlot(ComponentPool p, JComponent c) {
        c.setVisible(false);
        p.release(c);
    }

    private void ensureNorthPanel() {
        if (northPanel == null) {
            northPanel = new JPanel();
            northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
            add(northPanel, BorderLayout.NORTH);
        }
    }

    private JPanel buildSummaryRow() {
        Color bg  = darkTheme ? new Color(0x32363B) : new Color(0xE8ECF0);
        Color fg  = darkTheme ? new Color(0x9A9A9A) : new Color(0x666677);
        Color bdr = darkTheme ? new Color(0x4A4D52) : new Color(0xC8CDD3);

        summaryRowCount = new JLabel("Showing 0 rows");
        summaryRowCount.setFont(summaryRowCount.getFont().deriveFont(java.awt.Font.PLAIN, 10f));
        summaryRowCount.setForeground(fg);

        summarySelCount = new JLabel("0 rows selected");
        summarySelCount.setFont(summarySelCount.getFont().deriveFont(java.awt.Font.PLAIN, 10f));
        summarySelCount.setForeground(fg);

        model.addGridModelListener(new GridModelListener() {
            @Override
            public void rowsChanged(int first, int last) {
                updateSummaryCount();
            }
            @Override
            public void modelReset() {
                updateSummaryCount();
            }
        });

        selectionModel.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int n   = 0;
                int min = selectionModel.getMinSelectionIndex();
                int max = selectionModel.getMaxSelectionIndex();
                for (int i = min; i <= max && min >= 0; i++) {
                    if (selectionModel.isSelectedIndex(i)) {
                        n++;
                    }
                }
                summarySelCount.setText(n + " row(s) selected");
            }
        });

        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(bg);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, bdr),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        row.add(summaryRowCount, BorderLayout.WEST);
        row.add(summarySelCount, BorderLayout.EAST);
        return row;
    }

    private void updateSummaryCount() {
        int visible = model.getRowCount();
        int total   = model.getTotalRowCount();
        summaryRowCount.setText(visible == total
            ? "Showing " + total + " rows"
            : "Showing " + visible + " of " + total + " rows");
    }

    private String resolveRowType(GridRow row) {
        if (editable)            return "edit";
        if (row.isGroupHeader()) return "group-header";
        return row.getTag("fnd-type");
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
        int leadW         = canvasLeadWidth();
        int totalColWidth = totalColumnWidth();
        JPanel header = new JPanel(null);
        header.setBackground(headerBg);
        header.setPreferredSize(new Dimension(leadW + totalColWidth, rowHeight));

        int x = 0;
        for (Strip strip : strips) {
            if (strip.isVisible()) {
                int sw = strip.getWidth();
                JComponent cell = strip.headerCell(rowHeight);
                cell.setBounds(x, 0, sw, rowHeight);
                header.add(cell);
                x += sw;
            }
        }

        boolean multiSort = currentSortSpecs.size() > 1;
        for (int i = 0; i < cols.size(); i++) {
            ColumnDef col = cols.get(i);
            int w = columnWidths[i];

            // Determine sort direction and rank for this column
            SortOrder colSort = SortOrder.NONE;
            int rank = 0;
            for (int j = 0; j < currentSortSpecs.size(); j++) {
                if (currentSortSpecs.get(j).getKey().equals(col.getKey())) {
                    colSort = currentSortSpecs.get(j).getOrder();
                    rank    = multiSort ? j + 1 : 0; // rank number shown only in multi-sort
                    break;
                }
            }

            JComponent cell = headerRenderer.render(col, colSort, rank);
            cell.setBounds(x, 0, w, rowHeight);
            if (col.isSortable()) {
                final String sortKey = col.getKey();
                cell.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        cycleSortFor(sortKey, e.isShiftDown());
                    }
                });
            }
            header.add(cell);
            x += w;
        }
        return header;
    }

    private JPanel buildHeaderFilterRow(List<ColumnDef> cols) {
        int leadW         = canvasLeadWidth();
        int totalColWidth = totalColumnWidth();
        int filterHeight  = rowHeight - 2;
        JPanel filterRow  = new JPanel(null);
        filterRow.setBackground(filterRowBg);
        filterRow.setPreferredSize(new Dimension(leadW + totalColWidth, filterHeight));

        int x = 0;
        for (Strip strip : strips) {
            if (strip.isVisible()) {
                int sw = strip.getWidth();
                JComponent spacer = strip.filterSpacer(filterHeight);
                spacer.setBounds(x, 0, sw, filterHeight);
                filterRow.add(spacer);
                x += sw;
            }
        }
        for (int i = 0; i < cols.size() && i < columnFilterFields.length; i++) {
            int w = columnWidths[i];
            JTextField field = columnFilterFields[i];
            field.setBounds(x + 2, 2, w - 4, filterHeight - 4);
            filterRow.add(field);
            x += w;
        }
        return filterRow;
    }

    private void cycleSortFor(String key, boolean addToSort) {
        // Find this column's current position in the sort spec list (-1 if absent)
        int existingIdx = -1;
        for (int i = 0; i < currentSortSpecs.size(); i++) {
            if (currentSortSpecs.get(i).getKey().equals(key)) {
                existingIdx = i;
                break;
            }
        }

        if (addToSort) {
            // Shift+click: add column to multi-sort, cycle its direction, or remove it
            if (existingIdx >= 0) {
                SortSpec s = currentSortSpecs.get(existingIdx);
                if (s.getOrder() == SortOrder.ASCENDING) {
                    currentSortSpecs.set(existingIdx, new SortSpec(key, SortOrder.DESCENDING));
                } else {
                    currentSortSpecs.remove(existingIdx);
                }
            } else {
                currentSortSpecs.add(new SortSpec(key, SortOrder.ASCENDING));
            }
        } else {
            // Regular click: single-column sort, cycling NONE → ASC → DESC → NONE
            if (existingIdx >= 0 && currentSortSpecs.size() == 1) {
                // This column is already the sole sort — cycle its direction
                SortSpec s = currentSortSpecs.get(0);
                if (s.getOrder() == SortOrder.ASCENDING) {
                    currentSortSpecs.set(0, new SortSpec(key, SortOrder.DESCENDING));
                } else {
                    currentSortSpecs.clear(); // DESC → NONE
                }
            } else {
                // Different column clicked, or was part of multi-sort — start fresh
                currentSortSpecs.clear();
                currentSortSpecs.add(new SortSpec(key, SortOrder.ASCENDING));
            }
        }

        Set<GridRow> selectedRows = captureSelectedRows();
        if (model instanceof DefaultGridModel) {
            ((DefaultGridModel) model).sort(currentSortSpecs);
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
        int leadW         = canvasLeadWidth();
        int totalColWidth = totalColumnWidth();
        JPanel footer = new JPanel(null);
        footer.setBackground(footerBg);
        footer.setPreferredSize(new Dimension(leadW + totalColWidth, rowHeight));

        int x = 0;
        for (Strip strip : strips) {
            if (strip.isVisible()) {
                int sw = strip.getWidth();
                JComponent spacer = strip.footerSpacer(rowHeight);
                spacer.setBounds(x, 0, sw, rowHeight);
                footer.add(spacer);
                x += sw;
            }
        }

        List<GridRow> pageRows = getPageRows();
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

        southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));

        if (footerRenderer != null) {
            footerPanel = buildFooter(model.getColumns());
            southPanel.add(footerPanel);
        }
        if (pageSize > 0) {
            paginationBar = new PaginationBar(this);
            southPanel.add(paginationBar);
        }
        for (JComponent row : extraFooterRows) {
            southPanel.add(row);
        }
        // Summary row is always last — this is an opinionated design decision.
        // It provides visual grounding and a de-facto status contract that
        // consumers can rely on without knowing the rest of the south zone layout.
        southPanel.add(summaryRow);

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

    /**
     * VirtualCanvas is short because it's doing one very specific job with remarkable precision: it **lies to the JScrollPane** about its own size while only actually containing ~20 components.
        
        The fundamental problem is this: you want a scrollbar that represents 1,000 rows, but you can't afford 1,000 live JComponents. The naive solutions all fail — creating 1,000 JComponents crushes memory, rendering everything as painted pixels gives you dead UI, removing JScrollPane entirely means you have to reimplement all the scrollbar math yourself.
        
        VirtualCanvas threads the needle by exploiting a separation that already exists in JScrollPane's architecture: **the scrollbar's range is determined by the view's preferred size, not by the view's actual contents.**
        
        When JScrollPane asks "how tall are you?", VirtualCanvas answers via `getPreferredSize()`:
        
        ```java
        return new Dimension(w, model.getRowCount() * rowHeight);  // e.g. 32,000px
        ```
        
        JScrollPane believes it, creates a scrollbar spanning 32,000px, and shows a viewport window into that space. But the viewport doesn't render the canvas by asking each child component to paint itself in sequence — it renders whatever is physically at the canvas coordinates currently in view.
        
        VirtualCanvas exploits this by placing its ~20 real components at the **exact y coordinates** where they need to appear for the current scroll position:
        
        ```java
        slots[i].setBounds(0, rowIdx * rowHeight, width, rowHeight);
        // slot 0 at y=160, slot 1 at y=192, slot 2 at y=224...
        ```
        
        When the viewport shows canvas pixels y=160 to y=640, the slot components are *already there* — physically positioned at those coordinates within the 32,000px canvas. No fake painting, no illusion. The components are exactly where JScrollPane expects them.
        
        Scrolling doesn't move the components through a window — it moves the viewport window to where the components already are, and the components get repositioned and rebound in `refresh()` to match the new viewport. ~20 `setBounds()` calls plus ~20 `bind()` data loads per scroll event, regardless of whether the dataset has 100 rows or 100,000.
        
        The `Scrollable` interface is the communication layer between VirtualCanvas and JScrollPane. Without it, JScrollPane would make wrong assumptions:
        
        - `getScrollableTracksViewportWidth()` tells JScrollPane whether to stretch the canvas horizontally (suppresses the horizontal scrollbar when all columns fit)
        - `getScrollableUnitIncrement()` makes each scrollbar arrow click advance exactly one `rowHeight` — one row at a time
        - `getScrollableBlockIncrement()` makes Page Up/Down move exactly one viewport height
        - `getPreferredScrollableViewportSize()` is what `pack()` uses — returning a compact 400px height rather than 32,000px prevents the JInternalFrame sizing bug we fixed earlier
        
        The `null` layout is the final piece. Normal layout managers position children relative to each other in sequence. We need the opposite — each slot at an *absolute* y coordinate we compute ourselves. `null` layout means "I'll call `setBounds()` directly, don't touch the positions."
        
        The short code is deceptive because the heavy lifting is done by JScrollPane's viewport mechanism, which was already written in 1997. VirtualCanvas contributes about 30 lines, but those 30 lines precisely exploit 25 years of battle-tested scrolling infrastructure. It's less "clever algorithm" and more "finding the exact seam in the existing architecture where a small amount of code does disproportionate work."
     */
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
            int vpWidth = scrollPane.getViewport().getWidth();
            int w = Math.max(totalCanvasWidth(), vpWidth > 0 ? vpWidth : 400);

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
            int vpWidth = scrollPane.getViewport().getWidth();
            int w = Math.max(totalCanvasWidth(), vpWidth > 0 ? vpWidth : 400);
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
            return totalCanvasWidth() <= scrollPane.getViewport().getWidth();
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
