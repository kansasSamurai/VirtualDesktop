package org.katacode.appstage.internal;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.katacode.appstage.ModalService;

/**
 * Minimal modal / popover overlays using PALETTE (backdrop) and POPUP (content).
 */
public class DefaultModalService implements ModalService {

    private final JComponent paletteLayer;
    private final JComponent popupLayer;

    private JPanel backdrop;
    private JComponent activeContent;
    private boolean modalActive;

    public DefaultModalService(JComponent paletteLayer, JComponent popupLayer) {
        this.paletteLayer = paletteLayer;
        this.popupLayer = popupLayer;
    }

    @Override
    public void showModal(JComponent modalContent) {
        dismissActiveModal();
        if (modalContent == null) {
            return;
        }

        backdrop = new JPanel(null);
        backdrop.setOpaque(true);
        backdrop.setBackground(new Color(0, 0, 0, 90));
        backdrop.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // absorb clicks
            }
        });

        JPanel chrome = wrapWithDismiss(modalContent);
        activeContent = chrome;
        modalActive = true;

        syncBounds();
        paletteLayer.add(backdrop);
        popupLayer.add(chrome);
        paletteLayer.revalidate();
        popupLayer.revalidate();
        paletteLayer.repaint();
        popupLayer.repaint();
    }

    @Override
    public void showPopover(JComponent content, Component anchorComponent) {
        dismissActiveModal();
        if (content == null) {
            return;
        }

        activeContent = content;
        modalActive = true;
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(160, 160, 160)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        content.setOpaque(true);
        content.setBackground(Color.WHITE);

        Dimension pref = content.getPreferredSize();
        Point anchorOnPopup = new Point(0, 0);
        if (anchorComponent != null) {
            Point below = new Point(0, anchorComponent.getHeight());
            anchorOnPopup = SwingUtilities.convertPoint(anchorComponent, below, popupLayer);
        }
        int x = Math.max(8, Math.min(anchorOnPopup.x, popupLayer.getWidth() - pref.width - 8));
        int y = Math.max(8, Math.min(anchorOnPopup.y + 4, popupLayer.getHeight() - pref.height - 8));
        content.setBounds(x, y, pref.width, pref.height);

        popupLayer.add(content);
        popupLayer.revalidate();
        popupLayer.repaint();
    }

    @Override
    public void dismissActiveModal() {
        if (!modalActive) {
            return;
        }
        if (backdrop != null) {
            paletteLayer.remove(backdrop);
            backdrop = null;
        }
        if (activeContent != null) {
            popupLayer.remove(activeContent);
            activeContent = null;
        }
        modalActive = false;
        paletteLayer.revalidate();
        popupLayer.revalidate();
        paletteLayer.repaint();
        popupLayer.repaint();
    }

    /**
     * Keep backdrop / modal centered when the stage resizes.
     */
    public void relayout() {
        if (!modalActive) {
            return;
        }
        syncBounds();
        paletteLayer.repaint();
        popupLayer.repaint();
    }

    private void syncBounds() {
        int w = paletteLayer.getWidth();
        int h = paletteLayer.getHeight();
        if (backdrop != null) {
            backdrop.setBounds(0, 0, w, h);
        }
        if (activeContent != null && backdrop != null) {
            Dimension pref = activeContent.getPreferredSize();
            int cw = Math.max(pref.width, 240);
            int ch = Math.max(pref.height, 80);
            int x = Math.max(0, (w - cw) / 2);
            int y = Math.max(0, (h - ch) / 2);
            activeContent.setBounds(x, y, cw, ch);
        }
    }

    private JPanel wrapWithDismiss(JComponent body) {
        JPanel chrome = new JPanel(new BorderLayout(0, 8));
        chrome.setOpaque(true);
        chrome.setBackground(Color.WHITE);
        chrome.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(120, 120, 120)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        chrome.add(body, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.setOpaque(false);
        JButton close = new JButton("Close");
        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dismissActiveModal();
            }
        });
        buttons.add(close);
        chrome.add(buttons, BorderLayout.SOUTH);
        return chrome;
    }
}
