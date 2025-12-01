package org.jwellman.virtualdesktop.vapps;

/**
 * An interface to mark vapps that support the launch method.
 * This is a flavor of the Command pattern.
 * 
 * @author rwellman
 *
 */
public interface LaunchAware {

    void launch() throws Exception;

}
