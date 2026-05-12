package org.jwellman.swing.grid;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;

/**
 * Default row component: a JPanel with one cell component per column.
 *
 * Cell rendering:
 *   Each column is rendered by looking up its {@code fndType} in the shared
 *   {@code cellRenderers} registry.  If a {@link CellRenderer} is registered,
 *   it controls both the component type and value display.  Otherwise the column
 *   falls back to a plain {@code JLabel} with optional tree-indent prefix.
 *
 * The {@code cellRenderers} map is a shared mutable reference owned by SmartGrid,
 * so renderers registered after construction are visible immediately.
 *
 * Cell widths are set from a shared {@code int[] columnWidths} array (also owned by
 * SmartGrid), guaranteeing pixel-exact alignment across header, rows, and footer.
 */
public class StandardRowPanel extends JPanel implements Recyclable {

    // Light theme
    private static final Color EVEN_BG     = Color.WHITE;
    private static final Color ODD_BG      = new Color(0xF4F6FA);
    private static final Color BORDER_COL  = new Color(0xE0E0E0);
    private static final Color WARN_BG     = new Color(0xFFF8DC);
    private static final Color ERROR_BG    = new Color(0xFFDCDC);

    // Dark theme
    private static final Color EVEN_BG_DARK    = new Color(0x2B2D30);
    private static final Color ODD_BG_DARK     = new Color(0x313438);
    private static final Color BORDER_COL_DARK = new Color(0x4B4E52);
    private static final Color WARN_BG_DARK    = new Color(0x4A3D00);
    private static final Color ERROR_BG_DARK   = new Color(0x4A1A1A);

    private final List<ColumnDef>           columns;
    private final List<JComponent>          cells = new ArrayList<>(); // JLabel or custom
    private final Runnable                  expandCollapseAction;
    private final ListSelectionModel        selectionModel;
    private final int[]                     columnWidths;   // shared mutable reference
    private final Map<String, CellRenderer> cellRenderers;  // shared mutable reference
    private final boolean                   darkTheme;
    private MouseAdapter                    rowListener;

    public StandardRowPanel(List<ColumnDef> columns) {
        this(columns, null, null, null, null, false);
    }

    public StandardRowPanel(List<ColumnDef> columns, Runnable expandCollapseAction) {
        this(columns, expandCollapseAction, null, null, null, false);
    }

    public StandardRowPanel(List<ColumnDef> columns,
                            Runnable expandCollapseAction,
                            ListSelectionModel selectionModel) {
        this(columns, expandCollapseAction, selectionModel, null, null, false);
    }

    public StandardRowPanel(List<ColumnDef> columns,
                            Runnable expandCollapseAction,
                            ListSelectionModel selectionModel,
                            int[] columnWidths) {
        this(columns, expandCollapseAction, selectionModel, columnWidths, null, false);
    }

    public StandardRowPanel(List<ColumnDef> columns,
                            Runnable expandCollapseAction,
                            ListSelectionModel selectionModel,
                            int[] columnWidths,
                            Map<String, CellRenderer> cellRenderers) {
        this(columns, expandCollapseAction, selectionModel, columnWidths, cellRenderers, false);
    }

