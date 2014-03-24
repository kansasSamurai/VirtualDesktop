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
import javax.swing.*;

/**
 * 
 * @author XionDE TEAM
 */
public class IconItem extends DefaultItem {

    private final Dimension ROOT_SIZE = new Dimension(75, 75); // root size
    private final int ICON_TOP = 5; // number of pixels from root's top border to icon's top border
    private final int ICON_RES = 32; // icon resolution (width and height)
    private final int TEXT_TOP = 5; // number of pixels from icon's bottom border to text's top border
    private final int TEXT_WIDTH = 70; // width of text
    private JTextArea 
            temp1 = new JTextArea(), 
            temp2 = new JTextArea(), // text areas for calculating text width
            line1 = new JTextArea(), 
            line2 = new JTextArea(); // file name lines (max. num = 2 lines)

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        // from me
        FileViewer fview = (FileViewer) list; // reconfigure data (i know - this is FileViewer)
        UnixFile ufile = (UnixFile) value; // reconfigure data (i know - this is UnixFile)
        // init item
        initItem(fview, ufile, isSelected, ROOT_SIZE, ICON_RES);
        // configure addititional components
        icon.setLocation((getWidth() / 2) - (icon.getWidth() / 2), ICON_TOP);
        subIcon.setLocation(0, icon.getHeight() - subIcon.getHeight()); // put subicon in bottom left corner
        temp1.setFont(getFont());
        temp1.setText(text.getText());
        temp1.setSize(0, comment1.getPreferredSize().height); // single line
        temp2.setFont(getFont());
        temp2.setLineWrap(true);
        temp2.setText(text.getText());
        temp2.setSize(TEXT_WIDTH, comment1.getPreferredSize().height * 2); // 2 lines
        line1.setFont(getFont());
        line1.setOpaque(false);
        line1.setVisible(false);
        line2.setFont(getFont());
        line2.setOpaque(false);
        line2.setVisible(false);
        if (temp2.getPreferredSize().height <= (comment1.getPreferredSize().height * 1)) {
            // single line, else 2 or more lines
            line1.setText(temp1.getText());
            line1.setSize(line1.getPreferredSize().width, comment1.getPreferredSize().height);
            line1.setVisible(true);
        } else if (temp2.getPreferredSize().height <= (comment1.getPreferredSize().height * 2)) {
            // 2 lines
            for (int i = 0; temp2.getPreferredSize().height > comment1.getPreferredSize().height; i++) {
                final String t2 = temp2.getText();
                int len = t2.length();
                len = len - i; if (len < 0) break;
                temp2.setText(t2.substring(0, len));
            }
            temp1.setText(temp1.getText().substring(temp2.getText().length(), temp1.getText().length()));
            line1.setText(temp2.getText());
            line1.setSize(line1.getPreferredSize().width, comment1.getPreferredSize().height);
            line1.setVisible(true);
            line2.setText(temp1.getText());
            line2.setSize(line2.getPreferredSize().width, comment1.getPreferredSize().height);
            line2.setVisible(true);
        } else {
            // > 2 lines
            int j = 0; // just for debugging
            for (int i = 0; temp2.getPreferredSize().height > comment1.getPreferredSize().height; i++) {
                final String t2 = temp2.getText();
                int len = t2.length();
                len = len - i; if (len < 0) break;
                temp2.setText(t2.substring(0, len));
                final int t2h = temp2.getPreferredSize().height;
                final int c1h = comment1.getPreferredSize().height;
                j++;
            }
            
            // show "..." in end of the file name, if file name isn't visible full =)
            int start = temp2.getText().length();
            int end = temp1.getText().length();
            temp1.setText(temp1.getText().substring(start, end));
            temp1.setText(temp1.getText().concat("..."));
            for (int i = 0; temp1.getPreferredSize().width > TEXT_WIDTH; i++) {
                temp1.setText(temp1.getText().substring(0, temp1.getText().length() - 3 - i).concat("..."));
            }
            line1.setText(temp2.getText());
            line1.setSize(line1.getPreferredSize().width, comment1.getPreferredSize().height);
            line1.setVisible(true);
            line2.setText(temp1.getText());
            line2.setSize(line2.getPreferredSize().width, comment1.getPreferredSize().height);
            line2.setVisible(true);
        }
        line1.setLocation((getWidth() / 2) - (line1.getWidth() / 2), icon.getY() + icon.getHeight() + TEXT_TOP);
        line2.setLocation((getWidth() / 2) - (line2.getWidth() / 2), line1.getY() + line1.getHeight() + 0);
        
        // add components
        icon.add(subIcon); add(icon); add(line1); add(line2);
        
        // return cell renderer
        return this;
    }
}
