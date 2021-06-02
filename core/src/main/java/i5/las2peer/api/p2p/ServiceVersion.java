package i5.las2peer.api.p2p;

import i5.las2peer.api.SemverVersion;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/**
 * a simple class managing a service version number in the format major.minor.patchversion-build where minor, patchversion
 * and build are optional
 *
 */
public class ServiceVersion extends SemverVersion implements Comparable<ServiceVersion> {

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
		super(version);
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
		super(major, minor, patch, preRelease, build);
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
		super(major, minor, patch, preRelease);
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
		super(major, minor,patch);
	}

	/**
	 * generate a new ServiceVersion
	 *
	 * @param major Major version number part
	 * @param minor Minor version number part
	 * @throws IllegalArgumentException If a version number part is smaller than 0
	 */
	public ServiceVersion(int major, int minor) throws IllegalArgumentException {
		super(major, minor);
	}

	/**
	 * generate a new ServiceVersion
	 *
	 * @param major Major version number part
	 * @throws IllegalArgumentException If a version number part is smaller than 0
	 */
	public ServiceVersion(int major) throws IllegalArgumentException {
		super(major);
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
