package org.katacode.agents.auditor.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;

import javax.swing.JPanel;

import org.katacode.agents.auditor.model.RegisteredProject;

/**
 * Everything right of the project list: the breadcrumb bar over a card per view.
 */
@SuppressWarnings("serial")
public class PostureArea extends JPanel {

    private static final String POSTURE_CARD = "POSTURE";
    private static final String VENDOR_DETAIL_CARD = "VENDOR_DETAIL";
    private static final String NO_PROJECT = "No project selected";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final BreadcrumbBar breadcrumb = new BreadcrumbBar(this::showCard);
    private final PosturePanel posture = new PosturePanel(this::openCursorDetail);

    private RegisteredProject currentProject;

    public PostureArea() {
        super(new BorderLayout());

        setBackground(AuditorTheme.contentBackground());

        cards.setOpaque(false);
        cards.add(posture, POSTURE_CARD);
        cards.add(new VendorDetailPanel(), VENDOR_DETAIL_CARD);

        add(breadcrumb, BorderLayout.NORTH);
        add(cards, BorderLayout.CENTER);

        breadcrumb.reset(NO_PROJECT, POSTURE_CARD);
    }

    /**
     * Displays the given project. A different project returns the view to that
     * project's top level; showing the same project again leaves the view as it
     * is (the list re-selects it after a filter change).
     *
     * @param project the selected project, or null when there is none
     */
    public void showProject(RegisteredProject project) {
        if (project == currentProject) {
            return;
        }
        currentProject = project;

        posture.showProject(project);
        breadcrumb.reset(project == null ? NO_PROJECT : project.getTitle(), POSTURE_CARD);
        showCard(POSTURE_CARD);
    }

    private void openCursorDetail() {
        breadcrumb.push("Vendor Health: Cursor", VENDOR_DETAIL_CARD);
        showCard(VENDOR_DETAIL_CARD);
    }

    private void showCard(String name) {
        cardLayout.show(cards, name);
    }
}
