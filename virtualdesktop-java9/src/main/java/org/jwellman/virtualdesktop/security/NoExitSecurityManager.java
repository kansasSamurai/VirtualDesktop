package org.jwellman.virtualdesktop.security;

import java.security.Permission;

/**
 *
 * @author Rick
 */
public class NoExitSecurityManager extends SecurityManager {

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
                Class classes[] = this.getClassContext();
                final int howmany = classes.length;
                for (int i = 0; i < howmany; i++) {
                    System.out.println(classes[i]);
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
    For reference, his is the callstack when clicking the JFrame close button:
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