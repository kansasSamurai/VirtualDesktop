package org.jwellman.virtualdesktop.vapps;

import org.jwellman.jcx.JCXConsole;

/**
 *
 * @author Rick Wellman
 */
public class SpecJCXConsole extends VirtualAppSpec {

    public SpecJCXConsole() {
        super();

        final JCXConsole app = new JCXConsole();

        this.setTitle("JCX Console");
        this.setContent( app );

    }

}
