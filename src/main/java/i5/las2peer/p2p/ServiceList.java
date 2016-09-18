package i5.las2peer.p2p;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * This class stores information about each service, that is registered in the network
 */
public class ServiceList implements Serializable {

	private static final long serialVersionUID = -4423523057987069062L;

	private HashMap<String, HashMap<ServiceVersion, ServiceNameVersion>> services = new HashMap<>();

	/**
	 * Default constructor
	 */
	public ServiceList() {
	}

	/**
	 * Returns an array with the names of all registered services
	 * 
	 * @return the service names
	 */
	public ServiceNameVersion[] getServices() {
		ArrayList<ServiceNameVersion> result = new ArrayList<>();
		Iterator<Entry<String, HashMap<ServiceVersion, ServiceNameVersion>>> it = services.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, HashMap<ServiceVersion, ServiceNameVersion>> versions = it.next();
			Iterator<Entry<ServiceVersion, ServiceNameVersion>> it2 = versions.getValue().entrySet().iterator();
			while (it2.hasNext()) {
				Entry<ServiceVersion, ServiceNameVersion> pair = it2.next();
				result.add(pair.getValue());
			}
		}
		return result.toArray(new ServiceNameVersion[0]);
	}

	/**
	 * Returns an array with versions of the given servie name
	 * 
	 * @param serviceName the service's name
	 * @return
	 */
	public String[] getVersions(String serviceName) {
		HashMap<ServiceVersion, ServiceNameVersion> versions = services.get(serviceName);
		if (versions == null) {
			return new String[0];
		} else {
			return versions.keySet().toArray(new String[0]);
		}
	}

	/**
	 * Adds a new service to the list
	 * 
	 * @param serviceNameVersion the service version to add
	 */
	public void addService(ServiceNameVersion serviceNameVersion) {
		HashMap<ServiceVersion, ServiceNameVersion> versions = services.get(serviceNameVersion.getName());
		if (versions == null) {
			versions = new HashMap<ServiceVersion, ServiceNameVersion>();
			services.put(serviceNameVersion.getName(), versions);
		}

		if (!versions.containsKey(serviceNameVersion.getVersion())) {
			versions.put(serviceNameVersion.getVersion(), serviceNameVersion);
		}
	}

	/**
	 * Removes a service from the list
	 * 
	 * @param serviceNameVersion the service version to remove
	 */
	public void removeService(ServiceNameVersion serviceNameVersion) {
		HashMap<ServiceVersion, ServiceNameVersion> versions = services.get(serviceNameVersion.getName());
		if (versions != null) {
			if (versions.containsKey(serviceNameVersion.getVersion())) {
				versions.remove(serviceNameVersion.getVersion());
			}

			if (versions.size() == 0) {
				services.remove(serviceNameVersion.getName());
			}
		}
	}

}
