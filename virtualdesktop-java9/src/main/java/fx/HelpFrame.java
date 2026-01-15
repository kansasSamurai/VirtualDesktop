/*
 XionDE.fm - XionDE File Manager
 Copyright (C) 2007 XionDE TEAM (lucky@vectorstar.net)

 This program can be distributed under the terms of the GNU GPL
 See the file COPYING.
 */
package fx;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URL;
import javax.swing.*;
import javax.swing.event.*;

/**
 * File eXplorer - help frame class
 * 
 * @author Rick Wellman
 */
public class HelpFrame extends JFrame {
    
    // main values
    private final String APP_PREF = "jar:file:", APP_SUF = "!", MAN_FILE = "manual.html";
    
    // components
    private final Dimension ROOT_SIZE = new Dimension(400, 400); // root size
    private JEditorPane helpPane = new JEditorPane();
    private JLabel statusBar = new JLabel();

    public HelpFrame(JFrame owner) {
        try {
            // setup frame
            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent ev) {
                    setVisible(false);
                }
            });
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            setFont(owner.getFont());
            setIconImage(owner.getIconImage());
            setLayout(new BorderLayout());
            setLocationByPlatform(true);
            setSize(ROOT_SIZE);
            setTitle("Manual");
            helpPane.addHyperlinkListener(new HyperlinkListener() {
                public void hyperlinkUpdate(HyperlinkEvent ev) {
                    if (ev.getEventType().equals(HyperlinkEvent.EventType.EXITED)) // cursor exited url
                    {
                        updateURL(" ");
                    } else {
                        updateURL(ev.getURL().toString());
                    }
                }
            });
            helpPane.addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyCode() == e.VK_ESCAPE) {
                        setVisible(false);
                    }
                }
            });
            helpPane.setContentType("text/html; charset=UTF-8");
            helpPane.setEditable(false);
            helpPane.setFont(getFont());

            final String x = fx.Main.adjustURL(APP_PREF + fx.Main.appPath + File.separator + fx.Main.MPLATFORM_BIN + APP_SUF + File.separator + MAN_FILE);
            helpPane.setPage(new URL(x));
            statusBar.addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyCode() == e.VK_ESCAPE) {
                        setVisible(false);
                    }
                }
            });
            statusBar.setFont(getFont());
            statusBar.setText(" ");
            getContentPane().add(new JScrollPane(helpPane), BorderLayout.CENTER);
            getContentPane().add(statusBar, BorderLayout.SOUTH);
        } catch (Exception ex) {
            fx.Main.killApp("help", "error while initializing");
        }
    }
    
    private void updateURL(String url) {
        statusBar.setText(url);
    }
    
    public void showHelp() {
        if (!isVisible()) { setVisible(true); }
        toFront();
    }
    
}
