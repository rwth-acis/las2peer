package i5.las2peer.classLoaders.libraries;

import i5.las2peer.api.SemverVersion;
import i5.las2peer.api.p2p.ServiceVersion;

/**
 * A simple class managing a library version number in the format
 * {@code <major> "." <minor> "." <patch> "-" <pre-release> "+" <build>} (where minor, patch, pre-release and build
 * are optional) or "*" (no version specified / matches all versions).
 */
public class LibraryVersion  extends SemverVersion {

	/**
	 * Generate a Version from String representation
	 *
	 * format : Semver {@code <major> "." <minor> "." <patch> "-" <pre-release> "+" <build>} (where minor, patch,
	 * pre-release and build are optional) or "*" (no version specified / matches all versions)
	 *
	 * @param version A version string representation
	 * @throws IllegalArgumentException If the string contains no valid version representation
	 */
	public LibraryVersion(String version) throws IllegalArgumentException {
		super(version);
	}

	/**
	 * generate a new LibraryVersion
	 *
	 * @param major Major version number part
	 * @param minor Minor version number part
	 * @param patch Sub version number part
	 * @param preRelease pre-release version
	 * @param build build version
	 * @throws IllegalArgumentException If a version number part is smaller than 0
	 */
	public LibraryVersion(int major, int minor, int patch, String preRelease, String build) throws IllegalArgumentException {
		super(major, minor, patch, preRelease, build);
	}

	/**
	 * generate a new LibraryVersion
	 *
	 * @param major Major version number part
	 * @param minor Minor version number part
	 * @param patch Sub version number part
	 * @param preRelease pre-release version
	 * @throws IllegalArgumentException If a version number part is smaller than 0
	 */
	public LibraryVersion(int major, int minor, int patch, String preRelease) throws IllegalArgumentException {
		super(major, minor, patch, preRelease);
	}

	/**
	 * generate a new LibraryVersion
	 *
	 * @param major Major version number part
	 * @param minor Minor version number part
	 * @param patch patch version number part
	 * @throws IllegalArgumentException If a version number part is smaller than 0
	 */
	public LibraryVersion(int major, int minor, int patch) throws IllegalArgumentException {
		super(major, minor, patch);
	}

	/**
	 * generate a new LibraryVersion
	 *
	 * @param major Major version number part
	 * @param minor Minor version number part
	 * @throws IllegalArgumentException If a version number part is smaller than 0
	 */
	public LibraryVersion(int major, int minor) throws IllegalArgumentException {
		super(major, minor);
	}

	/**
	 * generate a new LibraryVersion
	 *
	 * @param major Major version number part
	 * @throws IllegalArgumentException If a version number part is smaller than 0
	 */
	public LibraryVersion(int major) throws IllegalArgumentException {
		super(major);
	}
}
