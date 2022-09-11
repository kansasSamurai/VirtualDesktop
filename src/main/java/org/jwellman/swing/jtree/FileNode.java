package org.jwellman.swing.jtree;

import java.util.ArrayList;
import java.util.List;

/**
 * A node class for JTrees that supports viewing the filesystem.
 * 
 * So that this class can support many use-cases(), it is a design goal to 
 * keep it very simple.  You will note that by extending the java.io.File class,
 * we have merely created this as an adapter to support the JTree model.
 * 
 * TODO After thinking this through... I need to remove the isText() and isImage()
 * features.  These originated from proof of concept code so I kept them; 
 * however, I want this class to support being in a multi-app/use-case 
 * environment and use of the static arrays would not support that
 * and honestly I don't think they should be here in the first place.
 * This type of logic would be better placed outside of this class.
 * 
 * @author rwellman
 *
 */
@SuppressWarnings("serial")
public class FileNode extends java.io.File {

	// false by default is what we want (for convenience)
	private boolean isRoot;
	
	private static final List<String> textTypes = new ArrayList<>();
	
	private static final List<String> imageTypes = new ArrayList<>();
	
    public FileNode(String directory) {
        super(directory);
    }

    public FileNode(FileNode parent, String child) {
        super(parent, child);        
    }

    @Override
    public String toString() {
    	if (this.isRoot()) {
    		final String path = this.getPath();
    		return path;
    	} else {
    		return this.getName();
    	}
    }

    public static void addTextTypes(String delimited) {
        setTextTypes(delimited);
    }

    public static void setTextTypes(String delimited) {
    	final String normalized = delimited.toLowerCase();
    	for (String onetype : normalized.split(";")) {
    	    if (onetype.startsWith(".")) {
                textTypes.add(onetype);    	        
    	    } else {
    	        System.out.println("file types/extensions must start with a period '.'");
    	    }
    	}
    }
    
    public static void setImageTypes(String delimited) {
    	final String normalized = delimited.toLowerCase();
        for (String onetype : normalized.split(";")) {
            if (onetype.startsWith(".")) {
                imageTypes.add(onetype);             
            } else {
                System.out.println("file types/extensions must start with a period '.'");
            }
        }
    }
    
    public boolean isImage() {
    	if (imageTypes == null) setImageTypes(".png;.jpg;.gif;.ico");

		final String lowername = this.getName().toLowerCase();
		for (String tryme : imageTypes) {
			if (lowername.endsWith(tryme)) return true;
		}

		return false;
    }
    
	public boolean isText() {
		if (textTypes == null) setTextTypes(".txt;.csv;.tsv;.xml;.html;.java");

		final String lowername = this.getName().toLowerCase();
		for (String tryme : textTypes) {
			if (lowername.endsWith(tryme)) return true;
		}

		return false;
	}
    
	/**
	 * @return the isRoot
	 */
	public boolean isRoot() {
		return isRoot;
	}

	/**
	 * @param isRoot the isRoot to set
	 */
	public void setRoot(boolean isRoot) {
		this.isRoot = isRoot;
	}

}
