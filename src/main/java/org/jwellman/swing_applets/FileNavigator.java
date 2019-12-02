package org.jwellman.swing_applets;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.FileReader;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.jwellman.swing.jtree.FileNode;
import org.jwellman.swing.jtree.FileSelectorModel;

@SuppressWarnings("serial")
public class FileNavigator extends JPanel {

    private JTree tree;
    private JLabel status;
    private JTextArea preview;
    private JComponent viewer;

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
    	
    	// by design, support a default implementation when null
        if (view == null) view = new JLabel(); // new JTextArea();
        if (listener == null) listener = new ImagesTreeSelectionListener((JLabel) view) ; // new DefaultTreeSelectionListener();
    	
        tree = new JTree(new FileSelectorModel(directory));
        tree.addTreeSelectionListener(listener);

        status = new JLabel(directory);

        viewer = view;
        if (view instanceof JTextArea) {
            preview = (JTextArea) view;
            preview.setEditable(false);
            preview.setLineWrap(false);
            // preview.setWrapStyleWord(false);        	
        }

        this.add(BorderLayout.WEST, new JScrollPane(tree));
        this.add(BorderLayout.SOUTH, status);
        this.add(new JScrollPane(viewer));
    	
    }
    
    public void setStatusText(String newtext) {
    	this.status.setText(newtext);
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
                    ImageIcon icon = new ImageIcon(selectedNode.getAbsolutePath());
                    labelasview.setIcon(icon);                
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

