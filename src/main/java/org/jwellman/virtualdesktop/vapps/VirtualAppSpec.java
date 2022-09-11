package org.jwellman.virtualdesktop.vapps;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Icon;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;

/**
 *
 * @author Rick Wellman
 */
@SuppressWarnings("rawtypes")
abstract public class VirtualAppSpec {

    private Icon icon;
    private String title;
	private Class clazz;
    private JPanel content;
    protected int height = 0;
    protected int width = 0;
    protected boolean internalFrameProvider = false;

    protected VirtualAppSpec() {
        // Intentionally Empty
    }

    /**
     * Utility/convenience method to wrap an
     * existing content/component/JComponent with
     * a JPanel for insertion into a JInternalFrame.
     * 
     * This method should not be called until setWidth/Height()
     * have been called (otherwise the defaults are zero). 
     *  
     * @param o
     * @return
     */
    protected JPanel createDefaultContent(Component o) {
        final JPanel pnl = new JPanel(new BorderLayout());
        // Only do this when non-zero dimensions are set; this should be the exception
        // since most uses cases would normally just use pack() as the framework initializes
        // the internal app window.
        if ( width * height > 0) {
            pnl.setPreferredSize(new Dimension(width, height));            
        }
        pnl.add(o, BorderLayout.CENTER);

        return pnl;
    }

    /**
     * If a virtual app is marked as an "internal frame provider",
     * then this callback will be used to populate the contents of the JInternalFrame.
     * Note that very few virtual apps will probably use this and its use
     * is discouraged, but it is provided for the one-off's since not all
     * use cases can be envisioned nor can legacy apps be immediately refactored.
     * 
     * Note:  Users of this callback are responsible for all JInternalFrame
     * display mechanisms such as setSize(), pack(), and setVisible().
     * 
     * @param frame
     */
    public void populateInternalFrame(JInternalFrame frame, JDesktopPane desktop) {
        // empty default impl
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return the clazz
     */
    public Class getClazz() {
        return clazz;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @param clazz the clazz to set
     */
    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    /**
     * @return the content
     */
    public JPanel getContent() {
        return content;
    }

    /**
     * @param content the content to set
     */
    public void setContent(JPanel content) {
        this.content = content;
    }

    /**
     * @return the icon
     */
    public Icon getIcon() {
        return icon;
    }

    /**
     * @param icon the icon to set
     */
    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public boolean isInternalFrameProvider() {
        return this.internalFrameProvider;
    }

}
