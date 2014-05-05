package org.jwellman.virtualdesktop.vapps;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JPanel;

/**
 *
 * @author Rick Wellman
 */
abstract public class VirtualAppSpec {

    private String title;
    private Class clazz;
    private JPanel content;
    protected int height = 200;
    protected int width = 300;

    protected VirtualAppSpec() {}

    protected JPanel createDefaultContent(Component o) {
        final JPanel content = new JPanel(new BorderLayout());
        content.add(o, BorderLayout.CENTER);

        return content;
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

}
