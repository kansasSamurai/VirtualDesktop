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
import javax.swing.UIManager;

/**
 * Default row component: a JPanel with one JLabel per column.
 *
 * Supports:
 *  - Alternating-row colors and fnd-style tag-based backgrounds
 *  - Tree indentation: first-cell left padding = depth * 16 px
 *  - Expand/collapse toggle: ▶ / ▼ prefix on first cell when hasChildren is true;
 *    clicking the row calls expandCollapseAction (injected at construction time)
 */
public class StandardRowPanel extends JPanel implements Recyclable {

    private static final Color EVEN_BG    = Color.WHITE;
    private static final Color ODD_BG     = new Color(0xF4F6FA);
    private static final Color BORDER_COL = new Color(0xE0E0E0);
    private static final Color WARN_BG    = new Color(0xFFF8DC);
    private static final Color ERROR_BG   = new Color(0xFFDCDC);

    private final List<ColumnDef> columns;
    private final List<JLabel> cells = new ArrayList<>();
    private final Runnable expandCollapseAction; // null = no tree support
    private MouseAdapter expandListener;          // tracked for removal in prepareForReuse

    public StandardRowPanel(List<ColumnDef> columns) {
        this(columns, null);
    }

    public StandardRowPanel(List<ColumnDef> columns, Runnable expandCollapseAction) {
        this.columns = columns;
        this.expandCollapseAction = expandCollapseAction;
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
        // Remove expand listener (ghost-event prevention).
        if (expandListener != null) {
            removeMouseListener(expandListener);
            expandListener = null;
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
        // Base alternating color
        setBackground(rowIndex % 2 == 0 ? EVEN_BG : ODD_BG);

        // Tag-based background overrides
        String style = row.getTag("fnd-style");
        if ("warning-glow".equals(style) || "warning".equals(style)) {
            setBackground(WARN_BG);
        } else if ("error".equals(style)) {
            setBackground(ERROR_BG);
        }

        Font base = UIManager.getFont("Label.font");
        Font font = row.isSelected() ? base.deriveFont(Font.BOLD) : base;

        // Tree: indentation and expand/collapse prefix on first cell
        int indent = row.getDepth() * 16;
        String prefix = buildTreePrefix(row);

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
            lbl.setFont(font);
        }

        // Attach expand/collapse listener for tree parent rows
        if (row.isHasChildren() && expandCollapseAction != null) {
            final GridRow captured = row;
            expandListener = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    captured.setExpanded(!captured.isExpanded());
                    expandCollapseAction.run();
                }
            };
            addMouseListener(expandListener);
        }
    }

    private String buildTreePrefix(GridRow row) {
        if (row.isHasChildren()) {
            return row.isExpanded() ? "▼ " : "▶ "; // ▼ / ▶
        }
        return row.getDepth() > 0 ? "  " : ""; // align leaf nodes with siblings
    }
}
