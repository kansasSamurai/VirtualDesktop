/*
   XionDE.fm - XionDE File Manager
   Copyright (C) 2007 XionDE TEAM (lucky@vectorstar.net)

   This program can be distributed under the terms of the GNU GPL
   See the file COPYING.
*/

// file manager package
package fx.filemanager;
// native utils class - gets special file info (uses native linux libs!)
public class NativeUtils
{
 // not constructable
 private NativeUtils() {}
 // load library
 static
 {
  String lib = fx.Main.libPath + java.io.File.separator + fx.Main.SHARED_LIB;
  try
  {
   System.load(lib); // load shared object
   initMimeDb(); // init mime database
  }
  catch (Exception ex) { fx.Main.killApp("natut", "cannot load native library (" + lib + ")"); }
 }
 // native methods
 private static native void initMimeDb(); // initialize shared mime database
 public static native int getFileUid(String file); // get file uid
 public static native String getFileOwner(String file); // get file owner
 public static native int getFileGid(String file); // get file gid
 public static native String getFileGroup(String file); // get file group
 public static native String getFileMode(String file); // get file mode
 public static native boolean setFileMode(String file, int mode); // set file mode
 public static native String getFileMimeInfo(String file); // get file mime
 public static native String getFileMimeDesc(String file); // get file mime description
 public static native boolean isSymlink(String file); // check symlink
 public static native boolean isMounted(String file); // check mounted
 public static native long lastCreated(String file); // get file create time
 public static native long lastModified(String file); // get file modify time
 public static native long lastAccessed(String file); // get file access time
 public static void execApp(String app) // execute application (dont wait for exit)
 {
  try
  {
   Process proc = Runtime.getRuntime().exec(app);
  } catch (Exception ex) {}
 }
 public static String execCmd(String[] args) // execute command
 {
  try
  {
   Process proc = Runtime.getRuntime().exec(args);
   java.io.BufferedInputStream stdin = new java.io.BufferedInputStream(proc.getInputStream());
   java.io.BufferedInputStream stderr = new java.io.BufferedInputStream(proc.getErrorStream());
   int exit = proc.waitFor();
   byte[] in = new byte[stdin.available()];
   byte[] err = new byte[stderr.available()];
   stdin.read(in);
   stderr.read(err);
   if (exit == 0) // return ok
   return null;
   else // return error message
   return new String(in) + new String(err);
  } catch (Exception ex) { return null; }
 }
 public static long getFreeSpace(String file) // get free space
 {
  return getFreeSpaceImpl(file) * 1024; // convert to bytes before return
 }
 public static long getFileSize(String file) // get file size (do not follow symlink)
 {
  return getFileSizeImpl(file) * 1024; // convert to bytes before return
 }
 private static native long getFreeSpaceImpl(String file); // get free space (in K)
 private static native long getFileSizeImpl(String file); // get file size (in K)
 public static native boolean mkSymlink(String linkTo, String linkName); // create symlink
 public static native boolean removeFile(String file); // remove file/symlink
}
