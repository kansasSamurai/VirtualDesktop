package org.jwellman.swing.grid;

import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;

/**
 * Edit-mode row panel — one JTextField per column.
 *
 * On focus-lost each field writes its text directly back to the bound
 * {@link GridRow} via {@code row.put(key, text)}, implementing bidirectional
 * data flow without an intermediate buffer.
 *
 * Registered automatically by {@link SmartGrid#setEditable(boolean)} under the
 * reserved type key {@code "edit"}.
 */
public class EditableRowPanel extends JPanel implements Recyclable {

    // Light theme
    private static final Color EVEN_BG         = Color.WHITE;
    private static final Color ODD_BG          = new Color(0xF4F6FA);
    private static final Color BORDER_COL      = new Color(0xE0E0E0);

    // Dark theme
    private static final Color EVEN_BG_DARK    = new Color(0x2B2D30);
    private static final Color ODD_BG_DARK     = new Color(0x313438);
    private static final Color BORDER_COL_DARK = new Color(0x4B4E52);

    private final List<ColumnDef>    columns;
    private final ListSelectionModel selectionModel;
    private final int[]              columnWidths;   // shared mutable reference
    private final boolean            darkTheme;
    private final List<JTextField>   fields = new ArrayList<>();
    private FocusListener[]          focusListeners;
    private MouseAdapter             rowListener;

    public EditableRowPanel(List<ColumnDef> columns,
                            ListSelectionModel selectionModel,
                            int[] columnWidths,
                            boolean darkTheme) {
        this.columns        = columns;
        this.selectionModel = selectionModel;
        this.columnWidths   = columnWidths;
        this.darkTheme      = darkTheme;
        focusListeners = new FocusListener[columns.size()];
        setLayout(null);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                darkTheme ? BORDER_COL_DARK : BORDER_COL));
        setOpaque(true);

        for (int i = 0; i < columns.size(); i++) {
            JTextField field = new JTextField();
            field.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            fields.add(field);
            add(field);
        }
    }

    @Override
    public void prepareForReuse() {
        if (rowListener != null) {
            removeMouseListener(rowListener);
            rowListener = null;
        }
        for (int i = 0; i < fields.size(); i++) {
            if (focusListeners[i] != null) {
                fields.get(i).removeFocusListener(focusListeners[i]);
                focusListeners[i] = null;
            }
            fields.get(i).setText("");
        }
        setBackground(darkTheme ? EVEN_BG_DARK : EVEN_BG);
    }

    @Override
    public void bind(GridRow row, int rowIndex) {
        if (rowListener != null) {
            removeMouseListener(rowListener);
            rowListener = null;
        }

        boolean selected = selectionModel != null && selectionModel.isSelectedIndex(rowIndex);
        if (selected) {
            setBackground(UIManager.getColor("Table.selectionBackground"));
        } else {
            setBackground(rowIndex % 2 == 0
                    ? (darkTheme ? EVEN_BG_DARK : EVEN_BG)
                    : (darkTheme ? ODD_BG_DARK  : ODD_BG));
        }

        int cellH = getHeight() > 0 ? getHeight() : 32;
        int x = 0;
        for (int i = 0; i < fields.size(); i++) {
            ColumnDef col = columns.get(i);
            int w = (columnWidths != null && i < columnWidths.length) ? columnWidths[i] : 80;
            JTextField field = fields.get(i);

            // Remove previous focus listener before populating to avoid spurious write-back
            if (focusListeners[i] != null) {
                field.removeFocusListener(focusListeners[i]);
                focusListeners[i] = null;
            }

            Object val = row.get(col.getKey());
            field.setText(val != null ? val.toString() : "");
            field.setBounds(x + 2, 2, w - 4, cellH - 4);

            final String key = col.getKey();
            final Object originalVal = val;
            final JTextField capturedField = field;
            FocusListener fl = new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    row.put(key, coerce(capturedField.getText(), originalVal));
                }
            };
            field.addFocusListener(fl);
            focusListeners[i] = fl;

            x += w;
        }

        final GridRow        capturedRow = row;
        final int            capturedIdx = rowIndex;
        final ListSelectionModel sm      = selectionModel;

        rowListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (sm != null && !capturedRow.isGroupHeader()) {
                    if (e.isControlDown() || e.isMetaDown()) {
                        if (sm.isSelectedIndex(capturedIdx)) {
                            sm.removeSelectionInterval(capturedIdx, capturedIdx);
                        } else {
                            sm.addSelectionInterval(capturedIdx, capturedIdx);
                        }
                    } else if (e.isShiftDown()) {
                        int anchor = sm.getAnchorSelectionIndex();
                        if (anchor < 0) {
                            anchor = capturedIdx;
                        }
                        sm.setSelectionInterval(anchor, capturedIdx);
                    } else {
                        sm.setSelectionInterval(capturedIdx, capturedIdx);
                    }
                }
            }
        };
        addMouseListener(rowListener);
    }

    /**
     * Tries to parse {@code text} back to the same type as {@code original}.
     * Falls back to returning the raw String if parsing fails or the original
     * type is not a recognized numeric type.
     */
    private static Object coerce(String text, Object original) {
        if (original instanceof Integer) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException e) {
                return text;
            }
        }
        if (original instanceof Long) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException e) {
                return text;
            }
        }
        if (original instanceof Double) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException e) {
                return text;
            }
        }
        if (original instanceof Float) {
            try {
                return Float.parseFloat(text.trim());
            } catch (NumberFormatException e) {
                return text;
            }
        }
        return text;
    }
}
