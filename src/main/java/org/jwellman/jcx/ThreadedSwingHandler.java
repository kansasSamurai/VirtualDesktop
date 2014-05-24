package org.jwellman.jcx;

import java.io.InputStream;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * This subclass is meant to generically handle Swing components.
 *
 * @author Rick Wellman
 */
public class ThreadedSwingHandler extends ThreadedStreamHandler {

    private JTextArea textarea;

    public ThreadedSwingHandler(InputStream is, JTextArea ta) {
        super(is);
        this.textarea = ta;
    }

    @Override
    protected void lineOut(final String line) {
        SwingUtilities.invokeLater(new Runnable() {
              @Override public void run() {
                // redirects data to the text area
                textarea.append(line);
                textarea.append("\n");
                // scrolls the text area to the end of data
                textarea.setCaretPosition(textarea.getDocument().getLength());
              }
        });
    }

}
