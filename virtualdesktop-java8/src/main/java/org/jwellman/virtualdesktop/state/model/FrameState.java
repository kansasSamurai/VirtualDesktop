package org.jwellman.virtualdesktop.state.model;

/**
 * Enumeration of possible frame states for a tool window.
 *
 * @author rwellman
 */
public enum FrameState {

    /** Normal, non-minimized, non-maximized state */
    NORMAL,

    /** Minimized/iconified state */
    MINIMIZED,

    /** Maximized state */
    MAXIMIZED,

    /** Hidden but not closed */
    HIDDEN

}
