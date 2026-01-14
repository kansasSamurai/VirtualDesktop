/*
 XionDE.fm - XionDE File Manager
 Copyright (C) 2007 XionDE TEAM (lucky@vectorstar.net)

 This program can be distributed under the terms of the GNU GPL
 See the file COPYING.
 */
// file manager package
package fx.filemanager;
// import classes
import java.io.*; // info about filesystem
import java.net.URI; // for new constructor
import java.math.BigDecimal; // for calculating pow
// unix file class (store detailed file's info with unix special info)
public class UnixFile extends File {
    // unix info fields

    private String owner, group, mode, mime, desc, size; // file owner, group, mode (example: drwxr-x---), mime info and description, human-readable size
    private int uid, gid; // file owner (uid) and group (gid)
    private boolean symlink, mounted; // get status (symlink, mounted)
    private boolean infoLoaded = false; // info is not loaded
    // construct unix file

    public UnixFile(File file) {
        super(file.getPath());
        if (file.exists()) {
            loadInfo();
        }
    }

    public UnixFile(String path) {
        this(new File(path));
        File file = new File(path);
        if (file.exists()) {
            loadInfo();
        }
    }

    public UnixFile(URI uri) {
        this(new File(uri));
        File file = new File(uri);
        if (file.exists()) {
            loadInfo();
        }
    }

    public UnixFile(File parent, String child) {
        this(new File(parent, child));
        File file = new File(parent, child);
        if (file.exists()) {
            loadInfo();
        }
    }

    public UnixFile(String parent, String child) {
        this(new File(parent, child));
        File file = new File(parent, child);
        if (file.exists()) {
            loadInfo();
        }
    }
    // types
    private static String[] FILE_SIZE_TYPE = {"b", "K", "M", "G", "T", "P", "E", "Z", "Y"};
    // get info

    private void loadInfo() {
        if (!infoLoaded) {
            int version = 2;
            switch (version) {
                case 1:
                    // load special unix info
                    uid = NativeUtils.getFileUid(getPath());
                    owner = NativeUtils.getFileOwner(getPath());
                    if (owner.equals("/-1")) {
                        owner = String.valueOf(uid); // error #0 (Success) - entry not found
                    }
                    gid = NativeUtils.getFileGid(getPath());
                    group = NativeUtils.getFileGroup(getPath());
                    if (group.equals("/-1")) {
                        group = String.valueOf(gid); // error #0 (Success) - entry not found
                    }
                    mode = NativeUtils.getFileMode(getPath());
                    mime = NativeUtils.getFileMimeInfo(getPath());
                    desc = NativeUtils.getFileMimeDesc(getPath());
                    symlink = NativeUtils.isSymlink(getPath());
                    mounted = NativeUtils.isMounted(getPath());
                    size = UnixFile.sizeToHuman(length());
                    break;
                case 2:
                    uid = 1; //NativeUtils.getFileUid(getPath());
                    gid = 1; //NativeUtils.getFileGid(getPath());
                    owner = "me"; //NativeUtils.getFileOwner(getPath());
                    if (owner.equals("/-1")) {
                        owner = String.valueOf(uid); // error #0 (Success) - entry not found
                    }
                    group = "agroup"; //NativeUtils.getFileGroup(getPath());
                    if (group.equals("/-1")) {
                        group = String.valueOf(gid); // error #0 (Success) - entry not found
                    }
                    mode = "rwxrwxrwx"; //NativeUtils.getFileMode(getPath());
                    mime = "removable/phone"; //NativeUtils.getFileMimeInfo(getPath());
                    desc = ""; //NativeUtils.getFileMimeDesc(getPath());
                    symlink = false; //NativeUtils.isSymlink(getPath());
                    mounted = true; //NativeUtils.isMounted(getPath());
                    size = "100"; //UnixFile.sizeToHuman(length());
                    break;
            }

            infoLoaded = true; // info is loaded
        }
    }
    // convert full size (in bytes) to human-readable

    public static String sizeToHuman(long prefSize) {
        double len = prefSize;
        String reg;
        int i;
        for (i = 0; len > 1023; i++) {
            len = len / 1024;
        }
        if (i >= FILE_SIZE_TYPE.length) {
            reg = "*10^" + (i * 3);
        } else {
            reg = FILE_SIZE_TYPE[i];
        }
        return String.format("%1$.2f" + reg, len);
    }
    // convert string file mode (drwxr-x---) to integer (750)

    public static int modeToInt(String mode) {
        int modeInt = 0;
        switch (mode.getBytes()[0]) {
            default:
                modeInt = modeInt | 0100000;
                break; // '-'
            case 'd':
                modeInt = modeInt | 040000;
                break;
            case 'b':
                modeInt = modeInt | 060000;
                break;
            case 'c':
                modeInt = modeInt | 020000;
                break;
            case 'p':
                modeInt = modeInt | 010000;
                break;
            case 's':
                modeInt = modeInt | 0140000;
                break;
        }
        for (int y = 0; y < 2; y++) {
            for (int x = 0, f = y + 1, mask = 0400; x < 3; x++, f = f + 3, mask = mask / 8) {
                if (mode.getBytes()[f] != '-') {
                    modeInt = modeInt | (mask / (y + 1)); // y ~ read/write
                }
            }
        }
        BigDecimal two = new BigDecimal(2);
        for (int x = 0, mask = 0100; x < 3; x++, mask = mask / 8) {
            if (mode.getBytes()[(x + 1) * 3] == 'x' | mode.getBytes()[(x + 1) * 3] == 's' | mode.getBytes()[(x + 1) * 3] == 't') {
                modeInt = modeInt | mask; // set exec
            }
            if (mode.getBytes()[(x + 1) * 3] != '-' && mode.getBytes()[(x + 1) * 3] != 'x') {
                modeInt = modeInt | (04000 / two.pow(x).intValue()); // set special (suid, sgid, sticky)
            }
        }
        return modeInt;
    }
    // convert integer file mode (750) to string (drwxr-x---)

