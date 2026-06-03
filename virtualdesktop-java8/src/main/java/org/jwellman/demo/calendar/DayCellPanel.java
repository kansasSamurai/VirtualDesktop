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
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import org.jwellman.swing.SmartTransferHandler;

/**
 * Renders a single calendar day within a CalendarWeekRowPanel.
 *
 * Layout (top to bottom):
 *   TOP_HEIGHT px  — day-number badge (top-right) + primary event label
 *   remainder      — row of small colored chip buttons, one per additional event
 *
 * DnD: DayCellPanel owns a single unified TransferHandler that handles both
 * drag-export and drop-import. primaryLabel and chip buttons trigger the drag
 * via mousePressed but carry no TransferHandler themselves, so drops landing
 * anywhere on the panel — including over those child components — are handled
 * consistently by the panel's handler.
 */
@SuppressWarnings("serial")
public class DayCellPanel extends JPanel {

    private static final int   TOP_HEIGHT       = 36;
    private static final int   CHIP_SIZE        = 14;
    private static final Color BG_IN_YEAR       = Color.WHITE;
    private static final Color BG_PAST          = new Color(0xE8, 0xF0, 0xFF);
    private static final Color BG_WEEKEND       = new Color(0xF4, 0xF4, 0xF4);
    private static final Color BG_OUT_YEAR      = new Color(0xF0, 0xF0, 0xF0);
    private static final Color BORDER_CLR       = new Color(0xD0, 0xD0, 0xD0);
    private static final Color TODAY_ACCENT     = new Color(0xE9, 0x1E, 0x8C);
    private static final LocalDate TODAY        = LocalDate.now();
    private static final LocalDate WEEK_END     = TODAY.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    private static final String[]  MONTH_ABBR   = {"JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC"};

    private static final class Borders {
        private static final javax.swing.border.Border DAY_CELL       = BorderFactory.createMatteBorder(0, 1, 1, 0, BORDER_CLR);
        private static final javax.swing.border.Border DAY_CELL_TODAY = BorderFactory.createMatteBorder(2, 2, 2, 2, TODAY_ACCENT);
        private static final javax.swing.border.Border PRIMARY_LABEL  = BorderFactory.createEmptyBorder(1, 3, 1, 3);
    }

    private static final class Fonts {
        private static final Font DAY_NUMBER       = new Font("SansSerif", Font.PLAIN, 11);
        private static final Font DAY_NUMBER_TODAY = new Font("SansSerif", Font.BOLD,  13);
        private static final Font PRIMARY_LABEL    = new Font("SansSerif", Font.PLAIN, 11);
    }

    private static final class Colors {
        private static final Color DAY_NUMBER = new Color(0x80, 0x80, 0x80);
    }

    private static final class Dimensions {
        private static final Dimension CHIPS = new Dimension(CHIP_SIZE, CHIP_SIZE);
    }

    /** Payload used when dragging a CalendarEvent out of a DayCellPanel. */
    static class CalendarEventTransfer {
        final CalendarEvent event;
        final DayCellPanel source;

        CalendarEventTransfer(CalendarEvent event, DayCellPanel source) {
            this.event = event;
            this.source = source;
        }
    }

    private final JLabel dayNumberLabel;
    private final JLabel primaryLabel;
    private final JPanel chipsPanel;
    private final Consumer<CalendarEvent> onEventClicked;
    private final boolean[] highlightOn;

    private CalendarEvent primaryEvent;
    private CalendarEvent draggingEvent;
    private DayData currentData;
    private boolean isToday            = false;
    private boolean isFutureCurrentWeek = false;
    private boolean isFutureThisWeek   = false;

