package org.jwellman.virtualdesktop.vswing;

import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JDesktopPane;

/**
 *
 * @author rwellman
 */
public class VDesktopPane extends JDesktopPane {

	private static final long serialVersionUID = 1L;

    private static final String path = "org/jwellman/virtualdesktop/images/";

    private static final String[] images = { "blankWall.jpg", "railroadTracks.jpg", "javaBeans.jpg"};

	private Image image;

    public VDesktopPane() {
        super();
        try {
            final URL url = this.getClass().getClassLoader().getResource(path + images[1]);
            this.image = ImageIO.read(url);
        } catch (IOException ex) {
            Logger.getLogger(VDesktopPane.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
    }

}