    public static String modeToString(int mode) {
        byte[] modeString = {'-', '-', '-', '-', '-', '-', '-', '-', '-', '-'};
        if ((mode & 040000) != 0) {
            modeString[0] = 'd';
        }
        if ((mode & 060000) != 0) {
            modeString[0] = 'b';
        }
        if ((mode & 020000) != 0) {
            modeString[0] = 'c';
        }
        if ((mode & 010000) != 0) {
            modeString[0] = 'p';
        }
        if ((mode & 0140000) != 0) {
            modeString[0] = 's';
        }
        BigDecimal two = new BigDecimal(2);
        for (int y = 0; y < 3; y++) {
            for (int x = 0, f = y + 1, mask = 0400; x < 3; x++, f = f + 3, mask = mask / 8) {
                if ((mode & (mask / two.pow(y).intValue())) != 0) {
                    switch (y) {
                        default:
                            modeString[f] = 'r';
                            break; // 0
                        case 1:
                            modeString[f] = 'w';
                            break;
                        case 2:
                            modeString[f] = 'x';
                            break;
                    }
                }
            }
        }
        for (int x = 3, mask = 04000; x <= 9; x = x + 3, mask = mask / 2) {
            if ((mode & mask) != 0) // set special
            {
                if (modeString[x] == '-') {
                    if (x != 9) {
                        modeString[x] = 'S';
                    } else {
                        modeString[x] = 'T';
                    }
                } else if (x != 9) {
                    modeString[x] = 's';
                } else {
                    modeString[x] = 't';
                }
            }
        }
        return new String(modeString);
    }
    // recursive calculation of file/dir size

    public static long getFileSize(UnixFile file) {
        if (!file.isDirectory()) // if not dir
        {
            return file.length();
        } else // if dir - recursive calculate files size
        {
            try {
                long size = 0;
                UnixFile[] list = file.listFiles();
                if (list == null) {
                    return 0; // dir empty
                }
                for (int i = 0; i < list.length; i++) {
                    size = size + getFileSize(list[i]);
                }
                return size;
            } catch (Exception ex) {
                return 0;
            } // access denied
        }
    }
    // convert files array to unix files array

    public static UnixFile[] fileToUnix(File[] files) {
        if (files == null) {
            return null;
        }
        UnixFile ufiles[] = new UnixFile[files.length];
        for (int i = 0; i < ufiles.length; i++) {
            ufiles[i] = new UnixFile(files[i]);
        }
        return ufiles;
    }
    // convert to string method for compatibility with drag files feature

    public String toString() {
        return "file://" + super.toString().replace(" ", "%20");
    }

    public String getSize() {
        return size;
    } // get human-readable size

    public int getUid() {
        loadInfo();
        return uid;
    } // get owner (uid)

    public String getOwner() {
        loadInfo();
        return owner;
    } // get owner

    public int getGid() {
        loadInfo();
        return gid;
    } // get group (gid)

    public String getGroup() {
        loadInfo();
        return group;
    } // get group

    public String getMode() {
        loadInfo();
        return mode;
    } // get mode

    public String getMimeInfo() {
        loadInfo();
        return mime;
    } // get mime info

    public String getMimeDesc() {
        loadInfo();
        return desc;
    } // get mime description

    public boolean isSymlink() {
        loadInfo();
        return symlink;
    } // get symlink status

    public boolean isMounted() {
        loadInfo();
        return mounted;
    } // get mount status
    // new delete method (without symlink following)

    public boolean delete() {
        if (isSymlink()) {
            return NativeUtils.removeFile(getPath()); // use native utils to delete symlink
        } else {
            return super.delete(); // use default delete method
        }
    }
    // new length method (without symlink following)

    public long lengthSymlink() {
        if (isSymlink()) {
            return NativeUtils.getFileSize(getPath()); // use native utils to get file size
        } else {
            return super.length(); // use default method
        }
    }
    // create symlink with name specified by this abstract pathname linked to specified file

    public boolean mkLink(UnixFile file) {
        return NativeUtils.mkSymlink(file.getPath(), getPath());
    }
    // get file time

    public long lastCreated() {
        return 100; //NativeUtils.lastCreated(this.getPath()) * 1000;
    }

    public long lastModified() {
        return 100; //NativeUtils.lastModified(this.getPath()) * 1000;
    }

    public long lastAccessed() {
        return 100; //NativeUtils.lastAccessed(this.getPath()) * 1000;
    }

    public UnixFile[] listFiles() {
        return UnixFile.fileToUnix(super.listFiles());
    }

    public UnixFile[] listFiles(FileFilter filter) {
        return UnixFile.fileToUnix(super.listFiles(filter));
    }

    public UnixFile[] listFiles(FilenameFilter filter) {
        return UnixFile.fileToUnix(super.listFiles(filter));
    }

    public UnixFile getAbsoluteFile() {
        return new UnixFile(super.getAbsoluteFile());
    }

    public UnixFile getCanonicalFile() throws IOException {
        return new UnixFile(super.getCanonicalFile());
    }

    public UnixFile getParentFile() {
        return new UnixFile(super.getParentFile());
    }
    
}
