/**
 * Core interfaces and abstractions for VirtualDesktop.
 *
 * <p>This module provides the shared contracts that both the Java 8 and Java 9+
 * implementations depend upon. It contains:</p>
 *
 * <ul>
 *   <li>Docking SPI interfaces (DockingService, DockingWorkspace, Dockable, etc.)</li>
 *   <li>Base abstractions (VirtualAppSpec interface, DesktopAction)</li>
 *   <li>Security classes (NoExitSecurityManager)</li>
 *   <li>Theme interfaces</li>
 * </ul>
 *
 * <p>This module has minimal dependencies to ensure it can be used across
 * different Java versions without compatibility issues.</p>
 */
package org.jwellman.virtualdesktop.core;
