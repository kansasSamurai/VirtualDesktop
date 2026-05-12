package org.jwellman.swing.grid;

import java.awt.Color;
import java.awt.Font;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Full-width row renderer for group header rows ({@link GridRow#isGroupHeader()} ==
 * true). Spans the entire data column area as a single unified cell, showing the
 * group label in bold. Registered by SmartGrid under the reserved type key
 * {@code "group-header"}.
 *
 * <p>Group header rows are not selectable — the {@code isGroupHeader} guard in
 * StandardRowPanel and EditableRowPanel prevents selection highlight from applying.
 */
public class GroupHeaderRowPanel extends JPanel implements Recyclable {

    private static final Color BG  = new Color(0xDDE3EF);
    private static final Color FG  = new Color(0x2C3E50);

    private final JLabel label = new JLabel();

    public GroupHeaderRowPanel() {
        setLayout(null);
        setBackground(BG);
        setOpaque(true);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setForeground(FG);
        add(label);
    }

    @Override
    public void prepareForReuse() {
        label.setText("");
    }

    @Override
    public void bind(GridRow row, int rowIndex) {
        label.setBounds(8, 0, Math.max(0, getWidth() - 16), getHeight());
        Object firstVal = null;
        for (Map.Entry<String, Object> e : row.getData().entrySet()) {
            if (e.getValue() != null && !e.getValue().toString().isEmpty()) {
                firstVal = e.getValue();
                break;
            }
        }
        label.setText(firstVal != null ? firstVal.toString() : "");
    }
}
