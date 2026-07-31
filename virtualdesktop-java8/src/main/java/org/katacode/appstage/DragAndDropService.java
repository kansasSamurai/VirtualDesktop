package org.katacode.appstage;

import java.awt.Component;
import java.awt.Point;

/**
 * Intra-stage drag lifecycle: promote to the stage DRAG layer, move, demote.
 *
 * <p>{@link #beginDrag} uses a subject-local grab point.
 * {@link #updateDrag} / {@link #endDrag} take <strong>stage-relative</strong> points
 * (callers should use {@link ApplicationStage#convertToStagePoint}).</p>
 *
 * <p>Cross-tool / desktop-level DnD is out of scope until the desktop itself
 * hosts a shared {@link ApplicationStage}.</p>
 */
public interface DragAndDropService {

    void beginDrag(Component dragSubject, Point startPointOnSubject);

    void updateDrag(Point currentMousePointOnStage);

    void endDrag(Point endPointOnStage);

    boolean isDragging();
}
