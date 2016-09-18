package i5.las2peer.classLoaders.helpers;

/**
 * a simple class managing a library version number in the format major.minor.subversion-build where minor, subversion
 * and build are optional
 * 
 * 
 *
 */
public class LibraryVersion implements Comparable<LibraryVersion> {

	private int major = 0;
	private Integer minor = null;
	private Integer sub = null;
	private Integer build = null;

	/**
	 * Generate a Version from String representation
	 * 
	 * format : major.minor.sub-build
	 * 
	 * minor, subversion and build are optional
	 * 
	 * @param version
	 * @throws IllegalArgumentException
	 */
	public LibraryVersion(String version) throws IllegalArgumentException {
		try {
			int posMinus = version.indexOf("-");
			String[] split;
			if (posMinus >= 0) {
				split = version.split("-");
				if (split.length != 2)
					throw new IllegalArgumentException("Syntax Error: more than one - in version string");

				this.build = Integer.valueOf(split[1]);
				version = split[0];

				if (this.build < 0)
					throw new IllegalArgumentException("Negative version numbers are not allowed!");
			}

			split = version.split("\\.");
			if (split.length > 3)
				throw new IllegalArgumentException(
						"Syntax Error: too many version numbers, a maximum of three is allowed");

			this.major = Integer.valueOf(split[0]);
			if (this.major < 0)
				throw new IllegalArgumentException("Negative version numbers are not allowed!");

			if (split.length > 1) {
				this.minor = Integer.valueOf(split[1]);
				if (this.minor < 0)
					throw new IllegalArgumentException("Negative version numbers are not allowed!");
			} else
				this.minor = null;

			if (split.length > 2) {
				this.sub = Integer.valueOf(split[2]);
				if (this.sub < 0)
					throw new IllegalArgumentException("Negative version numbers are not allowed!");
			} else
				this.sub = null;
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("The given string contains invalid number representations: " + version,
					e);
		}

	}

	/**
	 * generate a new LibraryVersion
	 * 
	 * @param major
	 * @param minor
	 * @param sub
	 * @param build
	 * @throws IllegalArgumentException a version is smaller than 0
	 */
	public LibraryVersion(int major, int minor, int sub, int build) throws IllegalArgumentException {
		this(major, minor, sub);
		if (build < 0)
			throw new IllegalArgumentException("Negative version numbers are not allowed!");

		this.build = build;
	}

	/**
	 * generate a new LibraryVersion
	 * 
	 * @param major
	 * @param minor
	 * @param sub
	 * @throws IllegalArgumentException a version is smaller than 0
	 */
	public LibraryVersion(int major, int minor, int sub) throws IllegalArgumentException {
		this(major, minor);
		if (sub < 0)
			throw new IllegalArgumentException("Negative version numbers are not allowed!");
		this.sub = sub;
	}

	/**
	 * generate a new LibraryVersion
	 * 
	 * @param major
	 * @param minor
	 * @throws IllegalArgumentException
	 */
	public LibraryVersion(int major, int minor) throws IllegalArgumentException {
		this(major);
		if (minor < 0)
			throw new IllegalArgumentException("Negative version numbers are not allowed!");
		this.minor = minor;
	}

	/**
	 * generate a new LibraryVersion
	 * 
	 * @param major
	 * @throws IllegalArgumentException
	 */
	public LibraryVersion(int major) throws IllegalArgumentException {
		if (major < 0)
			throw new IllegalArgumentException("Negative version numbers are not allowed!");
		this.major = major;
	}

	/**
	 * check if this version is larger than the given one
	 * 
	 * @param v
	 * @return true, if this version is larger than the given one
	 */
	public boolean isLargerThan(LibraryVersion v) {
		if (this.major > v.major)
			return true;
		if (this.major < v.major)
			return false;

		if (this.minor != null && v.minor != null) {
			if (this.minor > v.minor)
				return true;
			if (this.minor < v.minor)
				return false;
		} else if (this.minor == null)
			return false;
		else if (v.minor == null)
			return true;

		if (this.sub != null && v.sub != null) {
			if (this.sub > v.sub)
				return true;
			if (this.sub < v.sub)
				return false;
		} else if (this.sub == null)
			return false;
		else if (v.sub == null)
			return true;

		if (this.build == null)
			return false;
		if (v.build == null)
			return true;
		if (this.build > v.build)
			return true;

		return false;
	}

	/**
	 * tries to create a version number from the given string and compares it to this version
	 * 
	 * @param v
	 * @return true, if this version is larger than the given one
	 * @throws IllegalArgumentException
	 */
	public boolean isLargerThan(String v) throws IllegalArgumentException {
		return isLargerThan(new LibraryVersion(v));
	}

