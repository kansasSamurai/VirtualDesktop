package org.jwellman.demo;

import java.awt.BorderLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JPanel;

import org.jwellman.demo.calendar.CalendarEvent;
import org.jwellman.demo.calendar.CalendarWeekRowPanel;
import org.jwellman.demo.calendar.DayData;
import org.jwellman.demo.calendar.EventCategory;
import org.jwellman.demo.calendar.EventDetailPanel;
import org.jwellman.demo.calendar.OnCallStrip;
import org.jwellman.demo.calendar.WeekNumberStrip;
import org.jwellman.swing.grid.ColumnDef;
import org.jwellman.swing.grid.DefaultGridModel;
import org.jwellman.swing.grid.GridRow;
import org.jwellman.swing.grid.SmartGrid;

/**
 * Builds the year-calendar view: a SmartGrid with one row per week (Mon–Sun
 * columns) and an EventDetailPanel on the right that populates when a chip
 * or primary-event label is clicked.
 */
public class CalendarDemo {

    private static final String[] DAY_KEYS = {"mon", "tue", "wed", "thu", "fri", "sat", "sun"};

    public static JPanel createCalendarUI() {
        int year = LocalDate.now().getYear();
        Map<LocalDate, List<CalendarEvent>> eventMap = generateEvents(year);

        EventDetailPanel detailPanel = new EventDetailPanel();
        Consumer<CalendarEvent> onEventClicked = detailPanel::showEvent;

        Map<Integer, String> onCallSchedule = generateOnCallSchedule(year);
        DefaultGridModel model = buildModel(year, eventMap, onCallSchedule);

        SmartGrid grid = new SmartGrid(model);
        grid.setRowHeight(64);

        final int[] columnWidths = grid.getColumnWidths();
        grid.registerRowRenderer("calendar-week",
            () -> new CalendarWeekRowPanel(columnWidths, onEventClicked));

        grid.addStrip(new WeekNumberStrip(grid.getHeaderBackground()));
        grid.addStrip(new OnCallStrip(grid.getHeaderBackground()));
        grid.setCheckboxColumnVisible(false);
        grid.setRowSelectionEnabled(false);

        JPanel container = new JPanel(new BorderLayout());
        container.add(grid, BorderLayout.CENTER);
        container.add(detailPanel, BorderLayout.EAST);

        return container;
    }

    // -------------------------------------------------------------------------
    // Model construction
    // -------------------------------------------------------------------------

    private static DefaultGridModel buildModel(int year, Map<LocalDate, List<CalendarEvent>> eventMap,
                                               Map<Integer, String> onCallSchedule) {
        DefaultGridModel model = new DefaultGridModel();

        model.addColumn(new ColumnDef("mon", "Mon"));
        model.addColumn(new ColumnDef("tue", "Tue"));
        model.addColumn(new ColumnDef("wed", "Wed"));
        model.addColumn(new ColumnDef("thu", "Thu"));
        model.addColumn(new ColumnDef("fri", "Fri"));
        model.addColumn(new ColumnDef("sat", "Sat"));
        model.addColumn(new ColumnDef("sun", "Sun"));

        LocalDate jan1 = LocalDate.of(year, 1, 1);
        LocalDate dec31 = LocalDate.of(year, 12, 31);

        // First Monday on or before Jan 1; last Sunday on or after Dec 31
        LocalDate weekStart = jan1.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate yearEnd   = dec31.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        LocalDate current = weekStart;
        while (!current.isAfter(yearEnd)) {
            GridRow row = new GridRow();
            row.setTag("fnd-type", "calendar-week");

            for (int i = 0; i < 7; i++) {
                LocalDate day = current.plusDays(i);
                boolean inYear = day.getYear() == year;
                List<CalendarEvent> events = eventMap.containsKey(day)
                    ? eventMap.get(day)
                    : Collections.<CalendarEvent>emptyList();
                row.put(DAY_KEYS[i], new DayData(day, events, inYear));
            }

            int weekNum = current.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
            row.put("oncall", onCallSchedule.getOrDefault(weekNum, ""));

            model.addRow(row);
            current = current.plusWeeks(1);
        }

        model.notifyDataChanged();
        return model;
    }

    // -------------------------------------------------------------------------
    // Dummy event generation — seeded from today so events are always "upcoming"
    // -------------------------------------------------------------------------

