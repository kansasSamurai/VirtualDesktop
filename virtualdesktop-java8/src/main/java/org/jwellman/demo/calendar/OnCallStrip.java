package org.jwellman.demo.calendar;

import java.awt.Color;
import java.awt.Font;
import java.time.LocalDate;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.jwellman.swing.grid.GridRow;
import org.jwellman.swing.grid.Strip;

/**
 * Left-gutter strip that displays the name of the person on call for each
 * calendar week.  Reads the {@code "oncall"} String stored in the GridRow
 * by {@link org.jwellman.demo.CalendarDemo}.
 */
public class OnCallStrip implements Strip {

    static final int WIDTH = 70;

    private static final Color BG     = new Color(0xE8, 0xE8, 0xE8);
    private static final Color FG     = new Color(0x80, 0x80, 0x80);
    private static final Color BDR    = new Color(0xD0, 0xD0, 0xD0);
    private static final Color ACCENT = new Color(0xE9, 0x1E, 0x8C);
    private static final LocalDate TODAY = LocalDate.now();

    private static final javax.swing.border.Border SLOT_BORDER = BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 1, BDR),
        BorderFactory.createEmptyBorder(4, 4, 0, 4));
    private static final javax.swing.border.Border ACCENT_SLOT_BORDER = BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(4, 0, 0, 0, ACCENT),
        BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 1, BDR),
            BorderFactory.createEmptyBorder(0, 4, 0, 4)));
    private static final javax.swing.border.Border HEADER_BORDER =
        BorderFactory.createMatteBorder(0, 0, 0, 1, BDR);

    private final Color headerBg;
    private final boolean[] highlightOn;
    private JLabel[] slots;
    private boolean visible = true;

    public OnCallStrip(Color headerBg, boolean[] highlightOn) {
        this.headerBg   = headerBg;
        this.highlightOn = highlightOn;
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
            lbl.setBackground(BG);
            lbl.setForeground(FG);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, lbl.getFont().getSize2D() - 1f));
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
        Object val = row.get("oncall");
        lbl.setText(val != null ? val.toString() : "");
        Object mondayVal = row.get("mon");
        if (mondayVal instanceof DayData) {
            LocalDate monday = ((DayData) mondayVal).getDate();
            boolean isCurrentWeek = highlightOn[0] && !TODAY.isBefore(monday) && !TODAY.isAfter(monday.plusDays(6));
            lbl.setBorder(isCurrentWeek ? ACCENT_SLOT_BORDER : SLOT_BORDER);
        }
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
        JLabel lbl = new JLabel("On Call");
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
}
