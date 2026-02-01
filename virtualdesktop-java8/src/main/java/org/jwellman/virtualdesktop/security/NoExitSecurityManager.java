package org.jwellman.virtualdesktop.security;

import java.security.Permission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rick
 */
public class NoExitSecurityManager extends SecurityManager {

    private static final Logger LOG = LoggerFactory.getLogger(NoExitSecurityManager.class);

    @Override
    public void checkPermission(Permission perm) {
        // allow anything.
    }

    @Override
    public void checkPermission(Permission perm, Object context) {
        // allow anything.
    }

    @Override
    public void checkExit(int status) {
        int dothis = 3;
        switch (dothis) {
            case 1:
                super.checkExit(status);
                break;
            case 2:
                break; // do nothing; i.e. default behavior
            case 3:
                boolean found = false;
                @SuppressWarnings("rawtypes") Class classes[] = this.getClassContext();
                final int howmany = classes.length;
                for (int i = 0; i < howmany; i++) {
                    LOG.trace("{}", classes[i]);
                    if (classes[i].toString().contains("org.jwellman.virtualdesktop.App")) {
                        found = classes[i].toString().endsWith(".App");
                        if (found) {
                            break;
                        }
                    }
                }
                if (!found) {
                    throw new ExitException(status);
                }
                break;
            case 99:
                throw new ExitException(status);
        }
    }

    /*
    This is the callstack when shutting down with the security manager in place:
        class org.jwellman.virtualdesktop.security.NoExitSecurityManager
        class java.lang.Runtime
        class java.lang.System
        class org.jwellman.virtualdesktop.App

    This is the callstack when clicking the JFrame close button (before security manager):
        class org.jwellman.virtualdesktop.security.NoExitSecurityManager
        class java.lang.Runtime
        class java.lang.System
        class javax.swing.JFrame
        class java.awt.Window
        class java.awt.Component
        class java.awt.Container
        class java.awt.Window
        class java.awt.Component
        class java.awt.EventQueue
        class java.awt.EventQueue
        class java.awt.EventQueue$3
        class java.awt.EventQueue$3
        class java.security.ProtectionDomain$JavaSecurityAccessImpl
        class java.security.ProtectionDomain$JavaSecurityAccessImpl
        class java.awt.EventQueue$4
        class java.awt.EventQueue$4
        class java.security.ProtectionDomain$JavaSecurityAccessImpl
        class java.awt.EventQueue
        class java.awt.EventDispatchThread
        class java.awt.EventDispatchThread
        class java.awt.EventDispatchThread
        class java.awt.EventDispatchThread
        class java.awt.EventDispatchThread
        class java.awt.EventDispatchThread

    */
}