    public StandardRowPanel(List<ColumnDef> columns,
                            Runnable expandCollapseAction,
                            ListSelectionModel selectionModel,
                            int[] columnWidths,
                            Map<String, CellRenderer> cellRenderers,
                            boolean darkTheme) {
        this.columns = columns;
        this.expandCollapseAction = expandCollapseAction;
        this.selectionModel = selectionModel;
        this.columnWidths = columnWidths;
        this.cellRenderers = cellRenderers;
        this.darkTheme = darkTheme;
        setLayout(null);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                darkTheme ? BORDER_COL_DARK : BORDER_COL));
        setOpaque(true);

        for (int i = 0; i < columns.size(); i++) {
            JLabel lbl = new JLabel();
            lbl.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            cells.add(lbl);
            add(lbl);
        }
    }

    @Override
    public void prepareForReuse() {
        if (rowListener != null) {
            removeMouseListener(rowListener);
            rowListener = null;
        }
        for (int i = 0; i < cells.size(); i++) {
            JComponent cell = cells.get(i);
            if (cell instanceof JLabel) {
                JLabel lbl = (JLabel) cell;
                lbl.setText("");
                lbl.setFont(UIManager.getFont("Label.font"));
                lbl.setForeground(UIManager.getColor("Label.foreground"));
                lbl.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            } else {
                // Custom renderer component — replace with a blank JLabel for next use
                remove(cell);
                JLabel blank = new JLabel();
                blank.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
                cells.set(i, blank);
                add(blank);
            }
        }
        setBackground(darkTheme ? EVEN_BG_DARK : EVEN_BG);
    }

    @Override
    public void bind(GridRow row, int rowIndex) {

        // ① Remove stale listener before anything else
        if (rowListener != null) {
            removeMouseListener(rowListener);
            rowListener = null;
        }

        // ② Background: alternating → tag override → selection
        setBackground(rowIndex % 2 == 0
                ? (darkTheme ? EVEN_BG_DARK : EVEN_BG)
                : (darkTheme ? ODD_BG_DARK  : ODD_BG));
        String style = row.getTag("fnd-style");
        if ("warning-glow".equals(style) || "warning".equals(style)) {
            setBackground(darkTheme ? WARN_BG_DARK : WARN_BG);
        } else if ("error".equals(style)) {
            setBackground(darkTheme ? ERROR_BG_DARK : ERROR_BG);
        }
        boolean selected = selectionModel != null && selectionModel.isSelectedIndex(rowIndex);
        final Color fgColor;
        if (selected) {
            setBackground(UIManager.getColor("Table.selectionBackground"));
            fgColor = UIManager.getColor("Table.selectionForeground");
        } else {
            fgColor = UIManager.getColor("Label.foreground");
        }

        Font base  = UIManager.getFont("Label.font");
        int cellH  = getHeight() > 0 ? getHeight() : 32;

        // ④ Render and position each cell
        int x = 0;
        for (int i = 0; i < cells.size(); i++) {
            ColumnDef col = columns.get(i);
            int w = (columnWidths != null && i < columnWidths.length) ? columnWidths[i] : 80;
            Object val = row.get(col.getKey());

            String fndType = col.getFndType();
            CellRenderer renderer = (cellRenderers != null && fndType != null)
                ? cellRenderers.get(fndType) : null;

            if (renderer != null) {
                // Custom renderer path — may swap the component type
                JComponent existing = cells.get(i);
                JComponent newCell  = renderer.render(col, val, row, existing);
                if (newCell != existing) {
                    remove(existing);
                    cells.set(i, newCell);
                    add(newCell);
                }
                newCell.setBounds(x, 0, w, cellH);
            } else {
                // Default JLabel path
                JLabel lbl = ensureLabel(i);
                String text = val != null ? val.toString() : "";
                lbl.setText(text);
                lbl.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
                lbl.setFont(base);
                lbl.setForeground(fgColor);
                lbl.setBounds(x, 0, w, cellH);
            }
            x += w;
        }

        // ⑤ Combined selection + expand/collapse listener
        final GridRow        capturedRow = row;
        final int            capturedIdx = rowIndex;
        final ListSelectionModel sm      = selectionModel;
        final Runnable       expandAct   = expandCollapseAction;

        rowListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (sm != null && !capturedRow.isGroupHeader()) {
                    if (e.isControlDown() || e.isMetaDown()) {
                        if (sm.isSelectedIndex(capturedIdx))
                            sm.removeSelectionInterval(capturedIdx, capturedIdx);
                        else
                            sm.addSelectionInterval(capturedIdx, capturedIdx);
                    } else if (e.isShiftDown()) {
                        int anchor = sm.getAnchorSelectionIndex();
                        if (anchor < 0) anchor = capturedIdx;
                        sm.setSelectionInterval(anchor, capturedIdx);
                    } else {
                        sm.setSelectionInterval(capturedIdx, capturedIdx);
                    }
                }
                if (capturedRow.isHasChildren() && expandAct != null) {
                    capturedRow.setExpanded(!capturedRow.isExpanded());
                    expandAct.run();
                }
            }
        };
        addMouseListener(rowListener);
    }

    /**
     * Returns the cell at {@code index} as a JLabel. If a custom renderer
     * previously placed a non-JLabel there, swaps it back to a blank JLabel.
     */
    private JLabel ensureLabel(int index) {
        JComponent cell = cells.get(index);
        if (cell instanceof JLabel) return (JLabel) cell;
        remove(cell);
        JLabel lbl = new JLabel();
        lbl.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        cells.set(index, lbl);
        add(lbl);
        return lbl;
    }


}
