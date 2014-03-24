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
import java.awt.FlowLayout; // for text flowing after icon
import javax.swing.*; // for swing gui
import fx.filemanager.FileViewer; // to communicate
import fx.filemanager.UnixFile; // to show info
// list item class
public class ListItem extends DefaultItem
{
 // components params
 private final int ROOT_WIDTH = 150; // root width
 private final int ICON_RES = 16; // icon resolution (width and height)
 // override method
 public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
 {
  // from me
  FileViewer fview = (FileViewer) list; // reconfigure data (i know - this is FileViewer)
  UnixFile ufile = (UnixFile) value; // reconfigure data (i know - this is UnixFile)
  // init item
  initItem(fview, ufile, isSelected, new Dimension(ROOT_WIDTH, ICON_RES), ICON_RES);
  // configure addititional components
  setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
  subIcon.setLocation(0, icon.getHeight() - subIcon.getHeight()); // put subicon in bottom left corner
  text.setOpaque(false);
  text.setSize(getWidth() - icon.getWidth(), getHeight());
  if (text.getPreferredSize().width > text.getWidth()) // > full text area is visible
  {
   // show "..." in the end of file name, if file name isn't visible full =)
   text.setText(text.getText().concat("..."));
   for (int i = 0; text.getPreferredSize().width > text.getWidth(); i++)
   text.setText(text.getText().substring(0, text.getText().length() - 3 - i).concat("..."));
  }
  // add components
  icon.add(subIcon);
  add(icon);
  add(text);
  // return cell renderer
  return this;
 }
}
