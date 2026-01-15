/*
   XionDE.fm - XionDE File Manager
   Copyright (C) 2007 XionDE TEAM (lucky@vectorstar.net)

   This program can be distributed under the terms of the GNU GPL
   See the file COPYING.
*/

// file manager frames package
package fx.filemanager.frames;
// import classes
import javax.swing.*; // for swing label
import fx.filemanager.UnixFile; // unix file properties
// propreties thread class
public class PropertiesThread extends Thread
{
 // inner data
 private JLabel text = null;
 private UnixFile[] files = null;
 private long size = 0, time;
 private boolean exec = true;
 private int mode;
 public static final int MODE_SIZE = 1, MODE_CHMOD = 2; // thread's type
 // construct thread
 public PropertiesThread(JLabel label, UnixFile[] list, int type)
 {
  text = label;
  files = list;
  mode = type;
 }
 public void terminate()
 {
  exec = false;
 }
 public void run()
 {
  switch (mode)
  {
   default: // MODE_SIZE - default
    text.setText("Calculating... " + UnixFile.sizeToHuman(size) + " (" + size + ")");
    time = System.currentTimeMillis() + 1000;
    for (int i = 0; i < files.length; i++)
    {
     if (!exec) break;
     getFileSize(files[i], true);
    }
    text.setText(UnixFile.sizeToHuman(size) + " (" + size + ")");
    break;
   case MODE_CHMOD:
       
       
       
    break;
  }
 }
 // file's size recursive calculating (root (top) level?)
 private void getFileSize(UnixFile file, boolean root)
 {
  //System.out.println(file.getPath());
  if (file.isDirectory() && !file.isSymlink()) // if dir (not symlink) - recursive
  {
   try
   {
    String path = file.getCanonicalPath() + UnixFile.separator;
    String[] listX = file.list();
    UnixFile fileX;
    //UnixFile[] list = file.listFiles();
    if (listX != null) // dir not empty
    for (int i = 0; i < listX.length; i++)
    {
     if (!exec) break;
     fileX = new UnixFile(path + listX[i]);
     //getFileSize(list[i], false);
     getFileSize(fileX, false);
    }
   } catch (Exception ex) { return; } // access denied
  }
  // file size
  if (file.isSymlink() && !root)
  size = size + file.lengthSymlink();
  else
  size = size + file.length();
  if (System.currentTimeMillis() > time)
  {
   text.setText("Calculating... " + UnixFile.sizeToHuman(size) + " (" + size + ")");
   time = System.currentTimeMillis() + 1000;
  }
 }
}
