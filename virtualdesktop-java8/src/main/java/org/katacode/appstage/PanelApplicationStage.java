package org.katacode.appstage;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.katacode.appstage.internal.DefaultDragAndDropService;
import org.katacode.appstage.internal.DefaultModalService;
import org.katacode.appstage.internal.DefaultNotificationService;
import org.katacode.appstage.internal.PassThroughPanel;

/**
 * Default embeddable {@link ApplicationStage}: a {@link JPanel} wrapping a
 * private {@link JLayeredPane} with CardLayout base content and overlay layers.
 */
@SuppressWarnings("serial")
public class PanelApplicationStage extends JPanel implements ApplicationStage {

    private final JLayeredPane layeredPane = new JLayeredPane();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel baseCards = new JPanel(cardLayout);
    private final PassThroughPanel paletteLayer = new PassThroughPanel();
    private final PassThroughPanel dragLayer = new PassThroughPanel();
    private final PassThroughPanel popupLayer = new PassThroughPanel();

    private final Map<String, JComponent> cardsById = new HashMap<String, JComponent>();

    private final DefaultNotificationService notificationService;
    private final DefaultModalService modalService;
    private final DefaultDragAndDropService dragAndDropService;

    public PanelApplicationStage() {
        super(new BorderLayout());

        baseCards.setOpaque(true);

        layeredPane.setLayout(null);
        layeredPane.add(baseCards, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(paletteLayer, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(dragLayer, JLayeredPane.DRAG_LAYER);
        layeredPane.add(popupLayer, JLayeredPane.POPUP_LAYER);

        notificationService = new DefaultNotificationService(popupLayer);
        modalService = new DefaultModalService(paletteLayer, popupLayer);
        dragAndDropService = new DefaultDragAndDropService(layeredPane, dragLayer);

        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                syncLayerBounds();
                notificationService.relayout();
                modalService.relayout();
            }
        });

        add(layeredPane, BorderLayout.CENTER);
    }

    private void syncLayerBounds() {
        int w = layeredPane.getWidth();
        int h = layeredPane.getHeight();
        baseCards.setBounds(0, 0, w, h);
        paletteLayer.setBounds(0, 0, w, h);
        dragLayer.setBounds(0, 0, w, h);
        popupLayer.setBounds(0, 0, w, h);
        layeredPane.revalidate();
    }

    @Override
    public JComponent getBaseLayer() {
        return baseCards;
    }

    @Override
    public JComponent getPaletteLayer() {
        return paletteLayer;
    }

    @Override
    public JComponent getDragLayer() {
        return dragLayer;
    }

    @Override
    public JComponent getPopupLayer() {
        return popupLayer;
    }

    @Override
    public Point convertToStagePoint(Component source, Point p) {
        return SwingUtilities.convertPoint(source, p, layeredPane);
    }

    @Override
    public void addCard(String id, JComponent card) {
        if (id == null || card == null) {
            throw new IllegalArgumentException("card id and component are required");
        }
        if (cardsById.containsKey(id)) {
            baseCards.remove(cardsById.get(id));
        }
        cardsById.put(id, card);
        baseCards.add(card, id);
        baseCards.revalidate();
        baseCards.repaint();
    }

    @Override
    public void showCard(String id) {
        if (id == null || !cardsById.containsKey(id)) {
            throw new IllegalArgumentException("Unknown card id: " + id);
        }
        cardLayout.show(baseCards, id);
    }

    @Override
    public NotificationService getNotificationService() {
        return notificationService;
    }

    @Override
    public ModalService getModalService() {
        return modalService;
    }

    @Override
    public DragAndDropService getDragAndDropService() {
        return dragAndDropService;
    }
}
