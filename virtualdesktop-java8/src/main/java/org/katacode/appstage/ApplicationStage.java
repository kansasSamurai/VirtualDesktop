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

    JComponent getBaseLayer();

    JComponent getPaletteLayer();

    JComponent getDragLayer();

    JComponent getPopupLayer();

    Point convertToStagePoint(Component source, Point p);

    void addCard(String id, JComponent card);

    void showCard(String id);

    NotificationService getNotificationService();

    ModalService getModalService();

    DragAndDropService getDragAndDropService();
}
