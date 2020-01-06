package org.jwellman.swing_applets;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.jwellman.swing.jpanel.OverflowX;
import org.jwellman.swing.jtree.FileNode;
import org.jwellman.swing.jtree.FileSelectorModel;

import net.sf.image4j.codec.ico.ICODecoder;

@SuppressWarnings("serial")
public class FileNavigator extends JPanel {

    private JTree tree;
    private JLabel status;
    private JTextArea preview;
    private JComponent viewer;
    private SwingViewCreator imageView;
	private final Dimension dimFilePreview = new Dimension(149,149);
	private final String[] imageTypes = ".png;.jpg;.gif;.ico".split(";");
	private final ImageScaler SCALER = new ImageScaler();

    /**
     * Create a FileNavigator whose root is the directory specified.
     * This constructor will result in the default implementation of a simple
     * text file viewer; to provide custom behavior, either:<ul>
     * <li>call setView() and setListener() after using this constructor, or</li>
     * <li>use the full constructor FileNavigator(String directory, JComponent view, TreeSelectionListener listener)</li>
     * </ul>
     * 
     * @param directory
     */
    public FileNavigator(String directory) {
    	this(directory, null, null);
    }
    
    public FileNavigator(String directory, JComponent view, TreeSelectionListener listener) {
    	super(new BorderLayout());    	
    	
        status = new JLabel(directory);

    	// by design, support a default implementation when null
    	int design = 3;
    	switch (design) {
    	case 1:
    		if (view == null) view = new JTextArea();
            if (listener == null) listener = new DefaultTreeSelectionListener();
    		break;
    	case 2:
            if (view == null) view = new JLabel(); 
            if (listener == null) listener = new ImagesTreeSelectionListener((JLabel) view); 
    		break;
    	case 3:
            if (view == null) view = new OverflowX(); 
            if (listener == null) listener = new ImagesPanelTreeSelectionListener((JPanel) view, this);
    		break;    		
    	}
        viewer = view;
        if (view instanceof JTextArea) {
            preview = (JTextArea) view;
            preview.setEditable(false);
            preview.setLineWrap(false);
            // preview.setWrapStyleWord(false);        	
        }
    	
        tree = new JTree(new FileSelectorModel(directory, true));
        tree.addTreeSelectionListener(listener);

        this.add(status, BorderLayout.SOUTH);
        this.add(new JScrollPane(tree), BorderLayout.WEST);
        this.add(new JScrollPane(viewer), BorderLayout.CENTER);
    	
    }
    
    public void setStatusText(String newtext) {
    	this.status.setText(newtext);
    }

    public SwingViewCreator getImageView() {
		return imageView;
	}

	public void setImageView(SwingViewCreator imageView) {
		this.imageView = imageView;
	}

