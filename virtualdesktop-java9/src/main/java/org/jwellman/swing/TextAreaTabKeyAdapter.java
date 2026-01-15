package org.jwellman.swing;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JTextArea;

/**
 * A key adapter that will allow the tab key to transfer focus
 * instead of the default behavior of inserting a tab character.
 * 
 * @author Rick Wellman
 *
 */
public class TextAreaTabKeyAdapter extends KeyAdapter {

	private JTextArea textArea;
	
	public TextAreaTabKeyAdapter(JTextArea target) {
		this.textArea = target;
		
		target.addKeyListener(this);
	}
	
	@Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_TAB) {
            if (e.getModifiers() > 0) {
                textArea.transferFocusBackward();
            } else {
                textArea.transferFocus();
            }
            e.consume();
        }
    }
	
}
