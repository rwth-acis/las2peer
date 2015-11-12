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
		return nameVersion;
	}

	private String name;
	private String version;

	private String nameVersion;

	public ServiceNameVersion(String name, String version) {
		this.name = name;
		this.version = version;
		nameVersion = name + SEPERATOR + version;
	}

	@Override
	public boolean equals(Object obj) {
		return nameVersion.equals(obj);
	}

	@Override
	public int hashCode() {
		return nameVersion.hashCode();
	}

	@Override
	public String toString() {
		return nameVersion;
	}

	public static String toString(String serviceName, String version) {
		return serviceName + SEPERATOR + version;
	}
}