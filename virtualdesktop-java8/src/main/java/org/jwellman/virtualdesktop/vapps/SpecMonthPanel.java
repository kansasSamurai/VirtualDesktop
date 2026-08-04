package org.jwellman.virtualdesktop.vapps;

import org.jwellman.swing.calendar.MonthPanel;

public class SpecMonthPanel extends VirtualAppSpec {

    public SpecMonthPanel() {
        super();

        this.setTitle("Calendar");
        this.setContent(this.createDefaultContent(new MonthPanel()));
    }

}
