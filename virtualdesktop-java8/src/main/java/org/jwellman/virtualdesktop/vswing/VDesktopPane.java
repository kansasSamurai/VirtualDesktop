package org.jwellman.virtualdesktop.vswing;

import java.awt.Graphics;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JDesktopPane;
import javax.swing.TransferHandler;

/**
 * A simple extension to JDesktopPane that displays an image as the background.
 * This is intended for "full" background images; no provision for tiling has been made.
 * 
 * 12/19/2020 : Added the transfer handler for the "backgroundFile" property.
 * This has been proven to work with the filenavigator.bsh script using
 * the zz() "imageview".
 * 
 * @author rwellman
 */
public class VDesktopPane extends JDesktopPane {

    private static final String path = "org/jwellman/virtualdesktop/images/";

    private static final String[] images = { "blankWall.jpg", "railroadTracks.jpg", "javaBeans.jpg"};

    private File backgroundFile;
    
	private Image image;

    public VDesktopPane() {
        super();

        this.setTransferHandler(new TransferHandler("backgroundFile"));
        
        try {
            final URL url = this.getClass().getClassLoader().getResource(path + images[0]);
            this.image = ImageIO.read(url);
        } catch (IOException ex) {
            Logger.getLogger(VDesktopPane.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public File getBackgroundFile() {
        return this.backgroundFile;
    }
    
    public void setBackgroundFile(File f) {
        this.backgroundFile = f;
        
        try {
            final URL url = f.toURI().toURL();
            this.image = ImageIO.read(url);
            
            repaint();
            
        } catch (IOException ex) {
            Logger.getLogger(VDesktopPane.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
    }

    private static final long serialVersionUID = 1L;

}
