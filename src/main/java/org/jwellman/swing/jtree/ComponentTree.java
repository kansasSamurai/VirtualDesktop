package org.jwellman.swing.jtree;

/*
 * Copyright (c) 2004 David Flanagan.  All rights reserved.
 * 
 * This code is from the book Java Examples in a Nutshell, 3nd Edition.
 * It is provided AS-IS, WITHOUT ANY WARRANTY either expressed or implied.
 * You may study, use, and modify it for any non-commercial purpose,
 * including teaching and use in open-source projects.
 * You may distribute it non-commercially as long as you retain this notice.
 * For a commercial use license, or to purchase the book, 
 * please visit http://www.davidflanagan.com/javaexamples3.
 */

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * This class is a JTree subclass that displays the tree of AWT or Swing
 * component that make up a GUI.
 */
@SuppressWarnings("serial")
public class ComponentTree extends JTree {

	/**
	 * All this constructor method has to do is set the TreeModel and
	 * TreeCellRenderer objects for the tree. It is these classes (defined below)
	 * that do all the real work.
	 */
	public ComponentTree(Component c) {
		super(new ComponentTreeModel(c));
		setCellRenderer(new ComponentCellRenderer(getCellRenderer()));
	}

	/**
	 * The TreeModel class puts hierarchical data in a form that the JTree can
	 * display. This implementation interprets the containment hierarchy of a
	 * Component for display by the ComponentTree class. Note that any kind of
	 * Object can be a node in the tree, as long as the TreeModel knows how to
	 * handle it.
	 */
	static class ComponentTreeModel implements TreeModel {
		Component root; // The root object of the tree

		// Constructor: just remember the root object
		public ComponentTreeModel(Component root) {
			this.root = root;
		}

		// Return the root of the tree
		public Object getRoot() {
			return root;
		}

		// Is this node a leaf? (Leaf nodes are displayed differently by JTree)
		// Any node that isn't a container is a leaf, since they cannot have
		// children. We also define containers with no children as leaves.
		public boolean isLeaf(Object node) {
			if (!(node instanceof Container))
				return true;
			Container c = (Container) node;
			return c.getComponentCount() == 0;
		}

		// How many children does this node have?
		public int getChildCount(Object node) {
			if (node instanceof Container) {
				Container c = (Container) node;
				return c.getComponentCount();
			}
			return 0;
		}

		// Return the specified child of a parent node.
		public Object getChild(Object parent, int index) {
			if (parent instanceof Container) {
				Container c = (Container) parent;
				return c.getComponent(index);
			}
			return null;
		}

		// Return the index of the child node in the parent node
		public int getIndexOfChild(Object parent, Object child) {
			if (!(parent instanceof Container))
				return -1;
			Container c = (Container) parent;
			Component[] children = c.getComponents();
			if (children == null)
				return -1;
			for (int i = 0; i < children.length; i++) {
				if (children[i] == child)
					return i;
			}
			return -1;
		}

		// This method is only required for editable trees, so it is not
		// implemented here.
		public void valueForPathChanged(TreePath path, Object newvalue) {
		}

		// This TreeModel never fires any events (since it is not editable)
		// so event listener registration methods are left unimplemented
		public void addTreeModelListener(TreeModelListener l) {
		}

		public void removeTreeModelListener(TreeModelListener l) {
		}
	}

	/**
	 * A TreeCellRenderer displays each node of a tree. The default renderer
	 * displays arbitrary Object nodes by calling their toString() method. The
	 * Component.toString() method returns long strings with extraneous information.
	 * Therefore, we use this "wrapper" implementation of TreeCellRenderer to
	 * convert nodes from Component objects to useful String values before passing
	 * those String values on to the default renderer.
	 */
	static class ComponentCellRenderer implements TreeCellRenderer {

		TreeCellRenderer renderer; // The renderer we are a wrapper for

		// Constructor: just remember the renderer
		public ComponentCellRenderer(TreeCellRenderer renderer) {
			this.renderer = renderer;
		}

		// This is the only TreeCellRenderer method.
		// Compute the string to display, and pass it to the wrapped renderer
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
				boolean leaf, int row, boolean hasFocus) {

			String newvalue = value.getClass().getName(); // Component type

			String name = ((Component) value).getName(); // Component name
			if (name != null)
				newvalue += " (" + name + ")"; // unless null

			// Use the wrapped renderer object to do the real work
			return renderer.getTreeCellRendererComponent(tree, newvalue, selected, expanded, leaf, row, hasFocus);
		}

	}

	/**
	 * A "factory" method to create a JPanel with a ComponentTree in a ScrollPane;
	 * this serves to be used in simple examples/implementations and its
	 * source code is an example of how to start writing custom implementations.
	 * 
	 * @param root
	 * @return
	 */
	public static JPanel createTree(JComponent root) {
		final JPanel panel = new JPanel(new BorderLayout());
		
		// Only allow a single item in the tree to be selected at once
		final JTree tree = new ComponentTree(root);
		tree.setFont(new Font("SansSerif", Font.BOLD, 12));
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		
		final JScrollPane scrollpane = new JScrollPane();
		scrollpane.setViewportView(tree);

		panel.add(scrollpane, BorderLayout.CENTER);		
		return panel;
	}
	
	/**
	 * This main() method demonstrates the use of the ComponentTree class: it puts a
	 * ComponentTree component in a Frame, and uses the ComponentTree to display its
	 * own GUI hierarchy. It also adds a TreeSelectionListener to display additional
	 * information about each component as it is selected
	 */
	public static void main(String[] args) {
		// Create a frame for the demo, and handle window close requests
		JFrame frame = new JFrame("ComponentTree Demo");
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		// Create a scroll pane and a "message line" and add them to the
		// center and bottom of the frame.
		JScrollPane scrollpane = new JScrollPane();
		final JLabel msgline = new JLabel(" ");
		frame.getContentPane().add(scrollpane, BorderLayout.CENTER);
		frame.getContentPane().add(msgline, BorderLayout.SOUTH);

		// Now create the ComponentTree object, specifying the frame as the
		// component whose tree is to be displayed. Also set the tree's font.
		JTree tree = new ComponentTree(frame);
		tree.setFont(new Font("SansSerif", Font.BOLD, 12));

		// Only allow a single item in the tree to be selected at once
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		// Add an event listener for notifications when
		// the tree selection state changes.
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				// Tree selections are referred to by "path"
				// We only care about the last node in the path
				TreePath path = e.getPath();
				Component c = (Component) path.getLastPathComponent();
				// Now we know what component was selected, so
				// display some information about it in the message line
				if (c.isShowing()) {
					Point p = c.getLocationOnScreen();
					msgline.setText(
							"x: " + p.x + "  y: " + p.y + "  width: " + c.getWidth() + "  height: " + c.getHeight());
				} else {
					msgline.setText("component is not showing");
				}
			}
		});

		// Now that we've set up the tree, add it to the scrollpane
		scrollpane.setViewportView(tree);

		// Finally, set the size of the main window, and pop it up.
		frame.setSize(600, 400);
		frame.setVisible(true);
	}

}
