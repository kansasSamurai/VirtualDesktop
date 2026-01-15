package org.jwellman.vswing;

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
    
    private Image image;
    
    public VDesktopPane() {
        super();
        try {        
            final URL url = this.getClass().getClassLoader().getResource("org/jwellman/virtualdesktop/images/blankWall.jpg");
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
