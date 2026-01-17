package org.jwellman.virtualdesktop.taskbar;

/**
 * Indicates the docking status of a tool for visual display in the taskbar.
 *
 * @author rwellman
 */
public enum DockingIndicator {

    /** Tool has its original panel and no external content */
    NORMAL,

    /** Tool's original panel has been docked out to another frame */
    DOCKED_OUT,

    /** Tool has received panels from other tools */
    HAS_EXTRA_CONTENT,

    /** Tool has both docked out panels and received external content */
    MIXED;

    /**
     * Determine the indicator based on docking state flags.
     *
     * @param originalPanelPresent true if the tool's original panel is still in its frame
     * @param hasExternalContent true if the tool has panels from other tools
     * @return the appropriate indicator
     */
    public static DockingIndicator fromState(boolean originalPanelPresent, boolean hasExternalContent) {
        if (!originalPanelPresent && hasExternalContent) {
            return MIXED;
        } else if (!originalPanelPresent) {
            return DOCKED_OUT;
        } else if (hasExternalContent) {
            return HAS_EXTRA_CONTENT;
        } else {
            return NORMAL;
        }
    }

}
