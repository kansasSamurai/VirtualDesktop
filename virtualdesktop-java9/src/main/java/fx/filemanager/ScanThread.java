/*
   XionDE.fm - XionDE File Manager
   Copyright (C) 2007 XionDE TEAM (lucky@vectorstar.net)

   This program can be distributed under the terms of the GNU GPL
   See the file COPYING.
*/

// file manager package
package fx.filemanager;
// import classes
import java.beans.*; // extended events
import java.io.File; // to list files in dir
import java.util.*; // for useful utils
import javax.swing.*; // use swing gui
// directory scan thread class
class ScanThread extends Thread implements Runnable
{
 // thread data
 private FileViewer fview; // file viewer
 private JLabel prog, size; // progress monitor, size field
 private PropertyChangeListener pcl = new PropertyChangeListener()
 {
  public void propertyChange(PropertyChangeEvent ev)
  {
   fview.removePropertyChangeListener(pcl);
   prog.setText(fview.getIdleOption() + " " + list.length); // set idle mode
   long sizeSum = 0;
   for (int i = 0; i < list.length; i++)
   sizeSum = sizeSum + list[i].length();
   size.setText(fview.getSizeOption() + " " + UnixFile.sizeToHuman(sizeSum)); // set files size
   fview.setEnabled(true); // enable list
   fview.grabFocus(); // grab focus
  }
 }; // list data listener
 private Comparator nameCmp = new Comparator()
 {
  public int compare(Object o1, Object o2)
  {
   File f1 = (File) o1, f2 = (File) o2;
   int ext = 0;
   if (f1.isDirectory()) ext = ext - 100000;
   if (f2.isDirectory()) ext = ext + 100000;
   return f1.getName().compareTo(f2.getName()) + ext;
  }
 }; // sort by name comparator
 private Comparator sizeCmp = new Comparator()
 {
  public int compare(Object o1, Object o2)
  {
   File f1 = (File) o1, f2 = (File) o2;
   Long l1 = new Long(f1.length()), l2 = new Long(f2.length());
   int ext = 0;
   if (f1.isDirectory()) ext = ext - 100000;
   if (f2.isDirectory()) ext = ext + 100000;
   return l1.compareTo(l2) + ext;
  }
 }; // sort by size comparator
 private Comparator typeCmp = new Comparator()
 {
  public int compare(Object o1, Object o2)
  {
   File f1 = (File) o1, f2 = (File) o2;
   int ext = 0;
   if (f1.isDirectory()) ext = ext - 100000;
   if (f2.isDirectory()) ext = ext + 100000;
   return NativeUtils.getFileMimeInfo(f1.getPath())
     .compareTo(NativeUtils.getFileMimeInfo(f2.getPath())) + ext;
  }
 }; // sort by type comparator
 private Comparator dateCmp = new Comparator()
 {
  public int compare(Object o1, Object o2)
  {
   File f1 = (File) o1, f2 = (File) o2;
   Date d1 = new Date(f1.lastModified()), d2 = new Date(f2.lastModified());
   int ext = 0;
   if (f1.isDirectory()) ext = ext - 100000;
   if (f2.isDirectory()) ext = ext + 100000;
   return d1.compareTo(d2) + ext;
  }
 }; // sort by date comparator
 // inner data
 private UnixFile[] list; // file list
 private Vector listData; // file list data
 // construct class
 public ScanThread(FileViewer owner, JLabel progressLabel, JLabel sizeLabel)
 {
  fview = owner; // set file viewer - owner of thread
  prog = progressLabel; // set progress monitor
  size = sizeLabel; // size field
 }
 // run thread
 public void run()
 {
  try
  {
   setPriority(1);
   fview.setEnabled(false); // disable list
   fview.setSelectedIndex(0);
   fview.clearSelection(); // clear list selection
   fview.setListData(new Vector()); // clear list data
   UnixFile fs = new UnixFile(fview.getCwd());
   list = fs.listFiles();
   switch (fview.getSortType())
   {
    default: Arrays.sort(list, nameCmp); break; // sort by name is default (SORT_NAME)
    case FileViewer.SORT_SIZE: Arrays.sort(list, sizeCmp); break; // size
    case FileViewer.SORT_TYPE:
      Arrays.sort(list, nameCmp); Arrays.sort(list, typeCmp); break; // type (name & type)
    case FileViewer.SORT_DATE: Arrays.sort(list, dateCmp); break; // date
   }
   listData = new Vector();  
   for (int i = 0; i < list.length; i++)
   {
    // send progress in human-readable format (from 1 to ...)
    prog.setText(fview.getLoadingOption() + " " + (i + 1) + "/" + list.length);
    listData.addElement(list[i]); // set info
   }
   prog.setText(fview.getLoadingOption() + " ...");
   fview.addPropertyChangeListener(pcl);
   fview.setListData(listData);
  } catch (Exception ex) { fx.Main.killApp("scan", "directory scanning error"); }
 }
 // stop thread
 public void kill()
 {
  fview.removePropertyChangeListener(pcl);
  stop();
 }
}
