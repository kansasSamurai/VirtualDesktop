package org.jwellman.swing.grid;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;

/** Default header cell: bold JLabel with white text. Background comes from the header panel. */
public class DefaultHeaderCellRenderer implements HeaderCellRenderer {

    @Override
    public JComponent render(ColumnDef col) {
        JLabel lbl = new JLabel(col.getHeader());
        lbl.setForeground(Color.WHITE);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        lbl.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        return lbl;
    }
}
