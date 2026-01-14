/*
   XionDE.fm - XionDE File Manager
   Copyright (C) 2007 XionDE TEAM (lucky@vectorstar.net)

   This program can be distributed under the terms of the GNU GPL
   See the file COPYING.
*/

// file manager items package
package fx.filemanager.items;
// import what i need
import java.awt.Component; // for overriding method
import java.awt.Dimension; // for component location
import java.util.*;
import javax.swing.*; // for swing gui
import fx.filemanager.UnixFile; // to display info
import fx.filemanager.FileViewer; // to communicate (we are the ListCellRenderer for this extended JList)
// tile item class
public class TileItem extends DefaultItem
{
 // components params
 private final Dimension ROOT_SIZE = new Dimension(220, 60);
 private final int ICON_LEFT = 5; // number of pixels from root's left border to icon's left border
 private final int ICON_RES = 48; // icon resolution (width and height of icon)
 private final int TEXT_LEFT = 5; // number of pixels from icon's right border to text's left border
 private final int TEXT_WIDTH = 155; // width of the text
 // override method
 public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
 {
  // get data
  FileViewer fview = (FileViewer) list; // reconfigure data (i know - this is FileViewer)
  UnixFile ufile = (UnixFile) value; // reconfigure data (i know - this is UnixFile)
  // init item
  initItem(fview, ufile, isSelected, ROOT_SIZE, ICON_RES);
  // configure addititional components
  icon.setLocation(ICON_LEFT, (getHeight() / 2) - (icon.getHeight() / 2));
  subIcon.setLocation(0, icon.getHeight() - subIcon.getHeight());
  text.setLayout(null);
  text.setLineWrap(true);
  text.setOpaque(false);
  text.setWrapStyleWord(true);
  text.setSize(TEXT_WIDTH, comment1.getPreferredSize().height * 3);
  text.setLocation(icon.getY() + icon.getWidth() + TEXT_LEFT, (getHeight() / 2) - (text.getHeight() / 2));
  comment1.setBounds(0, text.getHeight() / 3, text.getWidth(), text.getHeight() / 3);
  comment2.setBounds(0, (text.getHeight() / 3) * 2, text.getWidth(), text.getHeight() / 3);
  if (text.getPreferredSize().height > text.getHeight() / 3) // > 1/3 of text area is visible
  comment1.setVisible(false); // hide comment 1
  if (text.getPreferredSize().height > (text.getHeight() / 3) * 2) // > 2/3 of text area is visible
  comment2.setVisible(false); // hide comment 2
  if (text.getPreferredSize().height > text.getHeight()) // > 3/3 of text area is visible ( > text area size)
  {
   // show "..." in end of name, if file name isn't visible full =)
   text.setText(text.getText().concat("..."));
   for (int i = 0; text.getPreferredSize().height > text.getHeight(); i++)
   text.setText(text.getText().substring(0, text.getText().length() - 3 - i).concat("..."));
  }
  // add components
  text.add(comment1);
  text.add(comment2);
  icon.add(subIcon);
  add(icon);
  add(text);
  // return cell renderer
  return this;
 }
}
