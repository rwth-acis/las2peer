package i5.las2peer.webConnector.serviceManagement;

import i5.las2peer.execution.NoSuchServiceMethodException;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.ServiceNameVersion;
import i5.las2peer.restMapper.data.PathTree;
import i5.las2peer.security.Agent;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is responsible for fetching, parsing and caching REST mappings.
 * 
 * It will keep up to {@link #maxCacheSize} REST mappings in memory. If this limit is exeeded, old entrys are deleted
 * until {{@link #wipeToSize} entrys are left.
 *
 */
public class ServiceMappingManager {
	public static final String SERVICE_SELFINFO_METHOD = "getRESTMapping";

	private static final L2pLogger logger = L2pLogger.getInstance(ServiceMappingManager.class.getName());

	private int maxCacheSize = 100;
	private int wipeToSize = 75;

	private ConcurrentHashMap<ServiceNameVersion, ServiceData> serviceRepository = new ConcurrentHashMap<>();

	private Node node;

	public ServiceMappingManager(Node node) {
		this.node = node;
	}

	public PathTree getServiceTree(ServiceNameVersion service, Agent acting, boolean localOnly) throws Exception {
		// get XML mapping if it doesn't exist
		if (!serviceRepository.containsKey(service)) {
			try {
				String xml = (String) node.invoke(acting, service, SERVICE_SELFINFO_METHOD, new Serializable[] {},
						false, localOnly);
				if (xml == null || xml.isEmpty()) {
					throw new Exception("Couldn't get xml mapping for " + service + "!");
				} else {
					try {
						serviceRepository.put(service, new ServiceData(service, xml));
					} catch (Exception e) {
						throw new Exception("Couldn't parse xml mapping for " + service + "!", e);
					}
				}
			} catch (NoSuchServiceMethodException e) {
				throw new Exception("service " + service + " doesn't provide a REST mapping", e);
			} catch (Exception e) {
				throw new Exception("Couldn't get xml mapping for " + service + "!", e);
			}

			// clean up old entrys
			if (serviceRepository.size() > maxCacheSize) {
				ServiceData[] data = serviceRepository.entrySet().toArray(new ServiceData[0]);
				Arrays.sort(data, Comparator.comparing(ServiceData::lastAccess));
				for (int i = 0; i < data.length - wipeToSize; i++) {
					serviceRepository.remove(data[i].getService());
				}
			}
		}

		serviceRepository.get(service).touch();

		return serviceRepository.get(service).getTree();

	}
}
