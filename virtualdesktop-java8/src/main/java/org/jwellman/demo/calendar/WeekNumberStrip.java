package org.jwellman.demo.calendar;

import java.awt.Color;
import java.awt.Font;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.jwellman.swing.grid.GridRow;
import org.jwellman.swing.grid.Strip;

/**
 * Left-gutter strip that shows the ISO week-of-year number for each calendar
 * week row.  Reads the DayData stored under the "mon" key to determine the
 * week's Monday date, then formats it as "W&lt;n&gt;".
 */
public class WeekNumberStrip implements Strip {

    static final int WIDTH = 34;

    private static final Color BG        = new Color(0xE8, 0xE8, 0xE8);
    private static final Color FG        = new Color(0x80, 0x80, 0x80);
    private static final Color BDR       = new Color(0xD0, 0xD0, 0xD0);
    private static final Color ACCENT    = new Color(0xE9, 0x1E, 0x8C);
    private static final Color ACCENT_FG = Color.WHITE;
    private static final LocalDate TODAY = LocalDate.now();

    private static final javax.swing.border.Border SLOT_BORDER = BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 1, BDR),
        BorderFactory.createEmptyBorder(0, 2, 0, 2));
    private static final javax.swing.border.Border HEADER_BORDER =
        BorderFactory.createMatteBorder(0, 0, 0, 1, BDR);

    private final Color headerBg;
    private final boolean[] highlightOn;
    private JLabel[] slots;
    private boolean visible = true;

    public WeekNumberStrip(Color headerBg, boolean[] highlightOn) {
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
        Object mondayVal = row.get("mon");
        if (mondayVal instanceof DayData) {
            LocalDate monday = ((DayData) mondayVal).getDate();
            int weekNum = monday.get(WeekFields.ISO.weekOfWeekBasedYear());
            lbl.setText("W" + weekNum);
            boolean isCurrentWeek = highlightOn[0] && !TODAY.isBefore(monday) && !TODAY.isAfter(monday.plusDays(6));
            lbl.setBackground(isCurrentWeek ? ACCENT : BG);
            lbl.setForeground(isCurrentWeek ? ACCENT_FG : FG);
            lbl.setFont(isCurrentWeek
                ? lbl.getFont().deriveFont(Font.BOLD)
                : lbl.getFont().deriveFont(Font.PLAIN));
        } else {
            lbl.setText("");
            lbl.setBackground(BG);
            lbl.setForeground(FG);
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
        JLabel lbl = new JLabel("Wk");
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
