package org.jwellman.virtualdesktop.vapps;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import org.jwellman.demo.calendar.CalendarPoCPanel;
import org.katacode.appstage.ApplicationStage;
import org.katacode.appstage.PanelApplicationStage;
import org.katacode.appstage.ToastType;

/**
 * Demo tool: {@link PanelApplicationStage} hosting dummy chrome around
 * {@link CalendarPoCPanel}. Proves stage toasts/modals; calendar PoC keeps
 * its own layered-pane drag for now.
 */
public class SpecAppStageCalendarDemo extends VirtualAppSpec {

    public SpecAppStageCalendarDemo() {
        super();
        this.setTitle("App Stage Calendar");
        this.setWidth(1000);
        this.setHeight(700);

        PanelApplicationStage stage = new PanelApplicationStage();
        stage.addCard("main", createMainCard(stage));
        stage.showCard("main");
        this.setContent(stage);
    }

    private JPanel createMainCard(final ApplicationStage stage) {
        JPanel root = new JPanel(new BorderLayout(0, 0));

        root.add(createToolbar(stage), BorderLayout.NORTH);
        root.add(createSidebar(), BorderLayout.WEST);
        root.add(new CalendarPoCPanel(), BorderLayout.CENTER);
        root.add(createStatusBar(), BorderLayout.SOUTH);

        return root;
    }

    private JPanel createToolbar(final ApplicationStage stage) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)));

        JButton newEvent = new JButton("New Event");
        newEvent.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stage.getNotificationService().showToast(
                        "Event", "Dummy event created (PoC).", ToastType.SUCCESS);
            }
        });

        JButton help = new JButton("Help");
        help.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JPanel body = new JPanel(new BorderLayout(0, 8));
                body.setOpaque(false);
                JLabel title = new JLabel("App Stage Calendar Demo");
                title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
                body.add(title, BorderLayout.NORTH);
                body.add(new JLabel("<html>This tool hosts a calendar PoC inside a<br>"
                        + "<b>PanelApplicationStage</b>. Toolbar actions use<br>"
                        + "NotificationService and ModalService.</html>"), BorderLayout.CENTER);
                stage.getModalService().showModal(body);
            }
        });

        JButton infoToast = new JButton("Toast");
        infoToast.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stage.getNotificationService().showToast("Stage toast demo", 2500);
            }
        });

        bar.add(newEvent);
        bar.add(help);
        bar.add(infoToast);
        return bar;
    }

    private JPanel createSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setPreferredSize(new Dimension(160, 0));
        side.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(200, 200, 200)),
                new EmptyBorder(8, 8, 8, 8)));

        JLabel heading = new JLabel("Calendars");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD));
        side.add(heading, BorderLayout.NORTH);

        DefaultListModel model = new DefaultListModel();
        model.addElement("Work");
        model.addElement("Personal");
        model.addElement("Team");
        JList list = new JList(model);
        side.add(new JScrollPane(list), BorderLayout.CENTER);

        return side;
    }

    private JPanel createStatusBar() {
        JPanel status = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        status.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)));
        status.add(new JLabel("Ready — drag event cards on the calendar grid."));
        return status;
    }
}
