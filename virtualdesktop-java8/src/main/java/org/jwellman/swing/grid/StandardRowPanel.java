package org.jwellman.swing.grid;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;

/**
 * Default row component: a JPanel with one JLabel per column.
 *
 * Cell widths are set from a shared {@code int[] columnWidths} array owned by
 * SmartGrid. All row panels in the pool reference the same array, so when
 * SmartGrid recomputes widths on resize the next bind() call picks up the new
 * values automatically — guaranteeing pixel-exact column alignment across header,
 * rows, and footer.
 *
 * Uses {@code null} layout; cell bounds are set explicitly in bind().
 */
public class StandardRowPanel extends JPanel implements Recyclable {

    private static final Color EVEN_BG    = Color.WHITE;
    private static final Color ODD_BG     = new Color(0xF4F6FA);
    private static final Color BORDER_COL = new Color(0xE0E0E0);
    private static final Color WARN_BG    = new Color(0xFFF8DC);
    private static final Color ERROR_BG   = new Color(0xFFDCDC);

    private final List<ColumnDef>    columns;
    private final List<JLabel>       cells = new ArrayList<>();
    private final Runnable           expandCollapseAction;
    private final ListSelectionModel selectionModel;
    private final int[]              columnWidths; // shared mutable reference from SmartGrid
    private MouseAdapter             rowListener;

    public StandardRowPanel(List<ColumnDef> columns) {
        this(columns, null, null, null);
    }

    public StandardRowPanel(List<ColumnDef> columns, Runnable expandCollapseAction) {
        this(columns, expandCollapseAction, null, null);
    }

    public StandardRowPanel(List<ColumnDef> columns,
                            Runnable expandCollapseAction,
                            ListSelectionModel selectionModel) {
        this(columns, expandCollapseAction, selectionModel, null);
    }

    public StandardRowPanel(List<ColumnDef> columns,
                            Runnable expandCollapseAction,
                            ListSelectionModel selectionModel,
                            int[] columnWidths) {
        this.columns = columns;
        this.expandCollapseAction = expandCollapseAction;
        this.selectionModel = selectionModel;
        this.columnWidths = columnWidths; // shared reference — never copy
        setLayout(null);                  // absolute positioning; bounds set in bind()
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL));
        setOpaque(true);

        for (int i = 0; i < columns.size(); i++) {
            JLabel lbl = new JLabel();
            lbl.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            cells.add(lbl);
            add(lbl); // bounds assigned in bind(), not by a layout manager
        }
    }

    @Override
    public void prepareForReuse() {
        if (rowListener != null) {
            removeMouseListener(rowListener);
            rowListener = null;
        }
        for (JLabel lbl : cells) {
            lbl.setText("");
            lbl.setFont(UIManager.getFont("Label.font"));
            lbl.setForeground(UIManager.getColor("Label.foreground"));
            lbl.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        }
        setBackground(EVEN_BG);
    }

    @Override
    public void bind(GridRow row, int rowIndex) {

        // ① Remove stale listener first — prevents double-attach when refresh()
        //    fires synchronously inside a mousePressed handler.
        if (rowListener != null) {
            removeMouseListener(rowListener);
            rowListener = null;
        }

        // ② Base background: alternating rows
        setBackground(rowIndex % 2 == 0 ? EVEN_BG : ODD_BG);

        // ③ Tag-based background override
        String style = row.getTag("fnd-style");
        if ("warning-glow".equals(style) || "warning".equals(style)) {
            setBackground(WARN_BG);
        } else if ("error".equals(style)) {
            setBackground(ERROR_BG);
        }

        // ④ Selection overrides everything
        boolean selected = selectionModel != null
                           && selectionModel.isSelectedIndex(rowIndex);
        final Color fgColor;
        if (selected) {
            setBackground(UIManager.getColor("Table.selectionBackground"));
            fgColor = UIManager.getColor("Table.selectionForeground");
        } else {
            fgColor = UIManager.getColor("Label.foreground");
        }

        // ⑤ Tree indentation + expand/collapse prefix on first cell
        int indent    = row.getDepth() * 16;
        String prefix = buildTreePrefix(row);
        Font base = UIManager.getFont("Label.font");

        for (int i = 0; i < cells.size(); i++) {
            Object val = row.get(columns.get(i).getKey());
            JLabel lbl = cells.get(i);
            String text = val != null ? val.toString() : "";
            if (i == 0 && !prefix.isEmpty()) {
                lbl.setText(prefix + text);
                lbl.setBorder(BorderFactory.createEmptyBorder(2, 8 + indent, 2, 8));
            } else {
                lbl.setText(text);
                lbl.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            }
            lbl.setFont(base);
            lbl.setForeground(fgColor);
        }

        // ⑥ Set absolute cell bounds from shared columnWidths array.
        //    The panel's own bounds (and therefore getHeight()) have already been
        //    set by SmartGrid.refresh() before bind() is called.
        int cellH = getHeight() > 0 ? getHeight() : 32;
        int x = 0;
        for (int i = 0; i < cells.size(); i++) {
            int w = (columnWidths != null && i < columnWidths.length) ? columnWidths[i] : 80;
            cells.get(i).setBounds(x, 0, w, cellH);
            x += w;
        }

        // ⑦ Single combined listener — mousePressed handles selection and tree expand
        final GridRow        capturedRow = row;
        final int            capturedIdx = rowIndex;
        final ListSelectionModel sm      = selectionModel;
        final Runnable       expandAct   = expandCollapseAction;

        rowListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (sm != null) {
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

    private String buildTreePrefix(GridRow row) {
        if (row.isHasChildren()) {
            return row.isExpanded() ? "▼ " : "▶ ";
        }
        return row.getDepth() > 0 ? "  " : "";
    }
}