    public DayCellPanel(Consumer<CalendarEvent> onEventClicked, boolean[] highlightOn) {
        this.onEventClicked = onEventClicked;
        this.highlightOn    = highlightOn;
        setLayout(null);
        setOpaque(true);
        setBorder(Borders.DAY_CELL);
        setTransferHandler(new SmartTransferHandler(
            () -> draggingEvent != null
                ? new CalendarEventTransfer(draggingEvent, DayCellPanel.this) : null,
            this::onEventDropped,
            () -> draggingEvent = null
        ));

        dayNumberLabel = new JLabel();
        dayNumberLabel.setFont(Fonts.DAY_NUMBER);
        dayNumberLabel.setForeground(Colors.DAY_NUMBER);
        dayNumberLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        add(dayNumberLabel);

        primaryLabel = new JLabel();
        primaryLabel.setFont(Fonts.PRIMARY_LABEL);
        primaryLabel.setBorder(Borders.PRIMARY_LABEL);
        primaryLabel.setOpaque(false);
        primaryLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (primaryEvent != null) {
                    onEventClicked.accept(primaryEvent);
                    draggingEvent = primaryEvent;
                    getTransferHandler().exportAsDrag(DayCellPanel.this, e, TransferHandler.MOVE);
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

        int badgeH = (isToday || isFutureCurrentWeek) ? 20 : 12;
        int badgeY = (isToday || isFutureCurrentWeek) ? TOP_HEIGHT / 2 - badgeH / 2 : 1;
        dayNumberLabel.setBounds(w - 22, badgeY, 20, badgeH);

        primaryLabel.setBounds(2, TOP_HEIGHT / 2 - 9, w - 26, 18);
        chipsPanel.setBounds(2, TOP_HEIGHT, w - 4, Math.max(0, botH));
    }

    public void populate(DayData data) {
        currentData = data;
        chipsPanel.removeAll();
        primaryEvent         = null;
        isToday              = false;
        isFutureCurrentWeek  = false;
        isFutureThisWeek     = false;

        if (data == null || !data.isInYear()) {
            setBorder(Borders.DAY_CELL);
            setBackground(BG_OUT_YEAR);
            primaryLabel.setText("");
            primaryLabel.setOpaque(false);
            primaryLabel.setHorizontalAlignment(SwingConstants.LEFT);
            primaryLabel.setCursor(Cursor.getDefaultCursor());
            dayNumberLabel.setText(data != null ? dayBadgeText(data.getDate()) : "");
            dayNumberLabel.setOpaque(false);
            dayNumberLabel.setFont(Fonts.DAY_NUMBER);
            dayNumberLabel.setForeground(Colors.DAY_NUMBER);
            revalidate();
            repaint();
            return;
        }

        LocalDate date     = data.getDate();
        DayOfWeek dow      = date.getDayOfWeek();
        boolean isWeekend  = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
        boolean highlights = highlightOn[0];

        isToday             = highlights && date.equals(TODAY);
        isFutureCurrentWeek = highlights && date.isAfter(TODAY) && !date.isAfter(WEEK_END);
        isFutureThisWeek    = isFutureCurrentWeek && data.getEvents().isEmpty();

        // ── background / cell border ─────────────────────────────────────────
        setBorder(isToday ? Borders.DAY_CELL_TODAY : Borders.DAY_CELL);
        boolean isPastWeekday = !isWeekend && date.isBefore(TODAY);
        setBackground(isWeekend ? BG_WEEKEND : isPastWeekday ? BG_PAST : BG_IN_YEAR);

        // ── day-number badge ─────────────────────────────────────────────────
        dayNumberLabel.setText(dayBadgeText(date));
        if (isToday) {
            dayNumberLabel.setOpaque(true);
            dayNumberLabel.setBackground(TODAY_ACCENT);
            dayNumberLabel.setForeground(Color.WHITE);
            dayNumberLabel.setFont(Fonts.DAY_NUMBER_TODAY);
            dayNumberLabel.setHorizontalAlignment(SwingConstants.CENTER);
        } else if (isFutureCurrentWeek) {
            dayNumberLabel.setOpaque(true);
            dayNumberLabel.setBackground(Colors.DAY_NUMBER);
            dayNumberLabel.setForeground(Color.WHITE);
            dayNumberLabel.setFont(Fonts.DAY_NUMBER_TODAY);
            dayNumberLabel.setHorizontalAlignment(SwingConstants.CENTER);
        } else {
            dayNumberLabel.setOpaque(false);
            dayNumberLabel.setForeground(Colors.DAY_NUMBER);
            dayNumberLabel.setFont(Fonts.DAY_NUMBER);
            dayNumberLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        }

        // ── primary label / future-week marker ───────────────────────────────
        if (isFutureThisWeek) {
            primaryLabel.setText("<<<");
            primaryLabel.setForeground(Color.WHITE);
            primaryLabel.setBackground(TODAY_ACCENT);
            primaryLabel.setOpaque(true);
            primaryLabel.setHorizontalAlignment(SwingConstants.CENTER);
            primaryLabel.setCursor(Cursor.getDefaultCursor());
        } else {
            List<CalendarEvent> events = data.getEvents();
            if (events.isEmpty()) {
                primaryLabel.setText("");
                primaryLabel.setOpaque(false);
                primaryLabel.setHorizontalAlignment(SwingConstants.LEFT);
                primaryLabel.setCursor(Cursor.getDefaultCursor());
            } else {
                primaryEvent = events.get(0);
                primaryLabel.setText(primaryEvent.getName());
                primaryLabel.setBackground(primaryEvent.getCategory().getColor());
                primaryLabel.setForeground(Color.WHITE);
                primaryLabel.setOpaque(true);
                primaryLabel.setHorizontalAlignment(SwingConstants.LEFT);
                primaryLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                for (int i = 1; i < events.size(); i++) {
                    chipsPanel.add(makeChip(events.get(i)));
                }
            }
        }

        revalidate();
        repaint();
    }

    private static String dayBadgeText(LocalDate date) {
        return date.getDayOfMonth() == 1
            ? MONTH_ABBR[date.getMonthValue() - 1]
            : String.valueOf(date.getDayOfMonth());
    }

    public void clear() {
        primaryEvent        = null;
        isToday             = false;
        isFutureCurrentWeek = false;
        isFutureThisWeek    = false;
        primaryLabel.setText("");
        primaryLabel.setOpaque(false);
        primaryLabel.setHorizontalAlignment(SwingConstants.LEFT);
        primaryLabel.setCursor(Cursor.getDefaultCursor());
        dayNumberLabel.setText("");
        dayNumberLabel.setOpaque(false);
        dayNumberLabel.setFont(Fonts.DAY_NUMBER);
        dayNumberLabel.setForeground(Colors.DAY_NUMBER);
        dayNumberLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        chipsPanel.removeAll();
        setBorder(Borders.DAY_CELL);
        setBackground(BG_IN_YEAR);
    }

    private void onEventDropped(Object data) {
        if (!(data instanceof CalendarEventTransfer)) {
            return;
        }
        CalendarEventTransfer transfer = (CalendarEventTransfer) data;
        if (transfer.source == this || currentData == null) {
            return;
        }
        if (transfer.source.currentData != null) {
            transfer.source.currentData.removeEvent(transfer.event);
            transfer.source.populate(transfer.source.currentData);
        }
        currentData.addEvent(transfer.event);
        populate(currentData);
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
        chip.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                draggingEvent = event;
                getTransferHandler().exportAsDrag(DayCellPanel.this, e, TransferHandler.MOVE);
            }
        });
        return chip;
    }
}
