/*
   XionDE.fm - XionDE File Manager
   Copyright (C) 2007 XionDE TEAM (lucky@vectorstar.net)

   This program can be distributed under the terms of the GNU GPL
   See the file COPYING.
*/

// file manager frames package
package fx.filemanager.frames;
// import classes
import java.io.*; // to operate with files
import java.util.*; // for useful utils
import javax.swing.*; // for swing gui
import fx.filemanager.NativeUtils; // use native utils
import fx.filemanager.UnixFile; // use unix files
// progress thread class
class ProgressThread extends Thread implements Runnable
{
 // thread data
 private ProgressFrame frame; // progress frame
 private UnixFile[] list; // list files to operate
 private UnixFile dest; // destination dir
 private boolean copy, delete; // operation flags
 private int rewriteOption = 0; // rewrite option (from RewriteQuestionDialog)
 private long curSize, fullSize = 0; // size
 private byte[] bufM = new byte[1024 * 1024]; // buffer = 1M
// private byte[] bufK = new byte[1024]; // buffer = 1K
 private FileInputStream in = null; // input file
 private FileOutputStream out = null; // output file
 private Comparator nameCmp = new Comparator()
 {
  public int compare(Object o1, Object o2)
  {
   UnixFile f1 = (UnixFile) o1, f2 = (UnixFile) o2;
   int ext = 0;
   if (f1.isDirectory()) ext = ext - 100000;
   if (f2.isDirectory()) ext = ext + 100000;
   return f1.getName().compareTo(f2.getName()) + ext;
  }
 }; // sort by name comparator (sort files by name before copy)
 // construct class
 public ProgressThread(ProgressFrame owner, UnixFile[] targets,
   UnixFile destDir, boolean copyFlag, boolean deleteFlag)
 {
  frame = owner; // set progress frame - owner of the thread
  // setup targets
  list = targets; // setup targets to operate
  dest = destDir; // setup destination
  copy = copyFlag; // setup copy flag
  delete = deleteFlag; // setup delete flag
  for (int i = 0; i < list.length; i++) // full size
  fullSize = UnixFile.getFileSize(list[i]) + fullSize;
 }
 // run thread
 public void run()
 {
  // we use 2 flags (copy, delete) combination for 4 actions:
  // copy - copy files
  // delete - delete files
  // copy & delete - move files (first copy, then delete)
  // [nothing] - copy as symlink
  try
  {
   setPriority(1);
   Arrays.sort(list, nameCmp); // sort root list
   if (copy) // copy flag is set
   {
    curSize = 0; // init data
    rewriteOption = 0;
    frame.setTitle(frame.getManager().getCopyInfo().split("#")[0]);
    frame.getButton().setText(frame.getManager().getCopyInfo().split("#")[2]);
    frame.getCurrentProgress().setIndeterminate(false);
    frame.getCurrentProgress().setValue(frame.getCurrentProgress().getMinimum());
    frame.getFullProgress().setValue(frame.getFullProgress().getMinimum());
    for (int i = 0; i < list.length; i++) // create directories tree
    mktree(list[i]);
    for (int i = 0; i < list.length; i++) // copy files
    copy(list[i]);
   }
   if (delete) // delete flag is set
   {
    curSize = 0; // init data
    frame.setTitle(frame.getManager().getDeleteInfo().split("#")[0]);
    frame.getButton().setText(frame.getManager().getDeleteInfo().split("#")[2]);
    frame.getCurrentProgress().setIndeterminate(true);
    frame.getCurrentProgress().setValue(frame.getCurrentProgress().getMinimum());
    frame.getFullProgress().setValue(frame.getFullProgress().getMinimum());
    for (int i = 0; i < list.length; i++) // delete files
    delete(list[i]);
   }
   if (!copy && !delete) // paste as symlink spec flag (!copy && !delete)
   {
    curSize = 0; // init data
    frame.setTitle(frame.getManager().getCopyInfo().split("#")[0]);
    frame.getButton().setText(frame.getManager().getCopyInfo().split("#")[2]);
    frame.getCurrentProgress().setIndeterminate(true);
    frame.getCurrentProgress().setValue(frame.getCurrentProgress().getMinimum());
    frame.getFullProgress().setValue(frame.getFullProgress().getMinimum());
    for (int i = 0; i < list.length; i++) // symlink files
    link(list[i]);
   }
   frame.setTitle(" ");
   frame.getStatus().setText("..."); // set status label
   frame.getButton().setEnabled(false); // disable cancel button
   frame.getButton().setText(" ");
   frame.getCurrentProgress().setValue(frame.getCurrentProgress().getMaximum());
   frame.getFullProgress().setValue(frame.getFullProgress().getMaximum());
   kill(false); // exit, do not check
  } catch (Exception ex) { ex.printStackTrace(); fx.Main.killApp("progress", "file operation error"); }
 }
 // stop thread
 public void kill(boolean needCheck)
 {
  if (needCheck) // if check is required
  for (int i = 0; i < list.length; i++)
  check(list[i]);
  frame.setVisible(false); // close progress frame
  if (frame.getManager().getCwd().equals(frame.getCwd())) // if cwd is not changed
  frame.getManager().setCwd(frame.getCwd()); // refresh
  stop();
 }
 // show error message
 private void errorMesg(String operError, String fileName, boolean needCheck)
 {
  // read error frame options
  // format: <title>|<message>
  // example: Error|Can't copy file
  try
  {
   JOptionPane.showMessageDialog(null, operError.split("#")[1]
     + ": " + fileName, operError.split("#")[0], JOptionPane.ERROR_MESSAGE);
  }
  catch (Exception ex) { fx.Main.killApp("thread", "can't show error message"); }
  kill(needCheck);
 }
 // recursive delete files
 private void delete(UnixFile file)
 {
  frame.getStatus().setText(frame.getManager().getDeleteInfo().split("#")[1] +
    ": " + file.getName());
  if (file.isDirectory()) // delete dir
  {
   if (!file.isSymlink()) // isn't link to directory
   {
    UnixFile[] ls = file.listFiles();
    Arrays.sort(ls, nameCmp); // sort files by name before delete
    for (int i = 0; i < ls.length; i++)
    delete(ls[i]);
   }
   if (!file.delete()) // try to delete empty dir or symlink
   // exit, do not check (check is not needed when deleting)
   errorMesg(frame.getManager().getDeleteError(), file.getName(), false);
  }
  else // delete file
  {
   long fileSize = file.length();
   if (!file.delete()) // try to delete file
   // exit, do not check (check is not needed when deleting)
   errorMesg(frame.getManager().getDeleteError(), file.getName(), false);
   else // if deleted
   {
    curSize = curSize + fileSize;
    if (fullSize != 0)
    frame.getFullProgress().setValue((int)
      (curSize * frame.getFullProgress().getMaximum() / fullSize));
   }
  }
 }
 // create directories tree
 private void mktree(UnixFile file)
 {
  if (file.isDirectory()) // is dir
  {
   if (!newName(file).exists()) // new dir isn't exists
   if (!newName(file).mkdir()) // try to create dir
   // exit, do not check (we are do not check directories)
   errorMesg(frame.getManager().getCopyError(), file.getName(), false);
   UnixFile[] ls = file.listFiles();
   Arrays.sort(ls, nameCmp); // sort files by name before create dirs
   for (int i = 0; i < ls.length; i++)
   mktree(ls[i]);
  }
 }
 // recursive copy files
 private void copy(UnixFile file)
 {
  frame.getStatus().setText(frame.getManager().getCopyInfo().split("#")[1]
    + ": " + file.getName());
  if (file.isDirectory()) // is dir
  {
   UnixFile[] ls = file.listFiles();
   Arrays.sort(ls, nameCmp); // sort files by name before copy
   for (int i = 0; i < ls.length; i++)
   copy(ls[i]);
  }
  else // isn't dir
  {
   frame.getCurrentProgress().setValue(frame.getCurrentProgress().getMinimum());
   if (!newName(file).exists()) // new file isn't exists
   copyFile(file, newName(file));
   else // file is exists - ask for operation
   {
    if (rewriteOption != RewriteQuestionDialog.YES_ALL_OPTION &&
        rewriteOption != RewriteQuestionDialog.NO_ALL_OPTION)
    {
     frame.getQuestionDialog().showQuestionDialog(
       frame.getManager().getCopyQuestion().split("#")[1]
       + ": " + file.getName() + " ?");
     rewriteOption = frame.getQuestionDialog().getReturnOption();
    }
    switch (rewriteOption)
    {
     case RewriteQuestionDialog.YES_OPTION: copyFile(file, newName(file)); break;
     case RewriteQuestionDialog.YES_ALL_OPTION: copyFile(file, newName(file)); break;
     case RewriteQuestionDialog.NO_OPTION: break;
     case RewriteQuestionDialog.NO_ALL_OPTION: break;
     default: kill(false); break; // exit, do not check
    }
   }
  }
 }
 // create symlink to specified file
 private void link(UnixFile file)
 {
  frame.getStatus().setText(frame.getManager().getCopyInfo().split("#")[1]
    + ": " + file.getName());
  long fileSize = file.length();
  if (!NativeUtils.mkSymlink(file.getPath(), newName(file).getPath())) // try to create symlink
  // exit, do not check (check is not needed when creating symlinks)
  errorMesg(frame.getManager().getCopyError(), file.getName(), false);
  else // if created
  {
   curSize = curSize + fileSize;
   if (fullSize != 0)
   frame.getFullProgress().setValue((int)
     (curSize * frame.getFullProgress().getMaximum() / fullSize));
  }
 }
 // recursive check files (if files damaged, they will be deleted)
 private void check(UnixFile file)
 {
  if (file.isDirectory()) // if directory
  {
   UnixFile[] ls = file.listFiles();
   Arrays.sort(ls, nameCmp); // sort files by name before check
   for (int i = 0; i < ls.length; i++)
   check(ls[i]);
  }
  else // check files only
  if (newName(file).exists()) // file exists
  if (newName(file).length() != file.length()) // file size are different (new file may be damaged - delete new file)
  if (!newName(file).delete()) // try to delete damaged file
  // exit, do not check damaged files (we doing it now)
  errorMesg(frame.getManager().getDeleteError(), file.getName(), false);
 }
 // convert this file name to new
 private UnixFile newName(UnixFile file)
 {
  if (dest.getPath().equals(list[0].getParent())) // if same file
  return new UnixFile(dest.getPath() + File.separator + "copy "
    + file.getPath().substring(list[0].getParent().length(),
    file.getPath().length()));
  else
  return new UnixFile(dest.getPath() + File.separator
    + file.getPath().substring(list[0].getParent().length(),
    file.getPath().length()));
 }
 // copy file
 private void copyFile(UnixFile from, UnixFile to)
 {
  long size = 0;
  try // try to copy file
  {
   in = new FileInputStream(from);
   out = new FileOutputStream(to);
   while (in.available() >= bufM.length) // read 1M blocks
   {
    if (in.read(bufM) != bufM.length) // read != available
    {
     try
     {
      if (in != null) { in.close(); in = null; }
      if (out != null) { out.close(); out = null; }
     } catch (Exception ex2) { in = null; out = null; }
     // exit and check damaged files
     errorMesg(frame.getManager().getCopyError(), from.getName(), true);
    }
    out.write(bufM);
    size = size + bufM.length;
    curSize = curSize + bufM.length;
    frame.getCurrentProgress().setValue((int)
      (size * frame.getCurrentProgress().getMaximum() / from.length()));
    frame.getFullProgress().setValue((int)
      (curSize * frame.getFullProgress().getMaximum() / fullSize));
   }
   // read other bytes ( < buffer)
   byte[] customBuf = new byte[in.available()];   
   if (in.read(customBuf) != customBuf.length) // read != available
   {
    try
    {
     if (in != null) { in.close(); in = null; }
     if (out != null) { out.close(); out = null; }
    } catch (Exception e2) { in = null; out = null; }
    // exit and check damaged files
    errorMesg(frame.getManager().getCopyError(), from.getName(), true);
   }
   out.write(customBuf);   
   size = size + customBuf.length;
   curSize = curSize + customBuf.length;
   try
   {
    if (in != null) { in.close(); in = null; }
    if (out != null) { out.close(); out = null; }
   } catch (Exception e2) { in = null; out = null; }
   frame.getCurrentProgress().setValue(frame.getCurrentProgress().getMaximum());
  }
  catch (Exception ex)
  {
   try
   {
    if (in != null) { in.close(); in = null; }
    if (out != null) { out.close(); out = null; }
   } catch (Exception ex2) { in = null; out = null; }
   // exit and check damaged files
   errorMesg(frame.getManager().getCopyError(), from.getName(), true);
  }
 }
}
