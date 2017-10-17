package i5.las2peer.api.p2p;

import java.io.Serializable;

/**
 * identifies a service name and version
 */
public class ServiceNameVersion implements Serializable {
	private static final long serialVersionUID = 2683103174627316556L;
	public static final String SEPERATOR = "@";

	private String name;
	private ServiceVersion version;

	/**
	 * Instantiate a ServiceNameVersion
	 * 
	 * @param name A canonical service name
	 * @param version see {@link ServiceVersion} how to specify a version
	 */
	public ServiceNameVersion(String name, String version) {
		this(name, new ServiceVersion(version));
	}

	/**
	 * Instantiate a ServiceNameVersion
	 * 
	 * @param name A canonical service name
	 * @param version see {@link ServiceVersion} how to specify a version
	 */
	public ServiceNameVersion(String name, ServiceVersion version) {
		this.name = name;
		this.version = version;
	}

	/**
	 * Gets the service name
	 * 
	 * @return Returns the service name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the service version
	 * 
	 * @return Returns the service version
	 */
	public ServiceVersion getVersion() {
		return version;
	}

	/**
	 * Gets a string representation
	 * 
	 * @return Returns the name version string
	 */
	public String getNameVersion() {
		return this.toString();
	}

	/**
	 * true if name and version are the same
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ServiceNameVersion) {
			return this.toString().equals(obj.toString());
		}

		return this.toString().equals(obj);
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public String toString() {
		return toString(name, version);
	}

	public static String toString(String serviceName, String version) {
		return serviceName + SEPERATOR + version;
	}

	public static String toString(String serviceName, ServiceVersion version) {
		return toString(serviceName, version.toString());
	}

	/**
	 * construct a ServiceNameVersion from a String of the format name@version
	 * 
	 * @param nameVersion A string representing a canonical service name and version
	 * @return Returns the service name version
	 */
	public static ServiceNameVersion fromString(String nameVersion) {
		String[] a = nameVersion.split(SEPERATOR);
		if (a.length > 1) {
			return new ServiceNameVersion(a[0], a[1]);
		} else {
			return new ServiceNameVersion(a[0], "*");
		}
	}

	/**
	 * Checks if the this version fits the required version
	 * 
	 * @param required A required service name and version to check for
	 * @return Returns true if the names are equal and this version fits the required version
	 */
	public boolean fits(ServiceNameVersion required) {
		return (this.name.equals(required.getName()) && this.getVersion().fits(required.getVersion()));
	}

}