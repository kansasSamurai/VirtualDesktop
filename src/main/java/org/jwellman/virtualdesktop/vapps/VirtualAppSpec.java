package org.jwellman.virtualdesktop.vapps;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;

/**
 *
 * @author Rick Wellman
 */
abstract public class VirtualAppSpec {

    private String title;
    private Class clazz;
    private JPanel content;
    protected int height = 0;
    protected int width = 0;
    protected boolean internalFrameProvider = false;

    protected VirtualAppSpec() {
        // Intentionally Empty
    }

    protected JPanel createDefaultContent(Component o) {
        final JPanel pnl = new JPanel(new BorderLayout());
        pnl.setPreferredSize(new Dimension(width, height));
        pnl.add(o, BorderLayout.CENTER);

        return pnl;
    }

    public void populateInternalFrame(JInternalFrame frame) {
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