    private static Map<LocalDate, List<CalendarEvent>> generateEvents(int year) {
        Map<LocalDate, List<CalendarEvent>> map = new LinkedHashMap<LocalDate, List<CalendarEvent>>();

        LocalDate today = LocalDate.now();

        // Spread events across roughly 10 weeks from today, with a few same-day
        // pairs to demonstrate the chip row.
        addEvent(map, today,
            new CalendarEvent("v2.1 Release", today, EventCategory.RELEASE,
                "Major release: new REST endpoints, UI overhaul, and performance improvements across the board."));

        addEvent(map, today,
            new CalendarEvent("Deploy Hotfix", today, EventCategory.HOTFIX,
                "Emergency patch for authentication bypass discovered in overnight security scan."));

        LocalDate d = today.plusDays(3);
        addEvent(map, d,
            new CalendarEvent("Sprint Planning", d, EventCategory.MEETING,
                "Q3 sprint kickoff. Review backlog, estimate stories, and assign owners."));

        d = today.plusDays(5);
        addEvent(map, d,
            new CalendarEvent("Security Patch", d, EventCategory.HOTFIX,
                "Dependency upgrade to resolve CVE-2026-1042 in the HTTP client library."));

        d = today.plusDays(6);
        addEvent(map, d,
            new CalendarEvent("Q2 Freeze", d, EventCategory.DEADLINE,
                "Feature freeze for Q2. No new features merged after this date; bugfixes only."));

        d = today.plusDays(10);
        addEvent(map, d,
            new CalendarEvent("Team Sync", d, EventCategory.MEETING,
                "Weekly all-hands. Engineering, Product, and Design leads in attendance."));

        d = today.plusDays(13);
        addEvent(map, d,
            new CalendarEvent("v2.2 Release", d, EventCategory.RELEASE,
                "Minor release: dependency updates, three customer-reported bug fixes."));

        d = today.plusDays(18);
        addEvent(map, d,
            new CalendarEvent("Architecture Review", d, EventCategory.MEETING,
                "Proposed microservices split for the billing module. Decision expected this session."));

        addEvent(map, d,
            new CalendarEvent("Perf Hotfix", d, EventCategory.HOTFIX,
                "Query optimizer patch — dashboard load reduced from 8s to 400ms in staging."));

        d = today.plusDays(23);
        addEvent(map, d,
            new CalendarEvent("API Freeze", d, EventCategory.DEADLINE,
                "Public API contract locked for the v3.0 cycle. Breaking changes require RFC after this point."));

        d = today.plusDays(38);
        addEvent(map, d,
            new CalendarEvent("Q2 Close", d, EventCategory.DEADLINE,
                "End-of-quarter sign-off: metrics review, OKR scoring, and board report submission."));

        addEvent(map, d,
            new CalendarEvent("Retrospective", d, EventCategory.MEETING,
                "Q2 retrospective. What went well, what to improve, action items for Q3."));

        d = today.plusDays(52);
        addEvent(map, d,
            new CalendarEvent("v3.0 Beta", d, EventCategory.RELEASE,
                "Beta release to design partners. Feature-complete; stabilization period begins."));

        d = today.plusDays(69);
        addEvent(map, d,
            new CalendarEvent("Q3 Checkpoint", d, EventCategory.DEADLINE,
                "Mid-quarter OKR check. Adjust resourcing if any key result is behind pace."));

        d = today.plusDays(90);
        addEvent(map, d,
            new CalendarEvent("v3.0 GA", d, EventCategory.RELEASE,
                "General availability release of v3.0. Marketing launch and blog post coordinated."));

        return map;
    }

    private static Map<Integer, String> generateOnCallSchedule(int year) {
        String[] names = {"Alice", "Bob", "Carol", "Dave", "Eve"};
        Map<Integer, String> schedule = new LinkedHashMap<Integer, String>();
        LocalDate jan1 = LocalDate.of(year, 1, 1);
        LocalDate weekStart = jan1.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate dec31 = LocalDate.of(year, 12, 31);
        LocalDate yearEnd = dec31.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        int rotation = 0;
        LocalDate current = weekStart;
        while (!current.isAfter(yearEnd)) {
            int weekNum = current.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
            schedule.put(weekNum, names[rotation % names.length]);
            rotation++;
            current = current.plusWeeks(1);
        }
        return schedule;
    }

    private static void addEvent(Map<LocalDate, List<CalendarEvent>> map,
                                 LocalDate date, CalendarEvent event) {
        List<CalendarEvent> list = map.get(date);
        if (list == null) {
            list = new ArrayList<CalendarEvent>();
            map.put(date, list);
        }
        list.add(event);
    }
}
