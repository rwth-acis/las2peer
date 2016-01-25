package i5.las2peer.p2p;

import java.io.Serializable;

/**
 * 
 */
public class ServiceNameVersion implements Serializable {
	private static final long serialVersionUID = 2683103174627316556L;
	public static final String SEPERATOR = "@";

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public String getNameVersion() {
		return name + SEPERATOR + version;
	}

	private String name;
	private String version;

	public ServiceNameVersion(String name, String version) {
		this.name = name;
		this.version = version;
	}

	@Override
	public boolean equals(Object obj) {
		return (name + SEPERATOR + version).equals(obj);
	}

	@Override
	public int hashCode() {
		return (name + SEPERATOR + version).hashCode();
	}

	@Override
	public String toString() {
		return name + SEPERATOR + version;
	}

	public static String toString(String serviceName, String version) {
		return serviceName + SEPERATOR + version;
	}
	
	public static ServiceNameVersion fromString(String nameVersion) {
		String[] a = nameVersion.split(SEPERATOR);
		if (a.length>1)
			return new ServiceNameVersion(a[0],a[1]);
		else
			return new ServiceNameVersion(a[0],"1.0"); // for compatibility
	}
}