	/**
	 * check if this version is smaller than the given one
	 * 
	 * @param v
	 * @return true, if this version is smaller than the given one
	 */
	public boolean isSmallerThan(LibraryVersion v) {
		return !this.equals(v) && !this.isLargerThan(v);
	}

	/**
	 * tries to create a version number from the given string and compares it to this version
	 * 
	 * @param v
	 * @return true, if this version is smaller than the given one
	 * @throws IllegalArgumentException
	 */
	public boolean isSmallerThan(String v) {
		return isSmallerThan(new LibraryVersion(v));
	}

	/**
	 * check if this version is larger than or equal to the given one
	 * 
	 * @param v
	 * @return true, if this version is larger than or equal to the given one
	 */
	public boolean isLargerOrEqual(LibraryVersion v) {
		return this.equals(v) || this.isLargerThan(v);
	}

	/**
	 * tries to create a version number from the given string and compares it to this version
	 * 
	 * @param v
	 * @return true, if this version is larger than or equal to the given one
	 * @throws IllegalArgumentException
	 */
	public boolean isLargerOrEqual(String v) {
		return this.isLargerOrEqual(new LibraryVersion(v));
	}

	/**
	 * check if this version is smaller than or equal to the given one
	 * 
	 * @param v
	 * @return true, if this version is smaller than or equal to the given one
	 */
	public boolean isSmallerOrEqual(LibraryVersion v) {
		return this.equals(v) || this.isSmallerThan(v);
	}

	/**
	 * tries to create a version number from the given string and compares it to this version
	 * 
	 * @param v
	 * @return true, if this version is smaller than or equal to the given one
	 * @throws IllegalArgumentException
	 */
	public boolean isSmallerOrEqual(String v) {
		return this.isSmallerOrEqual(new LibraryVersion(v));
	}

	/**
	 * check if this version is between the given ones
	 * 
	 * @param smaller
	 * @param larger
	 * @return true, if this version is between the two given ones
	 */
	public boolean isBetween(LibraryVersion smaller, LibraryVersion larger) {
		if (smaller.isLargerThan(larger))
			return isBetween(larger, smaller);

		if (this.equals(smaller))
			return true;
		else if (this.equals(larger))
			return true;
		else
			return this.isLargerThan(smaller) && this.isSmallerThan(larger);
	}

	/**
	 * tries to create a version number from the given strings and compares them to this version
	 * 
	 * @param smaller
	 * @param larger
	 * @return true, if this version is between the two given ones
	 * @throws IllegalArgumentException
	 */
	public boolean isBetween(String smaller, String larger) {
		return this.isBetween(new LibraryVersion(smaller), new LibraryVersion(larger));
	}

	/**
	 * compares to version and checks for equality
	 * 
	 * @param v
	 * @return true, if this version is the same as the given one
	 */
	public boolean equals(LibraryVersion v) {
		return v.toString().equals(this.toString());
	}

	/**
	 * compares this version with any object
	 * 
	 * if the given object is a String, the string representation of this version is compared to the given string
	 * 
	 * @param o
	 * @return true, if the given object is a version and the same as this one
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof LibraryVersion)
			return this.equals((LibraryVersion) o);
		else if (o instanceof String)
			return this.toString().equals(o);
		else
			return super.equals(o);
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
		if (minor == null)
			return 0;
		return minor;
	}

	/**
	 * get the major version number
	 * 
	 * @return major version number
	 */
	public int getMajor() {
		return major;
	}

	/**
	 * get the subversion of the minor version number
	 * 
	 * @return suberverion number of minor
	 */
	public int getSub() {
		if (sub == null)
			return 0;
		return sub;
	}

	/**
	 * get the build number of this (sub)version
	 * 
	 * @return build number
	 */
	public int getBuild() {
		if (build == null)
			return 0;
		return build;
	}

	/**
	 * @return a String representation of this version
	 */
	@Override
	public String toString() {
		String result = "" + major;
		if (minor != null) {
			result += "." + minor;
			if (sub != null)
				result += "." + sub;
		}

		if (build != null)
			result += "-" + build;

		return result;
	}

	/**
	 * implementation of Comparable, so that the L2pClassLoader may maintain TreeSets of versions
	 * 
	 * @param other
	 * @return coparison code
	 */
	@Override
	public int compareTo(LibraryVersion other) {
		if (this.isSmallerThan(other))
			return -1;
		else if (this.equals(other))
			return 0;
		else
			return 1;
	}

}
