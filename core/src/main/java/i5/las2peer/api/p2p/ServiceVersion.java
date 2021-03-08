package i5.las2peer.api.p2p;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/**
 * a simple class managing a service version number in the format major.minor.patchversion-build where minor, patchversion
 * and build are optional
 *
 */
public class ServiceVersion implements Comparable<ServiceVersion>, Serializable {

	private static final long serialVersionUID = 1L;

	private String value;

	private Integer major = null;
	private Integer minor = null;
	private Integer patch = null;

	private String build = null;
	private String preRelease = null;

	/**
	 * Generate a Version from String representation
	 *
	 * format : Semver {@code <major> "." <minor> "." <patch> "-" <pre-release> "+" <build>} (where minor, patch, pre-release and build are optional) or "*" (no version specified /
	 * matches all versions)
	 *
	 * @param version A version string representation to parse
	 * @throws IllegalArgumentException If the string contains no valid version representation
	 */
	public ServiceVersion(String version) throws IllegalArgumentException {
		this.value = version;

		if (version != null && !version.equals("*")) {
			try {

				String[] split;
				String rawVersion;
				// Separate additional metadata (pre-release/build)
				if (hasPreRelease(version)) {
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
	public ServiceVersion(int major, int minor, int patch, String preRelease, String build) throws IllegalArgumentException {
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
	public ServiceVersion(int major, int minor, int patch, String preRelease) throws IllegalArgumentException {
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
	public ServiceVersion(int major, int minor, int patch) throws IllegalArgumentException {
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
	public ServiceVersion(int major, int minor) throws IllegalArgumentException {
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
	public ServiceVersion(int major) throws IllegalArgumentException {
		if (major < 0) {
			throw new IllegalArgumentException("Negative version numbers are not allowed!");
		}
		this.major = major;
	}

	/**
	 * Identify if this version contains a pre-release according to semver specification
	 * @param version passed version string
	 * @return if this version contains a pre-release
	 */
	private boolean hasPreRelease(String version) {

		int firstIndexOfPlus = value.indexOf("+");
		int firstIndexOfHyphen = value.indexOf("-");

		if (firstIndexOfHyphen == -1) {
			return false;
		}

		return firstIndexOfPlus == -1 || firstIndexOfHyphen < firstIndexOfPlus;
	}

	/**
	 * check if this version is larger than the given one
	 *
	 * @param v Another service version to compare to
	 * @return true, if this version is larger than the given one
	 */
	public boolean isLargerThan(ServiceVersion v) {
		if (this.major != null && v.major != null) {
			if (this.major > v.major) {
				return true;
			}
			if (this.major < v.major) {
				return false;
			}
		} else if (this.major == null) {
			return false;
		} else if (v.major == null) {
			return true;
		}

		if (this.minor != null && v.minor != null) {
			if (this.minor > v.minor) {
				return true;
			}
			if (this.minor < v.minor) {
				return false;
			}
		} else if (this.minor == null) {
			return false;
		} else if (v.minor == null) {
			return true;
		}

		if (this.patch != null && v.patch != null) {
			if (this.patch > v.patch) {
				return true;
			}
			if (this.patch < v.patch) {
				return false;
			}
		} else if (this.patch == null) {
			return false;
		} else if (v.patch == null) {
			return true;
		}

		// Check if both are not a pre-release
		if (this.preRelease == null && v.preRelease == null) {
			return false;
		}

		// At least one version is a pre-release

		// Check self no pre-release, other pre-release, release takes precedence
		if (this.preRelease == null) {
			return true;
		}

		// Now compare pre-releases
		return this.preRelease.compareTo(v.preRelease) > 0;
	}

	/**
	 * check if this version is smaller than the given one
	 *
	 * @param v Another service version to compare to
	 * @return true, if this version is smaller than the given one
	 */
	public boolean isSmallerThan(ServiceVersion v) {
		return !this.equals(v) && !this.isLargerThan(v);
	}

	/**
	 * check if this version is larger than or equal to the given one
	 *
	 * @param v Another service version to compare to
	 * @return true, if this version is larger than or equal to the given one
	 */
	public boolean isLargerOrEqual(ServiceVersion v) {
		return this.equals(v) || this.isLargerThan(v);
	}

	/**
	 * check if this version is smaller than or equal to the given one
	 *
	 * @param v Another service version to compare to
	 * @return true, if this version is smaller than or equal to the given one
	 */
	public boolean isSmallerOrEqual(ServiceVersion v) {
		return this.equals(v) || this.isSmallerThan(v);
	}

	/**
	 * check if this version is between the given ones
	 *
	 * @param smaller A smaller service version to check for
	 * @param larger A larger service version to check for
	 * @return true, if this version is between the two given ones
	 */
	public boolean isBetween(ServiceVersion smaller, ServiceVersion larger) {
		if (smaller.isLargerThan(larger)) {
			return isBetween(larger, smaller);
		}

		if (this.equals(smaller)) {
			return true;
		} else if (this.equals(larger)) {
			return true;
		} else {
			return this.isLargerThan(smaller) && this.isSmallerThan(larger);
		}
	}

	/**
	 * tries to create a version number from the given strings and compares them to this version
	 *
	 * @param smaller A smaller service version to check for
	 * @param larger A larger service version to check for
	 * @return true, if this version is between the two given ones
	 */
	public boolean isBetween(String smaller, String larger) {
		return this.isBetween(new ServiceVersion(smaller), new ServiceVersion(larger));
	}

	/**
	 * compares to version and checks for equality
	 *
	 * @param v Another service version to compare to
	 * @return true, if this version is the same as the given one
	 */
	public boolean equals(ServiceVersion v) {
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
		if (o instanceof ServiceVersion) {
			return this.equals((ServiceVersion) o);
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
	 * @return patcher version number of minor
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
	 * @return suberversion number of minor
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
	/**
	 * implementation of Comparable
	 *
	 * @param other Another service version to check
	 * @return comparison code
	 */
	@Override
	public int compareTo(ServiceVersion other) {
		if (this.isSmallerThan(other)) {
			return -1;
		} else if (this.equals(other)) {
			return 0;
		} else {
			return 1;
		}
	}

	/**
	 * checks if this version "fits" to the required version
	 *
	 * e.g. "1.5.2-123" will fit "1.5", but "1.5" won't fit "1.6" or "1.5.2"
	 *
	 * @param required A required service version to check for
	 * @return Returns true if this version fits the required version
	 */
	public boolean fits(ServiceVersion required) {
		if (required.major == null) {
			return true;
		}
		if (this.major == null || required.major.intValue() != this.major.intValue()) {
			return false;
		}

		if (required.minor == null) {
			return true;
		}
		if (this.minor == null || required.minor.intValue() != this.minor.intValue()) {
			return false;
		}

		if (required.patch == null) {
			return true;
		}
		if (this.patch == null || required.patch.intValue() != this.patch.intValue()) {
			return false;
		}

		if (required.preRelease == null) {
			return true;
		}

		if (!Objects.equals(required.preRelease, this.preRelease)) {
			return false;
		}

		if (required.build == null) {
			return true;
		}

		if (this.build == null || !Objects.equals(required.build, this.build)) {
			return false;
		}

		return true;
	}

	/**
	 * returns the newest ServiceVersion from all available ServiceVersions that fits this version
	 *
	 * @param available available versions
	 * @return a fitting ServiceVersion or null if no fitting version exists
	 */
	public ServiceVersion chooseFittingVersion(ServiceVersion[] available) {
		if (available.length == 0) {
			return null;
		}

		Arrays.sort(available, Comparator.comparing((ServiceVersion s) -> s).reversed());

		for (ServiceVersion s : available) {
			if (s.fits(this)) {
				return s;
			}
		}

		return null;
	}

}
