package i5.las2peer.api;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/**
 * a simple class managing a service version number in the format major.minor.patchversion-build where minor, patchversion
 * and build are optional
 *
 */
public class SemverVersion {

    private String value;

    protected Integer major = null;
    protected Integer minor = null;
    protected Integer patch = null;

    protected String build = null;
    protected String preRelease = null;

    /**
     * Generate a Version from String representation
     *
     * format : Semver {@code <major> "." <minor> "." <patch> "-" <pre-release> "+" <build>} (where minor, patch, pre-release and build are optional) or "*" (no version specified /
     * matches all versions)
     *
     * @param version A version string representation to parse
     * @throws IllegalArgumentException If the string contains no valid version representation
     */
    public SemverVersion(String version) throws IllegalArgumentException {
        this.value = version;

        if (version != null && !version.equals("*")) {
            try {

                String[] split;
                String rawVersion;
                // Separate additional metadata (pre-release/build)
                if (hasPreRelease()) {
                    String[] tokens;
                    tokens = value.split("-", 2);

                    split = tokens[1].split("\\+");
                    if (split.length > 2) {
                        throw new IllegalArgumentException("Syntax Error: more than one + in version string");
                    } else if (split.length == 2) {
                        this.preRelease = split[0];
                        this.build = split[1];
                    } else {
                        this.preRelease = split[0];
                    }

                    if (this.preRelease != null && this.preRelease.contains("-")){
                        throw new IllegalArgumentException("Syntax Error: pre-release should not contain a -.");
                    }

                    rawVersion = tokens[0];
                }
                else {
                    split = version.split("\\+");
                    if (split.length > 2) {
                        throw new IllegalArgumentException("Syntax Error: more than one + in version string");
                    }

                    if (split.length == 2) {
                        this.build = split[1];
                    }
                    rawVersion = split[0];
                }

                split = rawVersion.split("\\.");
                if (split.length > 3) {
                    throw new IllegalArgumentException(
                            "Syntax Error: too many version numbers, a maximum of three is allowed");
                }

                this.major = Integer.valueOf(split[0]);
                if (this.major < 0) {
                    throw new IllegalArgumentException("Negative version numbers are not allowed!");
                }

                if (split.length > 1) {
                    this.minor = Integer.valueOf(split[1]);
                    if (this.minor < 0) {
                        throw new IllegalArgumentException("Negative version numbers are not allowed!");
                    }
                } else {
                    this.minor = null;
                }

                if (split.length > 2) {
                    this.patch = Integer.valueOf(split[2]);
                    if (this.patch < 0) {
                        throw new IllegalArgumentException("Negative version numbers are not allowed!");
                    }
                } else {
                    this.patch = null;
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "The given string contains invalid number representations: " + version, e);
            }
        }
    }

    /**
     * generate a new ServiceVersion
     *
     * @param major Major version number part
     * @param minor Minor version number part
     * @param patch Patch version number part
     * @param preRelease  pre-release version
     * @param build build version
     * @throws IllegalArgumentException If a version number part is smaller than 0
     */
    public SemverVersion(int major, int minor, int patch, String preRelease, String build) throws IllegalArgumentException {
        this(major, minor, patch, preRelease);

        this.build = build;
    }

    /**
     * generate a new ServiceVersion
     *
     * @param major Major version number part
     * @param minor Minor version number part
     * @param patch Patch version number part
     * @param preRelease  pre-release version
     * @throws IllegalArgumentException If a version number part is smaller than 0
     */
    public SemverVersion(int major, int minor, int patch, String preRelease) throws IllegalArgumentException {
        this(major, minor, patch);

        this.preRelease = preRelease;
    }

    /**
     * generate a new ServiceVersion
     *
     * @param major Major version number part
     * @param minor Minor version number part
     * @param patch Patch version number part
     * @throws IllegalArgumentException If a version number part is smaller than 0
     */
    public SemverVersion(int major, int minor, int patch) throws IllegalArgumentException {
        this(major, minor);
        if (patch < 0) {
            throw new IllegalArgumentException("Negative version numbers are not allowed!");
        }
        this.patch = patch;
    }

