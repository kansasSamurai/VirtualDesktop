package org.jwellman.virtualdesktop.security;

import java.security.Permission;

/**
 *
 * @author Rick
 */
public class NoExitSecurityManager extends SecurityManager
    {
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
                        for (int i=0; i < howmany; i++) {
                           System.out.println(classes[i]);
                           if (classes[i].toString().contains("org.jwellman.virtualdesktop.App")) {
                               found = classes[i].toString().endsWith(".App");
                               if (found) break;
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

    }