	public ImageIcon createFileIcon(File thisfile) {
    	ImageIcon icon = null;
    	
		final String lowername = thisfile.getName().toLowerCase();
		if (lowername.endsWith(".ico")) {
			// Java does not natively support the .ico format; use image4j
			// http://zetcode.com/articles/javaico/
			try {
				icon = new ImageIcon( ICODecoder.read(thisfile).get(0) );
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} else {
            icon = new ImageIcon(thisfile.getAbsolutePath());        			
		}
		
		return icon;
	}
	
	public BufferedImage createScaledImage(ImageIcon icon, int w, int h) {
    	return SCALER.scaleImage( icon, BufferedImage.TYPE_INT_RGB, w, h);			
	}		
	
	class ImagesPanelTreeSelectionListener implements TreeSelectionListener {

    	JPanel panelasview;
    	FileNavigator navigator;
    	
    	public ImagesPanelTreeSelectionListener(JPanel view, FileNavigator fnav) {
    		panelasview = view;
    		navigator = fnav;
    	}
    	
		@Override
		public void valueChanged(TreeSelectionEvent e) {
            final FileNode selectedNode = (FileNode) tree.getLastSelectedPathComponent();            
            if (selectedNode == null) {
                // Update the status bar...
                status.setText("UNEXPECTED NULL NODE");
            } else {
                // Update the status bar...
                status.setText(selectedNode.getAbsolutePath() + " (" + selectedNode.length() + " bytes)");
                
            	panelasview.removeAll();
            	// panelasview.invalidate(); // according to javadocs, this is un-necessary due to use of removeAll()
            	
	                if (selectedNode.isDirectory()) {
	                	JButton button = null; // reusable objref
            			JComponent view = null;
	                	
	                	final File[] files = selectedNode.listFiles();
	                	for (File thisfile : files) {
	                		if (this.isImageFile(thisfile)) {
	                			if (imageView != null) {
	                				view = imageView.createView(thisfile, navigator);
	                			} else {
		    		                // Update the file preview window...
		    	                	button = new JButton(thisfile.getName());
		    	                	button.setMinimumSize(dimFilePreview);
		    	                	button.setMaximumSize(dimFilePreview);
		    	                	button.setPreferredSize(dimFilePreview);
		    	                	// button.setVerticalAlignment(SwingConstants.BOTTOM);
		    	                	button.setVerticalTextPosition(SwingConstants.BOTTOM);
		    	                	button.setHorizontalTextPosition(SwingConstants.CENTER);

		    	                    if (true) {
		    	                    	ImageIcon icon = null;
		    	                    	
		    	                		final String lowername = thisfile.getName().toLowerCase();
		    	                		if (lowername.endsWith(".ico")) {
		    	                			// Java does not natively support the .ico format; use image4j
		    	                			// http://zetcode.com/articles/javaico/
		    	                			try {
		    	    							icon = new ImageIcon( ICODecoder.read(thisfile).get(0) );
		    	    						} catch (IOException e1) {
		    	    							e1.printStackTrace();
		    	    						}
		    	                		} else {
		    	                            icon = new ImageIcon(thisfile.getAbsolutePath());        			
		    	                		}
		    	                		
		    	                        if (icon != null) {
		    	                        	// TODO consider using https://github.com/rkalla/imgscalr/ instead
		    	                        	BufferedImage image = SCALER.scaleImage( icon, BufferedImage.TYPE_INT_RGB, 100, 100);
		    	                        	button.setIcon(new ImageIcon(image));	    	                        
		    	                        }
		    	                    }            	

		    	                	view = button; // panelasview.add(button);
	                			}
	    	                	panelasview.add(view);
	    	                	
	                		}	                		
	                	}
	                } else {
	                	// Any other non-directory things?... none for now
	                }
                
            	panelasview.revalidate();

            }
		}

		private boolean isImageFile(File thisfile) {
			final String lowername = thisfile.getName().toLowerCase();
			for (String tryme : imageTypes) {
				if (lowername.endsWith(tryme)) return true;
			} return false;
		}
		
    }
    
    class ImagesTreeSelectionListener implements TreeSelectionListener {

    	JLabel labelasview;
    	
    	public ImagesTreeSelectionListener(JLabel view) {
    		labelasview = view;
    	}
    	
    	@Override
        public void valueChanged(TreeSelectionEvent e) {
            final FileNode selectedNode = (FileNode) tree.getLastSelectedPathComponent();            
            if (selectedNode != null) {
                // Update the status bar...
                status.setText(selectedNode.getAbsolutePath() + " (" + selectedNode.length() + " bytes)");
                
                // Update the file preview window...
                if (selectedNode.isFile() && selectedNode.isImage()) {
                	ImageIcon icon = null;
                	
            		final String lowername = selectedNode.getName().toLowerCase();
            		if (lowername.endsWith(".ico")) {
            			// Java does not natively support the .ico format; use image4j
            			// http://zetcode.com/articles/javaico/
            			try {
							icon = new ImageIcon( ICODecoder.read(selectedNode).get(0) );
						} catch (IOException e1) {
							e1.printStackTrace();
						}
            		} else {
                        icon = new ImageIcon(selectedNode.getAbsolutePath());            			
            		}

                    if (icon != null) labelasview.setIcon(icon);
                    
                }            	
            }
            
    	}
    }
    
    class DefaultTreeSelectionListener implements TreeSelectionListener {

    	@Override
        public void valueChanged(TreeSelectionEvent e) {
            final FileNode selectedNode = (FileNode) tree.getLastSelectedPathComponent();
            if (selectedNode != null) {
                // Update the status bar...
                status.setText(selectedNode.getAbsolutePath() + " (" + selectedNode.length() + " bytes)");
                
                // Update the file preview window...
                if (selectedNode.isFile() && selectedNode.isText()) {
                	// Erase the current contents...
                    preview.setText(null);
                    
                    // Fill with the new contents...
                    String line = ""; int linecount = 0;
                    try (BufferedReader br = new BufferedReader(new FileReader(selectedNode.getAbsolutePath()))) {
                        while ((line = br.readLine()) != null) {
                            preview.append(line);
                            preview.append(System.getProperty("line.separator"));
                            linecount++;
                            if (linecount > 500) {
                            	preview.append("\n... (preview is limited to 500 lines)");
                            	break;
                            }
                        }
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    }
                    
                    // Make sure we view the top of the file...
                    preview.setCaretPosition(0);

                } // endif (selectedNode.isFile() && selectedNode.isText())
            } // endif (selectedNode != null)
        }
    	
    }
    
    class ImageScaler {

    	/**
         * Scale an image while preserving aspect ratio
         *
         * @param args Name of image file to be scaled (testing only)
         *
         */
        public void main(String[] args) throws Exception {
	    	BufferedImage in = javax.imageio.ImageIO.read(new java.io.File(args[0]));
	    	BufferedImage out = new ImageScaler().scaleImage(in, BufferedImage.TYPE_INT_RGB, 100, 200);
	    	javax.imageio.ImageIO.write(out, "JPG", new java.io.File("scaled.jpg"));
        }

        public BufferedImage scaleImage(ImageIcon icon, int typeIntRgb, int w, int h) {
            final BufferedImage resizedImg = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            
            final Graphics2D g2 = resizedImg.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(icon.getImage(), 0, 0, icon.getIconWidth(), icon.getIconHeight(), null);
            g2.dispose();

            return scaleImage(resizedImg, typeIntRgb, w, h);
		}

		/**
         *
         * @param image The image to be scaled
         * @param imageType Target image type, e.g. TYPE_INT_RGB
         * @param newWidth The required width
         * @param newHeight The required width
         *
         * @return The scaled image
         */
        public BufferedImage scaleImage(BufferedImage image, int imageType, int newWidth, int newHeight) {

	    	// Make sure the aspect ratio is maintained, so the image is not distorted
	    	double thumbRatio = (double) newWidth / (double) newHeight;
	    	int imageWidth = image.getWidth(null);
	    	int imageHeight = image.getHeight(null);
	    	double aspectRatio = (double) imageWidth / (double) imageHeight;
	
	    	if (thumbRatio < aspectRatio) {
	    	    newHeight = (int) (newWidth / aspectRatio);
	    	} else {
	    	    newWidth = (int) (newHeight * aspectRatio);
	    	}
	
	    	// Draw the scaled image
	    	BufferedImage newImage = new BufferedImage(newWidth, newHeight, imageType);
	    	Graphics2D graphics2D = newImage.createGraphics();
	    	graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	    	graphics2D.drawImage(image, 0, 0, newWidth, newHeight, null);
	
	    	return newImage;
        }
        
    }
    
}
