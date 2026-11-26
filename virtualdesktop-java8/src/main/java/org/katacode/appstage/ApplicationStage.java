package org.katacode.appstage;

import java.awt.Component;
import java.awt.Point;

import javax.swing.JComponent;

/**
 * Host contract for stage-aware tools: layered surfaces, card states, and micro-services.
 *
 * <p>Prefer services ({@link #getNotificationService()}, {@link #getModalService()},
 * {@link #getDragAndDropService()}) over raw layer accessors. Layer getters are escape
 * hatches for advanced tools.</p>
 */
public interface ApplicationStage {

    /**
     * Returns the bottom surface, which hosts the cards.
     *
     * <p>Add content with {@link #addCard(String, JComponent)} rather than adding
     * to this component directly, so the stage can track it by id.</p>
     *
     * @return the base layer; never {@code null}
     */
    JComponent getBaseLayer();

    /**
     * Returns the overlay surface directly above the base layer. The modal service
     * places its backdrop here.
     *
     * @return the palette layer; never {@code null}
     */
    JComponent getPaletteLayer();

    /**
     * Returns the overlay surface reserved for drag visuals. It is stacked above
     * the popup layer, so its content paints over toasts and modals.
     *
     * @return the drag layer; never {@code null}
     */
    JComponent getDragLayer();

    /**
     * Returns the overlay surface for toasts, modal content and popovers.
     *
     * @return the popup layer; never {@code null}
     */
    JComponent getPopupLayer();

    /**
     * Converts a point from a component's coordinate space to stage coordinates,
     * as required by {@link DragAndDropService#updateDrag(Point)} and
     * {@link DragAndDropService#endDrag(Point)}.
     *
     * @param source the component whose coordinate space {@code p} is expressed in
     * @param p the point to convert; it is not modified
     * @return a new point in stage coordinates
     */
    Point convertToStagePoint(Component source, Point p);

    /**
     * Registers a card on the base layer under the given id.
     *
     * <p>An existing card with the same id is removed and replaced without warning.
     * If the replaced card was showing, call {@link #showCard(String)} again.</p>
     *
     * @param id the key used to show the card later
     * @param card the card content
     * @throws IllegalArgumentException if {@code id} or {@code card} is {@code null}
     */
    void addCard(String id, JComponent card);

    /**
     * Makes the card registered under {@code id} the visible base content.
     * Overlay layers are not affected.
     *
     * @param id the id passed to {@link #addCard(String, JComponent)}
     * @throws IllegalArgumentException if {@code id} is {@code null} or unknown
     */
    void showCard(String id);

    /**
     * Returns the toast service for this stage.
     *
     * @return the notification service; never {@code null}
     */
    NotificationService getNotificationService();

    /**
     * Returns the modal and popover service for this stage.
     *
     * @return the modal service; never {@code null}
     */
    ModalService getModalService();

    /**
     * Returns the drag-and-drop service. Drags are confined to this stage.
     *
     * @return the drag-and-drop service; never {@code null}
     */
    DragAndDropService getDragAndDropService();
}
