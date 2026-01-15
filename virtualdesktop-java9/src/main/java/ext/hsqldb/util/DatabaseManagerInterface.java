package ext.hsqldb.util;

/**
 * An interface that defines callbacks used by FontDialogSwing, etc.
 *
 * @author rwellman
 */
interface DatabaseManagerInterface {

    public void setFont(String fontname);

    public void setFontSize(String fontsize);

    public void setStyle(int style);

    public void setBackground();

    public void setForeground();

}