    /**
     * generate a new ServiceVersion
     *
     * @param major Major version number part
     * @param minor Minor version number part
     * @throws IllegalArgumentException If a version number part is smaller than 0
     */
    public SemverVersion(int major, int minor) throws IllegalArgumentException {
        this(major);
        if (minor < 0) {
            throw new IllegalArgumentException("Negative version numbers are not allowed!");
        }
        this.minor = minor;
    }

    /**
     * generate a new ServiceVersion
     *
     * @param major Major version number part
     * @throws IllegalArgumentException If a version number part is smaller than 0
     */
    public SemverVersion(int major) throws IllegalArgumentException {
        if (major < 0) {
            throw new IllegalArgumentException("Negative version numbers are not allowed!");
        }
        this.major = major;
    }

    /**
     * Identify if this version contains a pre-release according to semver specification
     * @return if this version contains a pre-release
     */
    private boolean hasPreRelease() {

        int firstIndexOfPlus = value.indexOf("+");
        int firstIndexOfHyphen = value.indexOf("-");

        if (firstIndexOfHyphen == -1) {
            return false;
        }

        return firstIndexOfPlus == -1 || firstIndexOfHyphen < firstIndexOfPlus;
    }

    /**
     * compares to version and checks for equality
     *
     * @param v Another service version to compare to
     * @return true, if this version is the same as the given one
     */
    public boolean equals(SemverVersion v) {
        return v.toVersionString(false).equals(this.toVersionString(false));
    }

    /**
     * compares this version with any object
     *
     * if the given object is a String, the string representation of this version is compared to the given string
     *
     * @param o Another object to check
     * @return true, if the given object is a version and the same as this one
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof SemverVersion) {
            return this.equals((SemverVersion) o);
        } else if (o instanceof String) {
            return this.toString().equals(o);
        } else {
            return super.equals(o);
        }
    }

    /**
     * since equals is overridden, we should implement an own hashCode.
     *
     * @return a hash code as int
     */
    @Override
    public int hashCode() {
        return (this.toString()).hashCode();
    }

    /**
     * get the minor version number
     *
     * @return minor version number
     */
    public int getMinor() {
        if (minor == null) {
            return 0;
        }
        return minor;
    }

    /**
     * get the major version number
     *
     * @return major version number
     */
    public int getMajor() {
        if (major == null) {
            return 0;
        }
        return major;
    }


    /**
     * get the patch version of the minor version number
     *
     * @return patch version number of minor
     */
    public int getPatch() {
        if (patch == null) {
            return 0;
        }
        return patch;
    }

    /**
     * get the subversion of the minor version number
     *
     * @return subversion number of minor
     * @deprecated use {@link #getPatch()} instead
     */
    public int getSub() {
        return this.getPatch();
    }


    /**
     * get the build number of this (patch)version
     *
     * @return build number
     */
    public String getBuild() {
        if (build == null) {
            return "";
        }
        return build;
    }

    public String getPreRelease() {
        if (preRelease == null) {
            return "";
        }
        return preRelease;
    }

    /**
     * @return a String representation of this version
     */
    @Override
    public String toString() {
        return toVersionString(true);
    }

    /**
     * Returns the version without optional information
     * @param includeBuild should build information be included?
     * @return  a String representation of this version
     */
    private String toVersionString(Boolean includeBuild) {
        if (major == null) {
            return "*";
        }

        String result = "" + major;
        if (minor != null) {
            result += "." + minor;
            if (patch != null) {
                result += "." + patch;
            }
        }

        if (preRelease != null) {
            result += "-" + preRelease;
        }

        if (build != null && includeBuild) {
            result += "+" + build;
        }

        return result;
    }
}
