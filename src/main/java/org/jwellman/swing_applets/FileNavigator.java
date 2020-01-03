package org.jwellman.swing_applets;

import java.awt.BorderLayout;
import java.awt.Dimension;
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
	private final Dimension dimFilePreview = new Dimension(149,149);
	private final String[] imageTypes = ".png;.jpg;.gif;.ico".split(";");


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
            if (listener == null) listener = new ImagesPanelTreeSelectionListener((JPanel) view);
    		break;    		
    	}
        viewer = view;
        if (view instanceof JTextArea) {
            preview = (JTextArea) view;
            preview.setEditable(false);
            preview.setLineWrap(false);
            // preview.setWrapStyleWord(false);        	
        }
    	
        tree = new JTree(new FileSelectorModel(directory));
        tree.addTreeSelectionListener(listener);

        this.add(status, BorderLayout.SOUTH);
        this.add(new JScrollPane(tree), BorderLayout.WEST);
        this.add(new JScrollPane(viewer), BorderLayout.CENTER);
    	
    }
    
    public void setStatusText(String newtext) {
    	this.status.setText(newtext);
    }

    class ImagesPanelTreeSelectionListener implements TreeSelectionListener {

    	JPanel panelasview;
    	
    	public ImagesPanelTreeSelectionListener(JPanel view) {
    		panelasview = view;
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
	                	
	                	final File[] files = selectedNode.listFiles();
	                	for (File thisfile : files) {
	                		if (this.isImageFile(thisfile)) {
	    		                // Update the file preview window...
	    	                	button = new JButton(thisfile.getName());
	    	                	button.setMinimumSize(dimFilePreview);
	    	                	button.setMaximumSize(dimFilePreview);
	    	                	button.setPreferredSize(dimFilePreview);
	    	                	panelasview.add(button);	                			
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
    
}

