package org.jwellman.swing_applets;

import javax.swing.JComponent;

/**
 * A very generic interface for using "injectable" view providers using Swing.
 * 
 * The typical intent is that a "wrapper"/container component such as JPanel
 * is returned; however, nothing really prevents a view creator from creating
 * "low level" components such as JLabels, JButtons, etc. 
 * 
 * The api object can be considered optional; it can be used whenever 
 * another object has "helper" functions which can assist with creating
 * the view based on the model.
 * 
 * @author rwellman
 *
 */
public interface SwingViewCreator {

	public JComponent createView(Object model, Object api);
	
}
