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
import java.util.Date;
import javax.swing.*;

/**
 * 
 * @author Rick Wellman
 */
public class DetailsItem extends DefaultItem {
 // components params
 private final int ICON_RES = 16; // icon resolution (width and height)
 private JTextArea[] info = { // details information columns (max. num = 16)
   new JTextArea(), new JTextArea(), new JTextArea(), new JTextArea(),
   new JTextArea(), new JTextArea(), new JTextArea(), new JTextArea(),
   new JTextArea(), new JTextArea(), new JTextArea(), new JTextArea(),
   new JTextArea(), new JTextArea(), new JTextArea(), new JTextArea()};
 // override method
 public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
 {
  // from me
  FileViewer fview = (FileViewer) list; // reconfigure data (i know - this is FileViewer)
  UnixFile ufile = (UnixFile) value; // reconfigure data (i know - this is UnixFile)
  // init item
  initItem(fview, ufile, isSelected, new Dimension(ICON_RES, ICON_RES), ICON_RES);
  // configure addititional components
  icon.setLocation(0, 0);
  subIcon.setLocation(0, icon.getHeight() - subIcon.getHeight()); // put subicon in bottom left corner
  try
  {
   int infoPosX = icon.getWidth(), limit = (fview.getDetailsOption().split("#").length / 2);
   if (limit > info.length) limit = info.length;
   for (int i = 0; i < limit; i++)
   {
    //info[i] = new JTextArea();
    info[i].setFont(getFont());
    info[i].setText(fview.getDetailsOption()
      .split("#")[i * 2]
      .replace(FileViewer.INFO_NAME,  ufile.getName())
      .replace(FileViewer.INFO_SIZE,  ufile.getSize())
      .replace(FileViewer.INFO_DATE,  String.valueOf(new Date(ufile.lastCreated())))
      .replace(FileViewer.INFO_UID,   String.valueOf(ufile.getUid()))
      .replace(FileViewer.INFO_OWNER, ufile.getOwner())
      .replace(FileViewer.INFO_GID,   String.valueOf(ufile.getGid()))
      .replace(FileViewer.INFO_GROUP, ufile.getGroup())
      .replace(FileViewer.INFO_MODE,  ufile.getMode())
      .replace(FileViewer.INFO_MIME,  ufile.getMimeInfo())
      .replace(FileViewer.INFO_DESC,  ufile.getMimeDesc()));
    info[i].setOpaque(false);
    info[i].setLocation(infoPosX, 0);
    info[i].setSize(Integer.parseInt(fview.getDetailsOption().split("#")[(i * 2) + 1]), getHeight());
    infoPosX = infoPosX + info[i].getWidth();
    if (info[i].getPreferredSize().width > info[i].getWidth()) // > full text area is visible
    {
     // show "..." in the end of info line, if info line isn't visible full =)
     info[i].setText(info[i].getText().concat("..."));
     for (int j = 0; info[i].getPreferredSize().width > info[i].getWidth(); j++)
     info[i].setText(info[i].getText().substring(0, info[i].getText().length() - 3 - j).concat("..."));
    }
   }
  } catch (Exception ex) { fx.Main.killApp("details", "incorrect details option"); }
  // add components
  icon.add(subIcon);
  add(icon);
  for (int i = 0; i < info.length; i++) add(info[i]);
  // return cell renderer
  return this;
 }
}
