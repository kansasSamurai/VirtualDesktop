package org.jwellman.bsh;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

/**
 * A clipboard listener that extends Thread to run in background.
 * This only processes text content.
 * 
 * Inspired by: https://medium.com/swlh/creating-a-clipboard-history-application-in-java-with-swing-16b006f7b322
 * 
 * @author rwellman
 *
 */
public class ClipboardListener extends Thread implements ClipboardOwner {

    private EntryListener entryListener;

    private final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

    public void run() {
        Transferable transferable = clipboard.getContents(this);
        regainOwnership(clipboard, transferable);

        while (true)
            ;
    }

    public void setEntryListener(EntryListener entryListener) {
        this.entryListener = entryListener;
    }

    // This is the callback for anything that wants to listen - typically a GUI
    public interface EntryListener {
        void onCopy(String data);
    }

  // ================ Interface : ClipboardOwner
  @Override
  public void lostOwnership(Clipboard c, Transferable t) {
    try {
      sleep(200);
    } catch (Exception e) {
    }

    Transferable contents = c.getContents(this);
    processContents(contents);
    regainOwnership(c, contents);
  }

  private void processContents(Transferable t) {
    try {
      final String what = (String) (t.getTransferData(DataFlavor.stringFlavor));

      // we alert our entry listener
      if (entryListener != null) {
        entryListener.onCopy(what);
      }
    } catch (Exception e) {
        // we don't care about exceptions; eventually we might log them 
    }
  }

  private void regainOwnership(Clipboard c, Transferable t) {
    c.setContents(t, this);
  }

}
