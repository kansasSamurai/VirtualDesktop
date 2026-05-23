package org.jwellman.demo.calendar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/**
 * Right-side panel that displays details for a selected calendar event.
 * Updates via showEvent(); resets via clear().
 */
public class EventDetailPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private static final int PREFERRED_WIDTH = 210;
    private static final Color BORDER_CLR = new Color(0xCC, 0xCC, 0xCC);

    private final JPanel colorBar;
    private final JLabel nameLabel;
    private final JLabel dateLabel;
    private final JLabel categoryLabel;
    private final JTextArea descArea;

    public EventDetailPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER_CLR));
        setPreferredSize(new Dimension(PREFERRED_WIDTH, 0));

        colorBar = new JPanel();
        colorBar.setPreferredSize(new Dimension(0, 6));
        colorBar.setOpaque(true);
        colorBar.setBackground(BORDER_CLR);
        add(colorBar, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        nameLabel = new JLabel("No event selected");
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        nameLabel.setAlignmentX(LEFT_ALIGNMENT);

        dateLabel = new JLabel(" ");
        dateLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        dateLabel.setForeground(new Color(0x55, 0x55, 0x55));
        dateLabel.setAlignmentX(LEFT_ALIGNMENT);

        categoryLabel = new JLabel(" ");
        categoryLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        categoryLabel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel descHeader = new JLabel("Notes");
        descHeader.setFont(new Font("SansSerif", Font.BOLD, 10));
        descHeader.setForeground(new Color(0x88, 0x88, 0x88));
        descHeader.setAlignmentX(LEFT_ALIGNMENT);

        descArea = new JTextArea();
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setEditable(false);
        descArea.setOpaque(false);
        descArea.setFont(new Font("SansSerif", Font.PLAIN, 11));
        descArea.setAlignmentX(LEFT_ALIGNMENT);

        content.add(nameLabel);
        content.add(Box.createVerticalStrut(6));
        content.add(dateLabel);
        content.add(Box.createVerticalStrut(3));
        content.add(categoryLabel);
        content.add(Box.createVerticalStrut(14));
        content.add(descHeader);
        content.add(Box.createVerticalStrut(4));
        content.add(descArea);

        add(content, BorderLayout.CENTER);
    }

    public void showEvent(CalendarEvent event) {
        colorBar.setBackground(event.getCategory().getColor());
        nameLabel.setText(event.getName());
        dateLabel.setText(event.getDate().format(DATE_FMT));
        categoryLabel.setText(event.getCategory().getDisplayName());
        categoryLabel.setForeground(event.getCategory().getColor());
        descArea.setText(event.getDescription());
        revalidate();
        repaint();
    }

    public void clear() {
        colorBar.setBackground(BORDER_CLR);
        nameLabel.setText("No event selected");
        dateLabel.setText(" ");
        categoryLabel.setText(" ");
        categoryLabel.setForeground(new Color(0x55, 0x55, 0x55));
        descArea.setText("");
    }
}
