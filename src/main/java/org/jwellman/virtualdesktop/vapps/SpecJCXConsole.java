package org.jwellman.virtualdesktop.vapps;

import org.jwellman.jcx.JCXConsole;

/**
 *
 * @author Rick Wellman
 */
public class SpecJCXConsole extends VirtualAppSpec {

    public SpecJCXConsole() {
        super();

        this.setTitle("JCX Console");

        JCXConsole app = new JCXConsole();
        this.setContent( app );

    }

}
