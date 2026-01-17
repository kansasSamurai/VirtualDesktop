package org.jwellman.virtualdesktop.vapps;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Base class for vapps that run in the JVM but display in an external JFrame.
 *
 * This provides a placeholder panel shown in the virtual desktop while the
 * actual content is displayed in a separate JFrame window.
 *
 * Features planned for future development:
 * <ul>
 *   <li>minimize/maximize/bring-to-front control</li>
 *   <li>shutdown hooks for custom cleanup sequences</li>
 * </ul>
 *
 * @author rwellman
 */
public class AbstractExternalApp extends VirtualAppSpec {

    /**
     * Creates an external app placeholder with the given title.
     * @param title the display name for the application
     */
    public AbstractExternalApp(String title) {
        super();

        this.setTitle(title);
        this.setContent(createPlaceholderPanel());
    }

    /**
     * Creates the placeholder panel displayed in the virtual desktop.
     * Subclasses may override to customize the appearance.
     * @return the placeholder panel
     */
    protected JPanel createPlaceholderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Main message
        String message = String.format(
            "<html><center>" +
            "<b>External Window</b><br><br>" +
            "Application: <i>%s</i><br><br>" +
            "This app runs in the JVM but<br>" +
            "is displayed in an external window." +
            "</center></html>",
            getTitle()
        );

        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        // Bring to front link
        JLabel bringToFrontLink = new JLabel(
            "<html><center><br>If the window is hidden,<br>" +
            "<u>click here</u> to bring it to front.</center></html>",
            SwingConstants.CENTER
        );
        bringToFrontLink.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        bringToFrontLink.setForeground(new Color(0, 102, 204)); // Link blue
        bringToFrontLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bringToFrontLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                bringToFront();
            }
        });

        // Center panel with vertical layout
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(Box.createVerticalGlue());
        label.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        bringToFrontLink.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        centerPanel.add(label);
        centerPanel.add(bringToFrontLink);
        centerPanel.add(Box.createVerticalGlue());

        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Brings the external window to the front.
     * Subclasses should override this to implement the actual behavior.
     */
    protected void bringToFront() {
        // Default implementation does nothing.
        // Subclasses should override to bring their JFrame to front.
        System.out.println("bringToFront() not implemented for: " + getTitle());
    }

}
