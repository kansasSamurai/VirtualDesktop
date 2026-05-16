package org.jwellman.swing.grid;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Gutter strip that displays 1-based row numbers on the far left of the grid.
 *
 * The number shown is {@code modelIdx + 1}, where {@code modelIdx} is the
 * absolute position in the (filtered / paged) model — so row numbers correctly
 * track sorting, filtering, and pagination without any extra logic here.
 *
 * Off by default; enable via {@link SmartGrid#setRowNumbersVisible(boolean)}.
 */
class RowNumberStrip implements Strip {

    static final int WIDTH = 42;

    // Light theme
    private static final Color BG_LIGHT   = new Color(0xE8E8E8);
    private static final Color FG_LIGHT   = new Color(0x888888);
    private static final Color BDR_LIGHT  = new Color(0xD0D0D0);

    // Dark theme
    private static final Color BG_DARK    = new Color(0x3A3C40);
    private static final Color FG_DARK    = new Color(0x8A8A8A);
    private static final Color BDR_DARK   = new Color(0x4B4E52);

    private final boolean darkTheme;
    private final Color   headerBg;
    private JLabel[]      slots;
    private boolean       visible = false;

    RowNumberStrip(boolean darkTheme, Color headerBg) {
        this.darkTheme = darkTheme;
        this.headerBg  = headerBg;
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
        Color bg  = darkTheme ? BG_DARK  : BG_LIGHT;
        Color fg  = darkTheme ? FG_DARK  : FG_LIGHT;
        Color bdr = darkTheme ? BDR_DARK : BDR_LIGHT;
        slots = new JLabel[slotCount];
        for (int i = 0; i < slotCount; i++) {
            JLabel lbl = new JLabel();
            lbl.setOpaque(true);
            lbl.setBackground(bg);
            lbl.setForeground(fg);
            lbl.setHorizontalAlignment(SwingConstants.RIGHT);
            lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, bdr),
                BorderFactory.createEmptyBorder(0, 0, 0, 6)));
            lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN,
                lbl.getFont().getSize2D() - 1f));
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
        lbl.setText(String.valueOf(modelIdx + 1));
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
        JLabel lbl = new JLabel();
        lbl.setOpaque(true);
        lbl.setBackground(headerBg);
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
}
