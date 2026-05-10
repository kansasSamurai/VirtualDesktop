package org.jwellman.swing.grid;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
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
 * Supports:
 *  - Alternating-row colors and fnd-style tag-based backgrounds
 *  - Row selection via ListSelectionModel (system selection colors)
 *  - Tree indentation: first-cell left padding = depth * 16 px
 *  - Expand/collapse toggle: ▶ / ▼ prefix on first cell when hasChildren is true
 *
 * A single combined MouseAdapter (mousePressed) handles both selection and
 * expand/collapse. It is attached in bind() and removed at the START of the
 * next bind() call — guarding against double-attach when a selection change
 * synchronously triggers refresh() before the event has finished dispatching.
 */
public class StandardRowPanel extends JPanel implements Recyclable {

    private static final Color EVEN_BG    = Color.WHITE;
    private static final Color ODD_BG     = new Color(0xF4F6FA);
    private static final Color BORDER_COL = new Color(0xE0E0E0);
    private static final Color WARN_BG    = new Color(0xFFF8DC);
    private static final Color ERROR_BG   = new Color(0xFFDCDC);

    private final List<ColumnDef>   columns;
    private final List<JLabel>      cells = new ArrayList<>();
    private final Runnable          expandCollapseAction; // null = no tree support
    private final ListSelectionModel selectionModel;      // null = no selection support
    private MouseAdapter rowListener; // covers both selection and expand/collapse

    public StandardRowPanel(List<ColumnDef> columns) {
        this(columns, null, null);
    }

    public StandardRowPanel(List<ColumnDef> columns, Runnable expandCollapseAction) {
        this(columns, expandCollapseAction, null);
    }

    public StandardRowPanel(List<ColumnDef> columns,
                            Runnable expandCollapseAction,
                            ListSelectionModel selectionModel) {
        this.columns = columns;
        this.expandCollapseAction = expandCollapseAction;
        this.selectionModel = selectionModel;
        setLayout(new GridLayout(1, columns.size()));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL));
        setOpaque(true);

        for (ColumnDef col : columns) {
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
        //    is triggered synchronously during a mousePressed handler.
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

        // ④ Selection overrides everything — uses system table colors
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
            }
            lbl.setFont(base);
            lbl.setForeground(fgColor);
        }

        // ⑥ Single combined listener — mousePressed handles both selection and tree
        final GridRow        capturedRow = row;
        final int            capturedIdx = rowIndex;
        final ListSelectionModel sm      = selectionModel;
        final Runnable       expandAct   = expandCollapseAction;

        rowListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Selection
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
                    // SmartGrid's ListSelectionListener calls refresh() here (if !adjusting)
                }
                // Expand/collapse for tree parent rows
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
