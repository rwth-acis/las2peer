package i5.las2peer.p2p;

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
	 * @param name
	 * @param version see {@link ServiceVersion} how to specify a version
	 */
	public ServiceNameVersion(String name, String version) {
		this(name, new ServiceVersion(version));
	}

	/**
	 * Instantiate a ServiceNameVersion
	 * 
	 * @param name
	 * @param version
	 */
	public ServiceNameVersion(String name, ServiceVersion version) {
		this.name = name;
		this.version = version;
	}

	/**
	 * get the service name
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * get the service version
	 * 
	 * @return
	 */
	public ServiceVersion getVersion() {
		return version;
	}

	/**
	 * get a string representation
	 * 
	 * @return
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
	 * @param nameVersion
	 * @return
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
	 * true if the names are equal and this version fits the required version
	 * 
	 * @param required
	 * @return
	 */
	public boolean fits(ServiceNameVersion required) {
		return (this.name.equals(required.getName()) && this.getVersion().fits(required.getVersion()));
	}
}