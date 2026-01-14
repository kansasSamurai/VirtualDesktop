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

    private final JTextArea textarea;

    public ThreadedSwingHandler(InputStream is, JTextArea ta) {
        super(is);
        this.textarea = ta;
    }

    @Override
    public void run() {
        super.run();

//        SwingUtilities.invokeLater(new Runnable() {
//            @Override public void run() {
//                // redirects data to the text area
//                textarea.append("===== output buffer =====\n");
//                textarea.append(outputBuffer.toString());
//                textarea.append("\n");
//            }
//        });
    }

    @Override
    protected void lineOut(final String line) {
        super.lineOut ( line );
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                
                // redirects data to the text area
                textarea.append(line);
                textarea.append("\n");
//                textarea.append("└───hsqldb\n");
//                textarea.append("│   ├───bin\n");

                // scrolls the text area to the end of data // TODO make this a user option
                boolean scroll = false;
                int position = scroll ? textarea.getDocument().getLength() : 0;
                textarea.setCaretPosition(position);
            }
        });
    }

}
