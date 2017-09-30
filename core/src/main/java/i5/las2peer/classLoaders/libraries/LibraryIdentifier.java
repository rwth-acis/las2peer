package i5.las2peer.classLoaders.libraries;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * a basic class managing a library identifier of the format name-number where number fits the format of
 * {@link LibraryVersion}
 * 
 */
public class LibraryIdentifier {

	private String name;
	private LibraryVersion version;

	public static LibraryIdentifier fromFilename(String filename) {
		String fileNameWithOutExt = new File(filename).getName().replaceFirst("[.][^.]+$", "");

		Pattern versionPattern = Pattern.compile("-[0-9]+(?:.[0-9]+(?:.[0-9]+)?)?(?:-[0-9]+)?$");
		Matcher m = versionPattern.matcher(fileNameWithOutExt);

		String sName = null;
		String sVersion = null;
		if (m.find()) { // look for version information in filename
			sName = fileNameWithOutExt.substring(0, m.start());
			sVersion = m.group().substring(1);
			return new LibraryIdentifier(sName, sVersion);
		} else {
			sName = fileNameWithOutExt;
			return new LibraryIdentifier(sName, (LibraryVersion) null);
		}
	}

	/**
	 * generate a new LibraryIdentifier from its string representation
	 * 
	 * @param name A canonical library name
	 * @throws IllegalArgumentException If the version information is not correctly formatted
	 */
	public LibraryIdentifier(String name) throws IllegalArgumentException {
		int index = name.indexOf(";version=\"");
		if (index == -1) {
			throw new IllegalArgumentException("String does not include version info");
		}

		if (name.charAt(name.length() - 1) != '"') {
			throw new IllegalArgumentException("Version info not in qoutes!");
		}

		this.name = name.substring(0, index);
		String versionInfo = name.substring(index + 10, name.length() - 1);

		this.version = new LibraryVersion(versionInfo);
	}

	/**
	 * generate a new identifier
	 * 
	 * @param name A canonical library name
	 * @param version A library version
	 * @throws IllegalArgumentException If the version information is not correctly formatted
	 */
	public LibraryIdentifier(String name, String version) throws IllegalArgumentException {
		if (version != null) {
			this.name = name;
			this.version = new LibraryVersion(version);
		} else {
			throw new IllegalArgumentException("null given as version information");
		}
	}

	/**
	 * generate a new library identifier
	 * 
	 * @param name A canonical library name
	 * @param version A library version
	 */
	public LibraryIdentifier(String name, LibraryVersion version) {
		this.name = name;
		this.version = version;
	}

	/**
	 * 
	 * @return version of this library
	 */
	public LibraryVersion getVersion() {
		return version;
	}

	/**
	 * 
	 * @return name of the library
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return a string representation of this identifier
	 */
	@Override
	public String toString() {
		return name + ";version=\"" + version.toString() + "\"";
	}

	/**
	 * compares this identifier to another one
	 * 
	 * @param i A library identifier
	 * @return true, if the given identifier is the same as this
	 */
	public boolean equals(LibraryIdentifier i) {
		return this.toString().equals(i.toString());
	}

	/**
	 * compares this identifier against other objects if a string is given, the string representation of this identifier
	 * is compared to the given string
	 * 
	 * @param o Another object to compare to
	 * @return true, if the given object is an identifier and is the same as this
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof LibraryIdentifier) {
			return this.equals((LibraryIdentifier) o);
		} else if (o instanceof String) {
			return this.toString().equals(o);
		} else {
			return false;
		}
	}

	/**
	 * since equals is overridden, we should implement an own hashCode.
	 * 
	 * @return a hash code
	 */
	@Override
	public int hashCode() {
		return (this.toString()).hashCode();
	}

}
