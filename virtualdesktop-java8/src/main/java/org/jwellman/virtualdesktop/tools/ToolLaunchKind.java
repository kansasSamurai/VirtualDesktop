package org.jwellman.virtualdesktop.tools;

/**
 * How a catalogued tool is started.
 */
public enum ToolLaunchKind {

    /** Instantiates a VirtualAppSpec (or ToolSpec) by class name. */
    INTERNAL,

    /** Launches an OS process via ExternalAppSpec / LaunchAware. */
    EXTERNAL

}
