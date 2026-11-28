package org.katacode.agents.auditor.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import org.jwellman.swing.grid.GridRow;
import org.jwellman.swing.grid.Recyclable;
import org.jwellman.swing.grid.Selectable;
import org.katacode.agents.auditor.model.ProjectStatus;
import org.katacode.agents.auditor.model.RegisteredProject;

/**
 * SmartGrid row for one registered project: marker, name and status badge on a
 * rounded card. The cell owns its default, hover, selected and waived looks.
 *
 * <p>The component fills the whole grid row; the empty outer border produces the
 * gap between cards and leaves room for the selection glow.</p>
 */
@SuppressWarnings("serial")
class ProjectCell extends JPanel implements Recyclable, Selectable {

    /** Value key under which each grid row stores its {@link RegisteredProject}. */
    static final String PROJECT_KEY = "project";

    private static final int GLOW = 3;
    private static final int CARD_HEIGHT = 44;
    private static final int GAP_BELOW = 7;
    private static final int RADIUS = 6;

    static final int ROW_HEIGHT = GLOW + CARD_HEIGHT + GAP_BELOW;

    private final JLabel nameLabel = new JLabel();
    private final StatusBadge badge = new StatusBadge();
    private final RoundedLineBorder lineBorder =
            new RoundedLineBorder(AuditorTheme.cellBorder(), RADIUS);
    private final StatusMarkerBorder markerBorder = new StatusMarkerBorder();
    private final ListSelectionModel selectionModel;

    private MouseAdapter selectionListener;
    private ProjectStatus status = ProjectStatus.OK;
    private boolean selected;
    private boolean hovered;

    ProjectCell(ListSelectionModel selectionModel) {
        super(new BorderLayout());
        this.selectionModel = selectionModel;

        setOpaque(false);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(GLOW, GLOW, GAP_BELOW, GLOW),
                BorderFactory.createCompoundBorder(lineBorder, markerBorder)));

        nameLabel.setFont(AuditorTheme.uiFont(Font.PLAIN, 17f));
        add(nameLabel, BorderLayout.CENTER);

        // GridBagLayout keeps the badge at its preferred height, vertically centred
        JPanel badgeHolder = new JPanel(new GridBagLayout());
        badgeHolder.setOpaque(false);
        badgeHolder.add(badge);
        add(badgeHolder, BorderLayout.EAST);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setHovered(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHovered(false);
            }
        });
    }

    @Override
    public void prepareForReuse() {
        removeSelectionListener();
        nameLabel.setText("");
        setToolTipText(null);
        selected = false;
        hovered = false;
    }

    @Override
    public void bind(GridRow row, final int rowIndex) {
        removeSelectionListener();

        RegisteredProject project = (RegisteredProject) row.get(PROJECT_KEY);
        status = project.getProjectStatus();
        nameLabel.setText(project.getTitle());
        badge.setStatus(status, project.getCount());
        setToolTipText(project.getFolder());

        selectionListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                selectionModel.setSelectionInterval(rowIndex, rowIndex);
            }
        };
        addMouseListener(selectionListener);

        applyState();
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
        applyState();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int width = getWidth() - 2 * GLOW;
        int height = getHeight() - GLOW - GAP_BELOW;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (selected) {
            g2.setColor(AuditorTheme.cellSelectedGlow());
            g2.fillRoundRect(0, 0, getWidth(), height + 2 * GLOW,
                    (RADIUS + GLOW) * 2, (RADIUS + GLOW) * 2);
        }
        g2.setColor(cardBackground());
        g2.fillRoundRect(GLOW, GLOW, width, height, RADIUS * 2, RADIUS * 2);
        g2.dispose();
    }

    private void setHovered(boolean hovered) {
        this.hovered = hovered;
        applyState();
    }

    private void removeSelectionListener() {
        if (selectionListener != null) {
            removeMouseListener(selectionListener);
            selectionListener = null;
        }
    }

    private void applyState() {
        boolean waived = status == ProjectStatus.WAIVED;

        if (selected) {
            lineBorder.setColor(AuditorTheme.cellSelectedBorder());
            markerBorder.setColor(AuditorTheme.accent());
        } else {
            lineBorder.setColor(hovered ? AuditorTheme.cellHoverBorder() : AuditorTheme.cellBorder());
            markerBorder.setColor(waived ? AuditorTheme.waivedMarker() : null);
        }
        nameLabel.setForeground(waived && !selected
                ? AuditorTheme.textWaived()
                : AuditorTheme.textPrimary());

        repaint();
    }

    private Color cardBackground() {
        if (selected) {
            return AuditorTheme.cellSelectedBackground();
        }

        return hovered ? AuditorTheme.cellHoverBackground() : AuditorTheme.cellBackground();
    }
}
