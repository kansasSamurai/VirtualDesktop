package org.katacode.agents.auditor.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.katacode.agents.auditor.model.RegisteredProject;

/**
 * Posture overview of the selected project. So far only the header (project name
 * and folder path) and a temporary link into the vendor detail view.
 */
@SuppressWarnings("serial")
class PosturePanel extends ViewPanel {

    private static final String TITLE_PREFIX = "PROJECT POSTURE";

    // TEMPORARY: remove once the vendor tiles and health panels open the detail view
    private final LinkButton cursorLink = new LinkButton("Cursor Setup ›");

    /**
     * @param openCursorDetail run when the user asks for the Cursor detail view
     */
    PosturePanel(final Runnable openCursorDetail) {
        cursorLink.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openCursorDetail.run();
            }
        });

        JPanel links = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        links.setOpaque(false);
        links.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        links.add(cursorLink);
        add(links, BorderLayout.CENTER);

        showProject(null);
    }

    /**
     * Displays the given project.
     *
     * @param project the selected project, or null when there is none
     */
    void showProject(RegisteredProject project) {
        if (project == null) {
            showHeader(TITLE_PREFIX, "No project selected");
        } else {
            showHeader(TITLE_PREFIX + ": " + project.getTitle(), project.getFolder());
            setPathToolTip(project.getFolder());
        }
        cursorLink.setVisible(project != null);
    }
}
