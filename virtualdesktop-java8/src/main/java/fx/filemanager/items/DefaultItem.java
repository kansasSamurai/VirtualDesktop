/*
 XionDE.fm - XionDE File Manager
 Copyright (C) 2007 XionDE TEAM (lucky@vectorstar.net)

 This program can be distributed under the terms of the GNU GPL
 See the file COPYING.
 */
package fx.filemanager.items;

import fx.filemanager.FileViewer;
import fx.filemanager.UnixFile;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.util.Date;
import javax.swing.*;

/**
 *
 * @author XionDE TEAM
 */
abstract class DefaultItem extends JPanel implements ListCellRenderer {

    // system value
    static final long serialVersionUID = 1;
    // setup components
    JLabel icon = new JLabel(), subIcon = new JLabel();
    JTextArea text = new JTextArea();
    JLabel comment1 = new JLabel(), comment2 = new JLabel();
    // init component

    void initItem(FileViewer fview, UnixFile ufile, boolean isSelected, Dimension rootSize, int iconRes) {
        // load icons
        ImageIcon img = fview.getIconTheme().getIcon(ufile.getName(), ufile.getMimeInfo()); // main icon
        ImageIcon subImg = null; // subicon - null by default (empty)
        if (ufile.isSymlink()) {
            subImg = fview.getIconTheme().getLnkIcon(); // subicon - symlink
        }
        if (ufile.isMounted()) {
            subImg = fview.getIconTheme().getMntIcon(); // subicon - mounted
        }  // configure components
        setEnabled(fview.isEnabled());
        setFont(fview.getFont());
        setLayout(null);
        setOpaque(true);
        setPreferredSize(rootSize);
        setSize(getPreferredSize());
        if (isSelected) {
            setToolTipText(fview.getToolTipOption()
                    .replace(FileViewer.INFO_NAME, ufile.getName())
                    .replace(FileViewer.INFO_SIZE, ufile.getSize())
                    .replace(FileViewer.INFO_DATE, String.valueOf(new Date(ufile.lastCreated())))
                    .replace(FileViewer.INFO_UID, String.valueOf(ufile.getUid()))
                    .replace(FileViewer.INFO_OWNER, ufile.getOwner())
                    .replace(FileViewer.INFO_GID, String.valueOf(ufile.getGid()))
                    .replace(FileViewer.INFO_GROUP, ufile.getGroup())
                    .replace(FileViewer.INFO_MODE, ufile.getMode())
                    .replace(FileViewer.INFO_MIME, ufile.getMimeInfo())
                    .replace(FileViewer.INFO_DESC, ufile.getMimeDesc()));
            setBackground(fview.getSelectionBackground());
            setForeground(fview.getSelectionForeground());
        } else {
            setToolTipText(null);
            setBackground(fview.getBackground());
            setForeground(fview.getForeground());
        }
        icon.setFont(getFont());
        icon.setLayout(null);
        icon.setSize(iconRes, iconRes);
        if (img.getIconWidth() == icon.getWidth() && img.getIconHeight() == icon.getHeight()) {
            // source icon res = res you need
            icon.setIcon(img); // set this icon
        } else {
            // ... not equal -> resize (scale) icon to your resolution  \/
            icon.setIcon(new ImageIcon(img.getImage().getScaledInstance(icon.getWidth(), icon.getHeight(), Image.SCALE_SMOOTH)));
        }
        subIcon.setFont(getFont());
        subIcon.setSize(icon.getWidth() / 2, icon.getHeight() / 2);
        if (subImg != null) // subicon is not empty
        {
            if (subImg.getIconWidth() == subIcon.getWidth()
                    && subImg.getIconHeight() == subIcon.getHeight()) // source icon res = res you need
            {
                subIcon.setIcon(subImg); // set this icon
            } else // ... not equal -> resize (scale) icon to your resolution  \/
            {
                subIcon.setIcon(new ImageIcon(subImg.getImage()
                        .getScaledInstance(subIcon.getWidth(), subIcon.getHeight(), Image.SCALE_SMOOTH)));
            }
        }
        subIcon.setVisible(ufile.isSymlink() || ufile.isMounted());

        text.setFont(getFont());
        text.setText(ufile.getName());
        comment1.setFont(getFont());
        comment1.setText(fview.getComment1Option()
                .replace(FileViewer.INFO_SIZE, ufile.getSize())
                .replace(FileViewer.INFO_DATE, String.valueOf(new Date(ufile.lastCreated())))
                .replace(FileViewer.INFO_UID, String.valueOf(ufile.getUid()))
                .replace(FileViewer.INFO_OWNER, ufile.getOwner())
                .replace(FileViewer.INFO_GID, String.valueOf(ufile.getGid()))
                .replace(FileViewer.INFO_GROUP, ufile.getGroup())
                .replace(FileViewer.INFO_MODE, ufile.getMode())
                .replace(FileViewer.INFO_MIME, ufile.getMimeInfo())
                .replace(FileViewer.INFO_DESC, ufile.getMimeDesc()));
        comment1.setVisible(true);
        comment2.setFont(getFont());
        comment2.setText(fview.getComment2Option()
                .replace(FileViewer.INFO_SIZE, ufile.getSize())
                .replace(FileViewer.INFO_DATE, String.valueOf(new Date(ufile.lastCreated())))
                .replace(FileViewer.INFO_UID, String.valueOf(ufile.getUid()))
                .replace(FileViewer.INFO_OWNER, ufile.getOwner())
                .replace(FileViewer.INFO_GID, String.valueOf(ufile.getGid()))
                .replace(FileViewer.INFO_GROUP, ufile.getGroup())
                .replace(FileViewer.INFO_MODE, ufile.getMode())
                .replace(FileViewer.INFO_MIME, ufile.getMimeInfo())
                .replace(FileViewer.INFO_DESC, ufile.getMimeDesc()));
        comment2.setVisible(true);
    }
    // override method

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        // get data
        FileViewer fview = (FileViewer) list; // reconfigure data (i know - this is FileViewer)
        UnixFile ufile = (UnixFile) value; // reconfigure data (i know - this is UnixFile)
        // init item
        initItem(fview, ufile, isSelected, new Dimension(200, 50), 48); // example
        // code logic
        //...


        // return cell renderer
        return this;
    }
    
}
