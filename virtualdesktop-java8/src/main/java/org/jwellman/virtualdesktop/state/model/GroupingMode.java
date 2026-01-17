package org.jwellman.virtualdesktop.state.model;

/**
 * Enumeration of taskbar grouping modes.
 *
 * @author rwellman
 */
public enum GroupingMode {

    /** No grouping - tools listed individually */
    NONE,

    /** Group by tool type (class name) */
    BY_TYPE,

    /** Group by docking relationship (future) */
    BY_DOCKING

}
