package org.jwellman.demo.calendar;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.jwellman.swing.grid.GridRow;
import org.jwellman.swing.grid.Strip;

/**
 * Left-gutter strip that displays the sprint number for each calendar week.
 *
 * Numbering convention used in the demo:
 *   Row 0            → Sprint 27  (partial sprint — one week)
 *   Rows 1–2         → Sprint 28  (first full 2-week sprint)
 *   Rows 3–4         → Sprint 29
 *   …and so on
 *
 * Each sprint is tinted with a distinct soft color drawn from a 7-color
 * palette that cycles, giving a weather-map-style cool→warm progression
 * without any hard alert colors.
 */
public class SprintStrip implements Strip {

    static final int WIDTH = 52;

    private static final Color FG  = new Color(0x50, 0x50, 0x60);
    private static final Color BDR = new Color(0xD0, 0xD0, 0xD0);

    /** Soft weather-map palette: lavender → teal → sage → chartreuse → amber → peach → rose */
    private static final Color[] PALETTE = {
        new Color(0xDA, 0xDA, 0xF5), // lavender
        new Color(0xCE, 0xEC, 0xEC), // teal
        new Color(0xCE, 0xEC, 0xCE), // sage
        new Color(0xEC, 0xF5, 0xCE), // chartreuse
        new Color(0xF5, 0xF0, 0xCE), // amber
        new Color(0xF5, 0xDE, 0xCE), // peach
        new Color(0xF5, 0xCE, 0xDF), // rose
    };

    private static final javax.swing.border.Border SLOT_BORDER = BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 1, BDR),
        BorderFactory.createEmptyBorder(0, 4, 0, 4));
    private static final javax.swing.border.Border HEADER_BORDER =
        BorderFactory.createMatteBorder(0, 0, 0, 1, BDR);

    private final Color headerBg;
    private JLabel[] slots;
    private boolean visible = true;

    public SprintStrip(Color headerBg) {
        this.headerBg = headerBg;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void reallocate(int slotCount, JPanel canvas) {
        if (slots != null) {
            for (JLabel lbl : slots) {
                canvas.remove(lbl);
            }
        }
        if (slotCount == 0) {
            slots = null;
            return;
        }
        slots = new JLabel[slotCount];
        for (int i = 0; i < slotCount; i++) {
            JLabel lbl = new JLabel();
            lbl.setOpaque(true);
            lbl.setForeground(FG);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, lbl.getFont().getSize2D() - 1f));
            lbl.setBorder(SLOT_BORDER);
            slots[i] = lbl;
            canvas.add(lbl);
        }
    }

    @Override
    public void bind(int slotIdx, int xOffset, int y, int rowHeight, int modelIdx, GridRow row) {
        if (slots == null || slotIdx >= slots.length) {
            return;
        }
        JLabel lbl = slots[slotIdx];
        lbl.setText(String.valueOf(sprintForRow(modelIdx)));
        lbl.setBackground(PALETTE[paletteIndexForRow(modelIdx)]);
        lbl.setBounds(xOffset, y, WIDTH, rowHeight);
        lbl.setVisible(true);
    }

    @Override
    public void hide(int slotIdx) {
        if (slots != null && slotIdx < slots.length) {
            slots[slotIdx].setVisible(false);
        }
    }

    @Override
    public JComponent headerCell(int height) {
        JLabel lbl = new JLabel("Sprint");
        lbl.setOpaque(true);
        lbl.setBackground(headerBg);
        lbl.setForeground(Color.WHITE);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setBorder(HEADER_BORDER);
        return lbl;
    }

    @Override
    public JComponent filterSpacer(int height) {
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        return spacer;
    }

    @Override
    public JComponent footerSpacer(int height) {
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        return spacer;
    }

    // Row 0 = Sprint 27 (partial); row 1 starts Sprint 28, then every 2 rows increments.
    private static int sprintForRow(int modelIdx) {
        if (modelIdx == 0) {
            return 27;
        }
        return 28 + ((modelIdx - 1) / 2);
    }

    private static int paletteIndexForRow(int modelIdx) {
        if (modelIdx == 0) {
            return 0;
        }
        return (1 + ((modelIdx - 1) / 2)) % PALETTE.length;
    }
}
