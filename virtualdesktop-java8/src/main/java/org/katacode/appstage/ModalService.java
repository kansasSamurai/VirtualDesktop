package org.katacode.appstage;

import java.awt.Component;

import javax.swing.JComponent;

/**
 * Modal / popover overlays owned by the application stage.
 */
public interface ModalService {

    void showModal(JComponent modalContent);

    void showPopover(JComponent content, Component anchorComponent);

    void dismissActiveModal();
}
