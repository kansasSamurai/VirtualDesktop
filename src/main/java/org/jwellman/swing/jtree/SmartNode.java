package org.jwellman.swing.jtree;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * 
 * @author Rick Wellman
 *
 */
public class SmartNode extends DefaultMutableTreeNode {

	private static final long serialVersionUID = 1L;

	public SmartNode() {
		super();
	}

	public SmartNode(String value) {
		super(value);		
	}
	
	public SmartNode(Object o) {
		super(o);
	}
	
	public SmartNode(String value, Object[] array) {
		this(value);
		
		for (Object o : array) {
			this.add(new SmartNode(o));
		}
	}
	
}
