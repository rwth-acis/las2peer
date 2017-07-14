package i5.las2peer.classLoaders.libraries;

/**
 * a simple class managing a library version number in the format major.minor.subversion-build where minor, subversion
 * and build are optional
 * 
 * 
 *
 */
public class LibraryVersion {

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
}
