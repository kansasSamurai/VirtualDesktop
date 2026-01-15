package org.jwellman.virtualdesktop.vapps;

import groovy.ui.Console;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * A vapp specification for the Groovy Console
 * TODO 12/5/2019 :: this works but maybe I should make this a "groovy class"!?
 * TODO 12/6/2019 :: the only bug I see at the moment is... if I close the original external JFrame
 *                   then the "capture standout output/error" feature no longer works.
 * TODO 12/6/2019 :: another bug I just found because I did not know it was a feature is... drag/drop
 *                   supposedly you can drop a .groovy file from the file browser to open it
 *
 * @author rwellman
 */
public class SpecGroovyConsole extends VirtualAppSpec {

    public SpecGroovyConsole() {
        super();

        // the framework handles the title in both scenarios regarding 'internalFrameProvider'
        this.setTitle("Groovy Console");

        this.internalFrameProvider = true;
        if (this.internalFrameProvider) {
        	// do nothing here... it will be done in populateInternalFrame()
        } else {
        	// populateInternalFrame() will NOT be called so do "stuff" here...
        	// This works but results in an external JFrame...
            this.setContent(new JPanel());

	        Console c = new Console();
	        c.run();
        }
    }

    @Override
    public void populateInternalFrame(JInternalFrame iframe, JDesktopPane desktop) {
    	System.out.println("SpecGroovyConsole::populateInternalFrame()");

    	boolean trySomethingElse = false;
    	if (trySomethingElse) {
    		// 12/5/2019... I thought this worked once...
    		// maybe I thought it would but never finished the POC...
            final HashMap<String, Component> map = new HashMap<>();
            map.put("rootContainerDelegate", iframe);

            Console c = new Console();
            c.run(map); // this is a hack! not sure if it will even work

    	} else {

    		// This works ...
	        Console c = new Console(); c.run();
	        // TODO use a jprogressbar since c.run() takea a little bit of time.
	        
	        final JFrame jframe = (JFrame) c.getFrame();
	        final JPanel content = this.createDefaultContent(jframe.getContentPane());
	        this.setContent(content);

    		iframe.add(this.getContent());
      		iframe.setJMenuBar(jframe.getRootPane().getJMenuBar());
    		// DONE apparently the groovy .png is "big"... resize to look better in the DesktopManager
    		iframe.setFrameIcon(new ImageIcon( jframe.getIconImage().getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH) ));

    		SwingUtilities.invokeLater(() -> {
    			// TODO this is required because the edit panel and output panel do not have preferred sizes
    			iframe.setPreferredSize(new Dimension(200, 300)); 
        		iframe.pack();
        		iframe.setVisible(true);

        		// Remove the original jframe by setting its visibility to false
                jframe.setVisible(false);
    		} );

    	}

    }

    /**
     * This is the original version before the desktop was added to the API;
     * thus allowing a new internal frame to be created and added to the desktop.
     *
     * @param frame
     */
    public void populateInternalFrame(JInternalFrame frame) {
    	System.out.println("populateInternalFrame()");

    	boolean trySomethingElse = false;
    	if (trySomethingElse) {
    		// 12/5/2019... I thought this worked once...
    		// maybe I thought it would but never finished the POC...
            final HashMap<String, Component> map = new HashMap<>();
            map.put("rootContainerDelegate", frame);

            Console c = new Console();
            c.run(map); // this is a hack! not sure if it will even work

    	} else {

    		// This works ...
	        Console c = new Console(); c.run();
	        final JFrame jframe = (JFrame) c.getFrame();
            this.setContent(this.createDefaultContent(jframe.getContentPane()));

    		frame.add(this.getContent());
      		frame.setJMenuBar(jframe.getRootPane().getJMenuBar());
    		// DONE apparently the groovy .png is "big"... resize to look better in the DesktopManager
    		frame.setFrameIcon(new ImageIcon( jframe.getIconImage().getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH) ));

    		SwingUtilities.invokeLater(() -> {
    			// TODO this is required because the edit panel and output panel do not have preferred sizes
    			frame.setPreferredSize(new Dimension(200, 300));

        		frame.pack();
        		frame.setVisible(true);
    		} );

    	}


    }

    // I'm not using this at the moment but it was found as an alternative to the resizing
    // code above; not sure of the pros/cons of either approach.  FWIW, The approach used
    // above is a lot less code.
    @SuppressWarnings("unused")
	private Image getScaledImage(Image srcImg, int w, int h){
        BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImg.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(srcImg, 0, 0, w, h, null);
        g2.dispose();

        return resizedImg;
    }

    /**
     * Converts a given Image into a BufferedImage
     * ... most often used when the underlying Image is
     * ... an instance of sun.java.awt.ToolkitImage, and
     * ... you need a BufferedImage for things like
     * ... calling getScaledInstance()
     *
     * @param img The Image to be converted
     * @return The converted BufferedImage
     */    public static BufferedImage toBufferedImage(Image img) {

        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }

}
