package org.jwellman.virtualdesktop.taskbar;

import java.util.List;
import javax.swing.JComponent;

/**
 * Abstraction for the taskbar's visual layer.
 *
 * The controller holds a reference to this interface and never knows which
 * concrete view is installed. Swapping implementations (JList, SmartGrid,
 * etc.) requires no changes to the controller or the Redux machinery.
 *
 * Data contract: the controller passes the full item list on every state
 * change. The list structure encodes grouping — when grouping is enabled,
 * some items will have children; when disabled, all items are leaf nodes.
 * The view renders whatever structure it receives; it has no responsibility
 * for grouping logic.
 */
public interface TaskbarView {

    /**
     * Returns the component to embed in the application's panel hierarchy.
     * Called once at wiring time.
     */
    JComponent getComponent();

    /**
     * Replaces the full item list. Called by the controller on every Redux
     * state change that affects visible taskbar content.
     */
    void setItems(List<TaskbarItem> items);

    /**
     * Syncs the visual selection to the given tool ID. May be called
     * independently of setItems() when a frame gains focus without the
     * item list changing.
     *
     * Pass null to clear selection.
     */
    void setSelectedId(String toolId);

    /**
     * Registers the single listener that receives user-initiated events
     * from this view. Called once at wiring time, before the view is shown.
     */
    void setListener(TaskbarViewListener listener);

}
