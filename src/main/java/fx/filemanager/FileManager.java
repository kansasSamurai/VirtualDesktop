/*
   XionDE.fm - XionDE File Manager
   Copyright (C) 2007 XionDE TEAM (lucky@vectorstar.net)

   This program can be distributed under the terms of the GNU GPL
   See the file COPYING.
*/

// file manager package
package fx.filemanager;
// import classes
import java.awt.Image; // icon for dialogs
import java.io.File; // for file separator only ("/" in UNIX)
import javax.swing.ImageIcon; // icon for dialogs
import javax.swing.JLabel; // monitor - spectator
import javax.swing.JOptionPane; // option dialog
import fx.filemanager.frames.ProgressFrame; // use progress frame to display progress
import fx.filemanager.frames.PropertiesFrame; // use properties frame to display file infos
import fx.filemanager.frames.ApplicationsFrame; // list of applications to open with ...
// file manager class - extends file viewer
public class FileManager extends FileViewer
{
 // inner data
 private final String MOUNT_UTIL = "mount", UMOUNT_UTIL = "umount", EJECT_UTIL = "eject";
 private String // default options:
   copyInfo = "Copy#File#Cancel", copyError = "Error#Cant copy",
   copyQuestion = "Question#Rewrite file#Yes#Yes to all#No#No to all#Cancel",
   deleteInfo = "Delete#File#Cancel", deleteError = "Error#Cant delete",
   deleteQuestion = "Question#Delete file",
   mkdirError = "Error#Cant create", mkdirQuestion = "Input#New name",
   createError = "Error#Cant create", createQuestion = "Choice#New file",
   renameError = "Error#Cant rename", renameQuestion = "Input#New name",
   mountError = "Error#Cant mount", unmountError = "Error#Cant unmount",
   ejectError = "Error#Cant eject";
 private ProgressFrame prog;
 private PropertiesFrame prop = null;
 private UnixFile[] clipboard = null; // for cut, copy, paste
 private boolean cutMode; // cut mode
 private ImageIcon icon = null; // icon for dialogs
 private fx.Main main = null; // only for app db
 // construct file manager
 public FileManager(fx.Main owner, String startDir, IconTheme theme, JLabel progressLabel, JLabel sizeLabel)
 {
  super(startDir, theme, progressLabel, sizeLabel);
  main = owner;
 }
 // get/set frames informations / errors / questions
 // copy
 public String getCopyInfo() { return copyInfo; }
 public String getCopyError() { return copyError; }
 public String getCopyQuestion() { return copyQuestion; }
 public void setCopyInfo(String option) { copyInfo = option; }
 public void setCopyError(String option) { copyError = option; }
 public void setCopyQuestion(String option) { copyQuestion = option; }
 // delete
 public String getDeleteInfo() { return deleteInfo; }
 public String getDeleteError() { return deleteError; }
 public String getDeleteQuestion() { return deleteQuestion; }
 public void setDeleteInfo(String option) { deleteInfo = option; }
 public void setDeleteError(String option) { deleteError = option; }
 public void setDeleteQuestion(String option) { deleteQuestion = option; }
 // create dir
 public String getMkdirError() { return mkdirError; }
 public String getMkdirQuestion() { return mkdirQuestion; }
 public void setMkdirError(String option) { mkdirError = option; }
 public void setMkdirQuestion(String option) { mkdirQuestion = option; }
 // create file
 public String getCreateError() { return createError; }
 public String getCreateQuestion() { return createQuestion; }
 public void setCreateError(String option) { createError = option; }
 public void setCreateQuestion(String option) { createQuestion = option; }
 // rename
 public String getRenameError() { return renameError; }
 public String getRenameQuestion() { return renameQuestion; }
 public void setRenameError(String option) { renameError = option; }
 public void setRenameQuestion(String option) { renameQuestion = option; }
 // mount, unmount, eject
 public String getMountError() { return mountError; }
 public String getUnmountError() { return unmountError; }
 public String getEjectError() { return ejectError; }
 public void setMountError(String option) { mountError = option; }
 public void setUnmountError(String option) { unmountError = option; }
 public void setEjectError(String option) { ejectError = option; }
 // get set icon for dialogs and frames
 public Image getIconImage() { return icon.getImage(); }
 public void setIconImage(Image image) { icon = new ImageIcon(image); }
 // get/set properties frame options
 public String getFilePropsTitles() { return "Type:#Location:#Size:#Created:#Modified:#Accessed:#Mode:#Owner:#Group:#Other:#Owner:#Group:#Read#Write#Exec#Set UID#Set GID#Sticky"; }
 public String getFilePropsButtons() { return "Recursive#Ok#Cancel#Apply"; }
 public String getFilePropsError() { return "Error#Can't change files propeties"; }
 // select all
 public void selectAll()
 {
  addSelectionInterval(0, getModel().getSize() - 1);
 }
 // invert selection
 public void invertSelection()
 {
  for (int i = 0; i < getModel().getSize(); i++)
  if (isSelectedIndex(i))
  removeSelectionInterval(i, i);
  else addSelectionInterval(i, i);
 }
 // go one directory up
 public void goUp()
 {
  UnixFile cwd = new UnixFile(getCwd());
  if (cwd.getParent() != null) // it has parent dir
  setCwd(cwd.getParent());
 }
 // open file
 public void openFile()
 {
  if (getSelectedValue() != null) // if selected
  {
   UnixFile item = (UnixFile) getSelectedValue();
   if (item.isDirectory()) // open dir
   setCwd(item.getPath());
   else // open file
   {
    // app: 1 - ext, 2 - local mime, 3 - global mime
    // exm: 1 - .txt, 2 - text/plain, 3 - text/*
    String app1 = null, app2 = null, app3 = null, ext = null, mime = null;
    try
    {
     ext = item.getName().substring(item.getName().lastIndexOf("."), item.getName().length());
    } catch (Exception Ex) {}
    try
    {
     mime = item.getMimeInfo().split("/")[0] + "/*";
    } catch (Exception Ex) {}
    if (ext != null)
    app1 = main.conf.getProperty(fx.Main.ConfigDb.FM_ROOT_APPS + "."
      + "*" + ext + ".0");
    app2 = main.conf.getProperty(fx.Main.ConfigDb.FM_ROOT_APPS + "."
      + item.getMimeInfo() + ".0");
    app3 = main.conf.getProperty(fx.Main.ConfigDb.FM_ROOT_APPS + "."
      + mime + ".0");    
    if (app1 != null)
    app1 = app1.replace("%s", item.getPath().replace(" ", "%20"));
    if (app2 != null)
    app2 = app2.replace("%s", item.getPath().replace(" ", "%20"));
    if (app3 != null)
    app3 = app3.replace("%s", item.getPath().replace(" ", "%20"));
    if (app1 != null) NativeUtils.execApp(app1);
    else
    if (app2 != null) NativeUtils.execApp(app2);
    else
    if (app3 != null) NativeUtils.execApp(app3);
    else    
    openFileWith();
   }
  }
 }
 public void openFileWith()
 {
  if (getSelectedValue() != null) // if selected
  {
   UnixFile item = (UnixFile) getSelectedValue();
   ApplicationsFrame af = new ApplicationsFrame(main, this, item);
   af.showApps();
  }
 }
 // show file properties
 public void showProperties()
 {
  if (prop == null) prop = new PropertiesFrame(this);
  if (getSelectedValue() != null) // if selected
  {
   Object[] data = getSelectedValues();
   UnixFile[] items = new UnixFile[data.length];
   for (int i = 0; i < items.length; i++)
   items[i] = (UnixFile) data[i];
   prop.showFileProps(items);
  }
 }
 // create dir
 public void makeDir()
 {
  String newName = JOptionPane.showInputDialog(null,
    getMkdirQuestion().split("#")[1], getMkdirQuestion().split("#")[0],
    JOptionPane.QUESTION_MESSAGE);
  if (newName != null) // entered new name
  if (!newName.equals(""))
  {
   UnixFile newFile = new UnixFile(getCwd() + File.separator + newName);  
   if (!newFile.mkdir()) // if can't create dir
   JOptionPane.showMessageDialog(null,
     getMkdirError().split("#")[1] + ": " + newFile.getName(),
     getMkdirError().split("#")[0], JOptionPane.ERROR_MESSAGE);
   else setCwd(getCwd()); // finished - refresh
  }
 }
 // rename file
 public void renameFile()
 {
  if (getSelectedValue() != null) // if selected
  {
   UnixFile item = (UnixFile) getSelectedValue();
   String newName = JOptionPane.showInputDialog(null,
     getRenameQuestion().split("#")[1], item.getName()); // title option unavailable in rename dialog
   if (!item.getName().equals(newName) && newName != null) // entered new name
   {
    UnixFile newFile = new UnixFile(getCwd() + File.separator + newName);
    if (!item.renameTo(newFile)) // if can't rename
    JOptionPane.showMessageDialog(null,
      getRenameError().split("#")[1] + ": " + item.getName(),
      getRenameError().split("#")[0], JOptionPane.ERROR_MESSAGE);
    else setCwd(getCwd()); // finished - refresh
   }
  }
 }
 // delete file/dir
 public void deleteFile()
 {
  if (getSelectedValue() != null) // if selected
  {
   Object[] data = getSelectedValues();
   UnixFile[] items = new UnixFile[data.length];
   for (int i = 0; i < items.length; i++)
   items[i] = (UnixFile) data[i];
   if (JOptionPane.showConfirmDialog(null,
         getDeleteQuestion().split("#")[1] + ": " + items[0].getName() + " ... ?",
         getDeleteQuestion().split("#")[0], JOptionPane.YES_NO_OPTION,
         JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
   fileOperation(items, null, false, true); // delete operation
  }
 }
 // cut
 public void cut()
 {
  if (getSelectedValue() != null) // if selected
  {
   Object[] data = getSelectedValues();
   clipboard = new UnixFile[data.length];
   for (int i = 0; i < clipboard.length; i++)
   clipboard[i] = (UnixFile) data[i];
   cutMode = true;
  }
 }
 // copy
 public void copy()
 {
  if (getSelectedValue() != null) // if selected
  {
   Object[] data = getSelectedValues();
   clipboard = new UnixFile[data.length];
   for (int i = 0; i < clipboard.length; i++)
   clipboard[i] = (UnixFile) data[i];
   cutMode = false;
  }
 }
 // paste
 public void paste()
 {
  if (clipboard != null) // if clipboard is not empty
  {
   if (cutMode)
   {
    fileOperation(clipboard, new UnixFile(getCwd()), true, true); // cut operation
    clipboard = null;
   }
   else fileOperation(clipboard, new UnixFile(getCwd()), true, false); // copy operation
  }
 }
 // paste as symlink
 public void pasteLnk()
 {
  if (clipboard != null) // if clipboard is not empty
  fileOperation(clipboard, new UnixFile(getCwd()), false, false); // paste as symlink operation
 }
 // file operation (copy, move, delete, make symlinks)
 public void fileOperation(UnixFile[] targets, UnixFile destDir, boolean copy, boolean delete)
 {
  prog = new ProgressFrame(this);
  prog.startProcess(targets, destDir, copy, delete);
 }
 // mount filesystem by mountpoint
 public void mountDir() { execExtCommand(MOUNT_UTIL, getMountError()); }
 // unmount filesystem by mountpoint
 public void unmountDir() { execExtCommand(UMOUNT_UTIL, getUnmountError()); }
 // eject device by mountpoint
 public void ejectDir() { execExtCommand(EJECT_UTIL, getEjectError()); }
 // execute special external command by action (need to mount, unmount, eject, ...)
 private void execExtCommand(String command, String operError)
 {
  try
  {
   if (getSelectedValue() != null) // if selected
   {
    UnixFile item = (UnixFile) getSelectedValue();
    String[] cmd = { command, item.getPath() };
    String msg = NativeUtils.execCmd(cmd);
    if (msg != null)
    JOptionPane.showMessageDialog(null, msg,
      "Error", JOptionPane.ERROR_MESSAGE);
//    JOptionPane.showMessageDialog(null, msg +
//      operError.split("#")[1] + ": " + item.getName(),
//      operError.split("#")[0], JOptionPane.ERROR_MESSAGE);
    else setCwd(getCwd()); // finished - refresh
   }
  } catch (Exception ex) { fx.Main.killApp("manager", "error while executing " + command); }
 }
}
