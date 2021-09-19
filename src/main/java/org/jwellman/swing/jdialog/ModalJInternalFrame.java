package org.jwellman.swing.jdialog;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.awt.Container;
import java.awt.Component;
import java.awt.KeyboardFocusManager;

import javax.swing.JInternalFrame;
import javax.swing.JDesktopPane;

/**
 * This class uses some pieces of code from JOptionPane to make JInternalFrame behave modal.
 * 
 * This class is not yet used; see https://raw.githubusercontent.com/mzmine/mzmine1/master/src/net/sf/mzmine/userinterface/ModalJInternalFrame.java
 * I may need to exploit the code which uses privileged access
 * to start a modal "thread"/container.
 * 
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class ModalJInternalFrame extends JInternalFrame {

    private JDesktopPane desktop;

    public void showModal(JDesktopPane _desktop) {

        desktop = _desktop;

        desktop.add(this);
        setVisible(true);
        setLocation(( desktop.getWidth()-getWidth() ) / 2,  ( desktop.getHeight()-getHeight() ) / 2 );

        @SuppressWarnings("unused")
        Component fo = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

        /* Since all input will be blocked until this dialog is dismissed,
         * make sure its parent containers are visible first (this component
         * is tested below).  This is necessary for JApplets, because
         * because an applet normally isn't made visible until after its
         * start() method returns -- if this method is called from start(),
         * the applet will appear to hang while an invisible modal frame
         * waits for input.
         */
        if (isVisible() && !isShowing()) {
            Container parent = getParent();
            while (parent != null) {
                if (parent.isVisible() == false) {
                    parent.setVisible(true);
                }
                parent = parent.getParent();
            }
        }

        // Use reflection to get Container.startLWModal.
        try {
            Object obj = AccessController.doPrivileged(
                    new ModalPrivilegedAction( Container.class, "startLWModal") );
            if (obj != null) {
                ((Method)obj).invoke(this, (Object[])null);
            }
        } catch (IllegalAccessException ex) {
            System.err.println(ex.toString());
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.toString());
        } catch (InvocationTargetException ex) {
            System.err.println(ex.toString());
        }

    }

    public void disposeModal() {
        // !Added later!

        // Use reflection to get Container.startLWModal.
        try {
            Object obj = AccessController.doPrivileged(
                    new ModalPrivilegedAction( Container.class, "stopLWModal" ) );
            if (obj != null) {
                ((Method)obj).invoke(this, (Object[])null);
            }
        } catch (IllegalAccessException ex) {
            System.err.println(ex.toString());
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.toString());
        } catch (InvocationTargetException ex) {
            System.err.println(ex.toString());
        }

        dispose();
        desktop.remove(this);

    }

    /**
     * Retrieves a method from the provided class and makes it accessible.
     */
    private static class ModalPrivilegedAction implements PrivilegedAction {
        
        private Class clazz;
        private String methodName;

        public ModalPrivilegedAction(Class clazz, String methodName) {
            this.clazz = clazz;
            this.methodName = methodName;
        }

        public Object run() {
            Method method = null;
            try {
                method = clazz.getDeclaredMethod(methodName, (java.lang.Class[])null);
            } catch (NoSuchMethodException ex) {
            }
            if (method != null) {
                method.setAccessible(true);
            }
            return method;
        }
    }

    private static final long serialVersionUID = 1L;
    
}
