package org.jwellman.demo.calendar;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Standalone launcher for {@link CalendarPoCPanel}.
 */
@SuppressWarnings("serial")
public class CalendarPoCFrame extends JFrame {

    public CalendarPoCFrame() {
        setTitle("Calendar PoC (Java 8) - Spatial Stage & Layered Pane");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setContentPane(new CalendarPoCPanel());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new CalendarPoCFrame().setVisible(true);
            }
        });
    }
}
