package i5.las2peer.classLoaders.helpers;

/**
 * a library dependency defines a library and a version range to match
 * 
 */
public class LibraryDependency {

	private String name;
	private LibraryVersion min, max;

	private boolean optional = false;
	private boolean minIncluded = true;
	private boolean maxIncluded = true;

	/**
	 * generate a new dependency
	 * 
	 * @param name
	 * @param min
	 * @param max
	 */
	public LibraryDependency(String name, LibraryVersion min, LibraryVersion max) {
		this.name = name;
		this.min = min;
		this.max = max;
	}

	/**
	 * generate a new dependency optional dependencies are defined in osgi, to keep compatibility, we introduced this
	 * here but probably won't need it
	 * 
	 * @param name
	 * @param min
	 * @param max
	 * @param optional
	 */
	public LibraryDependency(String name, LibraryVersion min, LibraryVersion max, boolean optional) {
		this(name, min, max);
		this.optional = optional;
	}

	/**
	 * generate a new Dependency without a version range (only single version possible)
	 * 
	 * @param name
	 * @param version
	 */
	public LibraryDependency(String name, LibraryVersion version) {
		this(name, version, version);
	}

	/**
	 * generate a new dependency
	 * 
	 * @param name
	 * @param min
	 * @param max
	 */
	public LibraryDependency(String name, String min, String max) {
		this.name = name;
		this.min = new LibraryVersion(min);
		this.max = new LibraryVersion(max);
	}

	/**
	 * generate a new Dependency without a version range (only single version possible)
	 * 
	 * @param name
	 * @param version
	 */
	public LibraryDependency(String name, String version) {
		this(name, version, version);
	}

	/**
	 * generate a new Dependency without a version range directly from an identifier String
	 * 
	 * @param depString
	 */
	public LibraryDependency(String depString) {
		String[] split = depString.split("\\s*;\\s*");
		name = split[0];

		String versionInfo = null;

		for (int i = 1; i < split.length; i++) {
			if (split[i].startsWith("version="))
				versionInfo = split[i].substring(8);
			else if (split[i].equals("resolution:=\"optional\""))
				this.optional = true;
			else
				throw new IllegalArgumentException("unkown declaration '" + split[i] + "' for library " + name);
		}

		if (versionInfo == null)
			throw new IllegalArgumentException("version info missing for library " + name);

		versionInfo = versionInfo.trim();
		if (versionInfo.charAt(0) != '"' || versionInfo.charAt(versionInfo.length() - 1) != '"')
			throw new IllegalArgumentException("version info (" + name + ") has to be enclosed in quotes!");

		versionInfo = versionInfo.substring(1, versionInfo.length() - 1);

		if (versionInfo.charAt(0) == '(') {
			minIncluded = false;
			versionInfo = versionInfo.substring(1);
		} else if (versionInfo.charAt(0) == '[')
			versionInfo = versionInfo.substring(1);

		if (versionInfo.charAt(versionInfo.length() - 1) == ')') {
			maxIncluded = false;
			versionInfo = versionInfo.substring(0, versionInfo.length() - 1);
		} else if (versionInfo.charAt(versionInfo.length() - 1) == ']')
			versionInfo = versionInfo.substring(0, versionInfo.length() - 1);

		split = versionInfo.split(",");
		if (split.length == 1) {
			min = max = new LibraryVersion(split[0]);

			if (!minIncluded || !maxIncluded)
				throw new IllegalArgumentException("this dependency cannot be matched!");
		} else if (split.length == 2) {
			min = new LibraryVersion(split[0]);
			max = new LibraryVersion(split[1]);
		} else
			throw new IllegalArgumentException("version info not correct");
	}

	/**
	 * generate a new Dependency without a version range directly from an identifier
	 * 
	 * @param identifier
	 */
	public LibraryDependency(LibraryIdentifier identifier) {
		this(identifier.getName(), identifier.getVersion());
	}

	/**
	 * 
	 * @return minimal version of the required library
	 */
	public LibraryVersion getMin() {
		return min;
	}

	/**
	 * 
	 * @return maximal version of the required library
	 */
	public LibraryVersion getMax() {
		return max;
	}

	/**
	 * 
	 * @return name of the required library
	 */
	public String getName() {
		return name;
	}

	/**
	 * is this dependency optional?
	 * 
	 * @return true, if this dependency is marked as optional
	 */
	public boolean isOptional() {
		return optional;
	}

	/**
	 * is the minimal version included in the possible range?
	 * 
	 * @return true, if the minimal version is included in the possible range
	 */
	public boolean isMinIncluded() {
		return minIncluded;
	}

	/**
	 * is the maximal version included in the possible range?
	 * 
	 * @return true, if the maximal version is included in the possible range
	 */
	public boolean isMaxIncluded() {
		return maxIncluded;
	}

	/**
	 * check, if the given library identifier fits this dependency
	 * 
	 * @param id
	 * @return true, if the given id fits this dependency
	 */
	public boolean fits(LibraryIdentifier id) {
		return this.name.equals(id.getName()) && this.fits(id.getVersion());
	}

	/**
	 * check, if the given library version fits this dependency
	 * 
	 * @param version
	 * @return true, if the given version fits this dependency
	 */
	public boolean fits(LibraryVersion version) {
		if (!this.isMinIncluded() && version.equals(this.getMin()))
			return false;
		if (!this.isMaxIncluded() && version.equals(this.getMax()))
			return false;

		return version.isBetween(this.getMin(), this.getMax());
	}

	/**
	 * check, if the given library identifier fits this dependency
	 * 
	 * @param id
	 * @return true, if the given library id fits this dependency
	 */
	public boolean fits(String id) {
		return this.fits(new LibraryIdentifier(id));
	}

	/**
	 * @return string representation of this dependency
	 */
	@Override
	public String toString() {
		String result = name + ";version=\"";
		if (min.equals(max))
			result += min.toString() + "\"";
		else {
			if (minIncluded)
				result += "[";
			else
				result += "(";
			result += min.toString() + "," + max.toString();

			if (maxIncluded)
				result += "]";
			else
				result += ")";
			result += "\"";
		}

		if (optional)
			result += ";resolution:=\"optional\"";

		return result;
	}

	/**
	 * generate an array of LibraryDependencies from a comma-separated String
	 * 
	 * @param multiple
	 * @return array of dependencies
	 */
	public static LibraryDependency[] fromString(String multiple) {
		if (multiple == null || multiple.trim().isEmpty())
			return new LibraryDependency[0];

		String[] split = multiple.trim().split(",");

		// take care of "," in version information!
		// collect reconcatenated strings in first opening position
		// delete all predesessing strings
		int count = 0;
		for (int i = 0; i < split.length; i++) {
			int iStart = i;
			while ((split[iStart].replaceAll("[^\"]", "").length()) % 2 == 1 && i < split.length - 1) {
				i++;
				split[iStart] += "," + split[i];
				split[i] = null;
			}

			count++;
		}

		// now just use all non deleted string to generate a dependency
		LibraryDependency[] result = new LibraryDependency[count];
		count = 0;
		for (int i = 0; i < split.length; i++) {
			if (split[i] != null) {
				result[count] = new LibraryDependency(split[i].trim());
				count++;
			}

		}

		return result;
	}

}
