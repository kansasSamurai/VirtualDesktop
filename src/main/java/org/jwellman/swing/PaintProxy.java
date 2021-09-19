package org.jwellman.swing;

import java.awt.Graphics ;

import javax.swing.JComponent ;

public interface PaintProxy {

    public void paintComponent(Graphics g, JComponent c);
    
}
