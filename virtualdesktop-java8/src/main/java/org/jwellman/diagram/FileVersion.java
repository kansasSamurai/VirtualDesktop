package org.jwellman.diagram;

/**
 * Structured semantic version for the .dgx file format.
 *
 * major : incremented on breaking schema changes (old loaders cannot read the file)
 * minor : incremented on significant additive changes (new fields, new sections)
 * patch : incremented on safe, additive tweaks (new optional visual properties)
 *
 * Files without a version block (pre-versioning .json files) are treated as 0.0.0
 * and loaded leniently.
 */
public class FileVersion {

    public static final int CURRENT_MAJOR = 0;
    public static final int CURRENT_MINOR = 1;
    public static final int CURRENT_PATCH = 1;

    private int major;
    private int minor;
    private int patch;

    public FileVersion() {}

    public FileVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static FileVersion current() {
        return new FileVersion(CURRENT_MAJOR, CURRENT_MINOR, CURRENT_PATCH);
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public int getPatch() {
        return patch;
    }

    public void setPatch(int patch) {
        this.patch = patch;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
