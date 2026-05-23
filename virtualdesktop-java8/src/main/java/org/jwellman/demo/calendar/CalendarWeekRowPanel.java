package org.jwellman.demo.calendar;

import java.util.function.Consumer;
import javax.swing.JPanel;
import org.jwellman.swing.grid.GridRow;
import org.jwellman.swing.grid.Recyclable;

/**
 * Custom SmartGrid row renderer for a week in the year-calendar view.
 * Holds seven DayCellPanel instances (Mon–Sun) and positions them to align
 * with SmartGrid's column headers via the shared columnWidths[] reference.
 */
public class CalendarWeekRowPanel extends JPanel implements Recyclable {

    private static final String[] DAY_KEYS = {"mon", "tue", "wed", "thu", "fri", "sat", "sun"};

    private final DayCellPanel[] dayCells;
    private final int[] columnWidths;

    public CalendarWeekRowPanel(int[] columnWidths, Consumer<CalendarEvent> onEventClicked) {
        this.columnWidths = columnWidths;
        setLayout(null);
        setOpaque(true);

        dayCells = new DayCellPanel[7];
        for (int i = 0; i < 7; i++) {
            dayCells[i] = new DayCellPanel(onEventClicked);
            add(dayCells[i]);
        }
    }

    @Override
    public void doLayout() {
        int h = getHeight();
        int x = 0;
        for (int i = 0; i < 7; i++) {
            int w = (columnWidths != null && i < columnWidths.length && columnWidths[i] > 0)
                ? columnWidths[i]
                : getWidth() / 7;
            dayCells[i].setBounds(x, 0, w, h);
            x += w;
        }
    }

    @Override
    public void prepareForReuse() {
        for (DayCellPanel cell : dayCells) {
            cell.clear();
        }
    }

    @Override
    public void bind(GridRow row, int rowIndex) {
        for (int i = 0; i < DAY_KEYS.length; i++) {
            Object val = row.get(DAY_KEYS[i]);
            DayData data = (val instanceof DayData) ? (DayData) val : null;
            dayCells[i].populate(data);
        }
    }
}
