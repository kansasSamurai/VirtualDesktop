package org.katacode.appstage.internal;

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import org.katacode.appstage.DragAndDropService;

/**
 * Intra-stage drag: promote subject onto DRAG_LAYER, move, demote to original parent.
 */
public class DefaultDragAndDropService implements DragAndDropService {

    private final JLayeredPane layeredPane;

    private Component subject;
    private Container originalParent;
    private int originalIndex = -1;
    private Point grabOffsetOnSubject;
    private int subjectWidth;
    private int subjectHeight;
    private boolean dragging;

    public DefaultDragAndDropService(JLayeredPane layeredPane, JComponent dragLayer) {
        this.layeredPane = layeredPane;
        // dragLayer reserved for future ghost hosting; v1 promotes onto layeredPane DRAG_LAYER
    }

    @Override
    public void beginDrag(Component dragSubject, Point startPointOnSubject) {
        if (dragging || dragSubject == null) {
            return;
        }

        subject = dragSubject;
        originalParent = dragSubject.getParent();
        if (originalParent == null) {
            subject = null;
            return;
        }

        originalIndex = indexOf(originalParent, dragSubject);
        grabOffsetOnSubject = new Point(startPointOnSubject);
        subjectWidth = dragSubject.getWidth();
        subjectHeight = dragSubject.getHeight();

        Point onStage = SwingUtilities.convertPoint(originalParent, dragSubject.getLocation(), layeredPane);

        originalParent.remove(dragSubject);
        originalParent.revalidate();
        originalParent.repaint();

        dragSubject.setBounds(onStage.x, onStage.y, subjectWidth, subjectHeight);
        layeredPane.add(dragSubject, JLayeredPane.DRAG_LAYER);
        layeredPane.moveToFront(dragSubject);
        layeredPane.repaint();

        dragging = true;
    }

    @Override
    public void updateDrag(Point currentMousePointOnStage) {
        if (!dragging || subject == null || currentMousePointOnStage == null) {
            return;
        }
        int x = currentMousePointOnStage.x - grabOffsetOnSubject.x;
        int y = currentMousePointOnStage.y - grabOffsetOnSubject.y;
        subject.setBounds(x, y, subjectWidth, subjectHeight);
        layeredPane.repaint();
    }

    @Override
    public void endDrag(Point endPointOnStage) {
        if (!dragging || subject == null) {
            return;
        }

        Point stagePoint = endPointOnStage != null
                ? endPointOnStage
                : new Point(subject.getX() + grabOffsetOnSubject.x, subject.getY() + grabOffsetOnSubject.y);

        int x = stagePoint.x - grabOffsetOnSubject.x;
        int y = stagePoint.y - grabOffsetOnSubject.y;
        Rectangle stageBounds = new Rectangle(x, y, subjectWidth, subjectHeight);

        layeredPane.remove(subject);

        if (originalParent != null) {
            Point inParent = SwingUtilities.convertPoint(layeredPane, stageBounds.getLocation(), originalParent);
            subject.setBounds(inParent.x, inParent.y, subjectWidth, subjectHeight);
            if (originalIndex >= 0 && originalIndex <= originalParent.getComponentCount()) {
                originalParent.add(subject, originalIndex);
            } else {
                originalParent.add(subject);
            }
            originalParent.revalidate();
            originalParent.repaint();
        }

        layeredPane.repaint();
        clearState();
    }

    @Override
    public boolean isDragging() {
        return dragging;
    }

    private void clearState() {
        subject = null;
        originalParent = null;
        originalIndex = -1;
        grabOffsetOnSubject = null;
        dragging = false;
    }

    private static int indexOf(Container parent, Component child) {
        Component[] kids = parent.getComponents();
        for (int i = 0; i < kids.length; i++) {
            if (kids[i] == child) {
                return i;
            }
        }
        return -1;
    }
}
