package org.jwellman.demo.calendar;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Renders a single calendar day within a CalendarWeekRowPanel.
 *
 * Layout (top to bottom):
 *   TOP_HEIGHT px  — day-number badge (top-right) + primary event label
 *   remainder      — row of small colored chip buttons, one per additional event
 */
public class DayCellPanel extends JPanel {

    private static final int   TOP_HEIGHT  = 36;
    private static final int   CHIP_SIZE   = 14;
    private static final Color BG_IN_YEAR  = Color.WHITE;
    private static final Color BG_PAST     = new Color(0xE8, 0xF0, 0xFF);
    private static final Color BG_WEEKEND  = new Color(0xF4, 0xF4, 0xF4);
    private static final Color BG_OUT_YEAR = new Color(0xF0, 0xF0, 0xF0);
    private static final Color BORDER_CLR   = new Color(0xD0, 0xD0, 0xD0);
    private static final Color TODAY_ACCENT = new Color(0x18, 0x76, 0xD2);
    private static final LocalDate TODAY    = LocalDate.now();

    private static final class Borders {
        private static final javax.swing.border.Border DAY_CELL = BorderFactory.createMatteBorder(0, 1, 1, 0, BORDER_CLR);
        private static final javax.swing.border.Border TODAY_CELL = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(3, 0, 0, 0, TODAY_ACCENT),
            BorderFactory.createMatteBorder(0, 1, 1, 0, BORDER_CLR));
        private static final javax.swing.border.Border PRIMARY_LABEL = BorderFactory.createEmptyBorder(1, 3, 1, 3);
    }

    private static final class Fonts {
        private static final Font DAY_NUMBER       = new Font("SansSerif", Font.PLAIN, 11);
        private static final Font DAY_NUMBER_TODAY = new Font("SansSerif", Font.BOLD,  11);
        private static final Font PRIMARY_LABEL    = new Font("SansSerif", Font.PLAIN, 11);
    }
    
    private static final class Colors {
        private static final Color DAY_NUMBER = new Color(0x80, 0x80, 0x80);
    }
    
    private static final class Dimensions {
        private static final Dimension CHIPS = new Dimension(CHIP_SIZE, CHIP_SIZE);
    }

    private final JLabel dayNumberLabel;
    private final JLabel primaryLabel;
    private final JPanel chipsPanel;
    private final Consumer<CalendarEvent> onEventClicked;

    private CalendarEvent primaryEvent;

    public DayCellPanel(Consumer<CalendarEvent> onEventClicked) {
        this.onEventClicked = onEventClicked;
        setLayout(null);
        setOpaque(true);
        setBorder(Borders.DAY_CELL);

        dayNumberLabel = new JLabel();
        dayNumberLabel.setFont(Fonts.DAY_NUMBER);
        dayNumberLabel.setForeground(Colors.DAY_NUMBER);
        dayNumberLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        add(dayNumberLabel);

        primaryLabel = new JLabel();
        primaryLabel.setFont(Fonts.PRIMARY_LABEL);
        primaryLabel.setBorder(Borders.PRIMARY_LABEL);
        primaryLabel.setOpaque(false);
        primaryLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        primaryLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (primaryEvent != null) {
                    onEventClicked.accept(primaryEvent);
                }
            }
        });
        add(primaryLabel);

        chipsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 1));
        chipsPanel.setOpaque(false);
        add(chipsPanel);
    }

    @Override
    public void doLayout() {
        int w = getWidth();
        int h = getHeight();
        int botH = h - TOP_HEIGHT;

        dayNumberLabel.setBounds(w - 22, 1, 20, 12);
        primaryLabel.setBounds(2, TOP_HEIGHT / 2 - 9, w - 26, 18);
        chipsPanel.setBounds(2, TOP_HEIGHT, w - 4, Math.max(0, botH));
    }

    public void populate(DayData data) {
        chipsPanel.removeAll();
        primaryEvent = null;

        if (data == null || !data.isInYear()) {
            setBackground(BG_OUT_YEAR);
            primaryLabel.setText("");
            primaryLabel.setOpaque(false);
            dayNumberLabel.setText(data != null ? String.valueOf(data.getDate().getDayOfMonth()) : "");
            revalidate();
            repaint();
            return;
        }

        DayOfWeek dow = data.getDate().getDayOfWeek();
        boolean isWeekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
        boolean isPastWeekday = !isWeekend && data.getDate().isBefore(TODAY);
        Color bg = isWeekend ? BG_WEEKEND : isPastWeekday ? BG_PAST : BG_IN_YEAR;
        setBackground(bg);

        boolean isToday = data.getDate().equals(TODAY);
        setBorder(isToday ? Borders.TODAY_CELL : Borders.DAY_CELL);
        dayNumberLabel.setText(String.valueOf(data.getDate().getDayOfMonth()));
        dayNumberLabel.setFont(isToday ? Fonts.DAY_NUMBER_TODAY : Fonts.DAY_NUMBER);
        dayNumberLabel.setForeground(isToday ? TODAY_ACCENT : Colors.DAY_NUMBER);

        List<CalendarEvent> events = data.getEvents();
        if (events.isEmpty()) {
            primaryLabel.setText("");
            primaryLabel.setOpaque(false);
        } else {
            primaryEvent = events.get(0);
            primaryLabel.setText(primaryEvent.getName());
            primaryLabel.setBackground(primaryEvent.getCategory().getColor());
            primaryLabel.setForeground(Color.WHITE);
            primaryLabel.setOpaque(true);

            for (int i = 1; i < events.size(); i++) {
                chipsPanel.add(makeChip(events.get(i)));
            }
        }

        revalidate();
        repaint();
    }

    public void clear() {
        primaryEvent = null;
        primaryLabel.setText("");
        primaryLabel.setOpaque(false);
        dayNumberLabel.setText("");
        dayNumberLabel.setFont(Fonts.DAY_NUMBER);
        dayNumberLabel.setForeground(Colors.DAY_NUMBER);
        chipsPanel.removeAll();
        setBorder(Borders.DAY_CELL);
        setBackground(BG_IN_YEAR);
    }

    private JButton makeChip(CalendarEvent event) {
        JButton chip = new JButton();
        chip.setPreferredSize(Dimensions.CHIPS);
        chip.setBackground(event.getCategory().getColor());
        chip.setOpaque(true);
        chip.setContentAreaFilled(true);
        chip.setBorderPainted(false);
        chip.setFocusPainted(false);
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.setToolTipText(event.getName() + " (" + event.getCategory().getDisplayName() + ")");
        chip.addActionListener(e -> onEventClicked.accept(event));
        return chip;
    }

}
