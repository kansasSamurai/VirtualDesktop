package org.jwellman.virtualdesktop.vapps;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import org.jwellman.demo.CalendarDemo;

/**
 * VApp that hosts the year-calendar SmartGrid demo inside a VirtualDesktop
 * internal frame.  UI is provided by {@link CalendarDemo#createCalendarUI()}.
 */
public class SpecCalendarDemo extends VirtualAppSpec {

    public SpecCalendarDemo() {
        super();
        this.setTitle("Year Calendar");
        this.setWidth(960);
        this.setHeight(620);

        JPanel content = new JPanel(new BorderLayout());
        content.add(CalendarDemo.createCalendarUI(), BorderLayout.CENTER);
        this.setContent(content);
    }
}
