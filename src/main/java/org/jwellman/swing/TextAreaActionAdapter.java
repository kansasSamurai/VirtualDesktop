package org.jwellman.swing;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * A key adapter that will allow the enter key to 
 * click an associated action button.
 * 
 * @author Rick Wellman
 *
 */
public class TextAreaActionAdapter extends KeyAdapter implements Runnable {

	private JTextArea textarea;
	private JButton command;

	public TextAreaActionAdapter(JTextArea target, JButton button) {
		this.textarea = target;
		this.command = button;
		
		this.textarea.addKeyListener(this);
	}
	
	@Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
        	textarea.selectAll();
        	command.doClick();
        	
            e.consume();
            
//            SwingUtilities.invokeLater(this);
        }
    }

	@Override
	public void run() {
		textarea.requestFocusInWindow();
	}
	
}
