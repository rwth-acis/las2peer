package i5.las2peer.p2p;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import i5.las2peer.persistency.EnvelopeException;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.ServiceInfoAgent;
import rice.pastry.NodeHandle;

/**
 * Caching class to manage the knowledge of a node about existing services
 */
public class NodeServiceCache {	
	
	private final Node runningAt;
	
	private Hashtable<String, Hashtable<ServiceVersion, ServiceAgent>> localServiceVersions = new Hashtable<>();

	private long lifeTimeSeconds = 10;
	private Hashtable<String, ServiceVersionData> serviceVersions = new Hashtable<>();
	private Hashtable<String, ServiceAgentNodeData> serviceAgentNodeData = new Hashtable<String, ServiceAgentNodeData>();

	public NodeServiceCache(Node parent, long lifeTime) {
		this.runningAt = parent;
		this.lifeTimeSeconds = lifeTime;
	}
	
	public ServiceAgent getServiceAgent(String name, String version) {
		return getServiceAgent(new ServiceNameVersion(name,version));
	}

	public ServiceAgent getServiceAgent(ServiceNameVersion service) {
		ServiceAgentNodeData serviceData = update(service);

		if (serviceData != null) {
			return serviceData.getServiceAgent();
		} else {
			return null;
		}
	}
	
	private ServiceAgentNodeData getServiceAgentNodeData(ServiceNameVersion service) {
		return serviceAgentNodeData.get(service.toString());
	}
	
	public ArrayList<NodeHandle> getServiceNodes(String name, String version) {
		return getServiceNodes(new ServiceNameVersion(name,version));
	}

	public ArrayList<NodeHandle> getServiceNodes(ServiceNameVersion service) {
		ServiceAgentNodeData serviceData = update(service);
		if (serviceData != null) {
			return serviceData.getNodes();
		} else {
			return null;
		}
	}

	private boolean needsUpdate(long lastTime) {
		long current = System.currentTimeMillis() / 1000L;
		return (lastTime + lifeTimeSeconds) < current;
	}

	private ServiceAgentNodeData update(ServiceNameVersion service) {
		ServiceAgentNodeData serviceData = getServiceAgentNodeData(service);
		if (serviceData == null) {
			serviceData = new ServiceAgentNodeData(service);
			try {
				updateServiceAgentNodeData(serviceData);
				serviceAgentNodeData.put(service.toString(), serviceData);
				return serviceData;
			} catch (AgentNotKnownException | EnvelopeException e) {
				// do nothing
			}
			return null;
		} else {
			if (needsUpdate(serviceData.getLastUpdateTime()) == false) {
				return serviceData;
			}

			try {
				updateServiceAgentNodeData(serviceData);

			} catch (AgentNotKnownException | EnvelopeException e) {
				removeServiceAgentEntry(serviceData);
			}
			return serviceData;
		}
	}

	private void updateServiceAgentNodeData(ServiceAgentNodeData serviceData)
			throws AgentNotKnownException, EnvelopeException {
		ServiceAgent agent = this.runningAt.getServiceAgent(serviceData.getService());
		ArrayList<NodeHandle> nodes = ServiceInfoAgent.getNodes(serviceData.getService().getName(),serviceData.getService().getVersion());
		serviceData.setServiceAgent(agent);
		serviceData.setNodes(nodes);
		serviceData.setLastUpdateTime(System.currentTimeMillis() / 1000L);
	}
	
	private ServiceVersionData update(String serviceName) {
		ServiceVersionData serviceData = serviceVersions.get(serviceName);
		if (serviceData == null) {
			serviceData = new ServiceVersionData(serviceName);
			try {
				updateServiceVersionData(serviceData);
				serviceVersions.put(serviceName, serviceData);
				return serviceData;
			} catch (AgentNotKnownException | EnvelopeException e) {
				// do nothing
			}
			return null;
		} else {
			if (needsUpdate(serviceData.getLastUpdateTime()) == false) {
				return serviceData;
			}

			try {
				updateServiceVersionData(serviceData);

			} catch (AgentNotKnownException | EnvelopeException e) {
				serviceVersions.remove(serviceName);
			}
			return serviceData;
		}
	}

	private void updateServiceVersionData(ServiceVersionData serviceData)
			throws AgentNotKnownException, EnvelopeException {
		String[] versions = ServiceInfoAgent.getServiceVersions(serviceData.getServiceName());
		serviceData.setVersions(versions);
		serviceData.setLastUpdateTime(System.currentTimeMillis() / 1000L);
	}

	public void removeServiceAgentEntry(ServiceNameVersion service) {
		serviceAgentNodeData.remove(service.toString());
	}

	public void removeServiceAgentEntry(ServiceAgentNodeData serviceData) {
		removeServiceAgentEntry(serviceData.getService());
	}

	public void removeServiceAgentEntryNode(ServiceNameVersion service, NodeHandle handle) {
		ServiceAgentNodeData serviceData = getServiceAgentNodeData(service);
		if (serviceData != null) {
			serviceData.removeNode(handle);
		}
	}
	
	public ServiceVersion[] getVersions(String serviceName) {
		ServiceVersionData data = update(serviceName);
		
		if (data==null)
			return null;
		
		return data.getVersions();
	}
	
	// local services
	
	public void registerLocalService(ServiceAgent agent) {
		ServiceNameVersion serviceNameVersion = agent.getServiceNameVersion();
		ServiceVersion serviceVersion = new ServiceVersion(serviceNameVersion.getVersion());
		
		Hashtable<ServiceVersion,ServiceAgent> versions = localServiceVersions.get(serviceNameVersion.getName());
		if (versions == null) {
			versions = new Hashtable<>();
			localServiceVersions.put(serviceNameVersion.getName(), versions);
		}
		
		if (!versions.containsKey(serviceVersion)) {
			versions.put(serviceVersion, agent);
		}
		else if (versions.get(serviceVersion) != agent) {
			throw new IllegalStateException("Another ServiceAgent running the same Service is present on this Node - something went wrong!");
		}
	}
	
	public ServiceAgent getLocalServiceAgent(String name, ServiceVersion version) {
		Hashtable<ServiceVersion,ServiceAgent> versions = localServiceVersions.get(name);
		if (versions == null)
			return null;
		else
			return versions.get(version);
	}
	
	public ServiceAgent getLocalServiceAgent(ServiceNameVersion service) {
		return getLocalServiceAgent(service.getName(),new ServiceVersion(service.getVersion()));
	}
	
	public ServiceVersion[] getLocalVersions(String serviceName) {
		Hashtable<ServiceVersion,ServiceAgent> versions = localServiceVersions.get(serviceName);
		if (versions == null)
			return null;
		else
			return versions.keySet().toArray(new ServiceVersion[0]);
	}
	
	public void unregisterLocalService(ServiceAgent agent) {
		ServiceNameVersion serviceNameVersion = agent.getServiceNameVersion();
		
		Hashtable<ServiceVersion,ServiceAgent> versions = localServiceVersions.get(serviceNameVersion.getName());
		if (versions == null) return;
		ServiceVersion serviceVersion = new ServiceVersion(serviceNameVersion.getVersion());
		if (versions.get(serviceVersion) != agent) {
			throw new IllegalStateException("Another ServiceAgent running the same Service is present on this Node - something went wrong!");
		}
		versions.remove(serviceVersion);
		if (versions.size() == 0)
			localServiceVersions.remove(serviceNameVersion.getName());
	}

	public class ServiceAgentNodeData {

		private ServiceAgent serviceAgent = null;
		private ArrayList<NodeHandle> nodes = new ArrayList<NodeHandle>();
		private long lastUpdateTime;
		private ServiceNameVersion service;

		public ServiceNameVersion getService() {
			return service;
		}

		public long getLastUpdateTime() {
			return lastUpdateTime;
		}

		public void setLastUpdateTime(long lastUpdateTime) {
			this.lastUpdateTime = lastUpdateTime;
		}

		public ServiceAgent getServiceAgent() {
			return serviceAgent;
		}

		public void setServiceAgent(ServiceAgent serviceAgent) {
			this.serviceAgent = serviceAgent;
		}

		public ArrayList<NodeHandle> getNodes() {
			return nodes;
		}

		public void setNodes(ArrayList<NodeHandle> nodes) {
			this.nodes = nodes;
		}

		public ServiceAgentNodeData(ServiceNameVersion service) {
			this.service = service;
			this.lastUpdateTime = System.currentTimeMillis() / 1000L;
		}

		public void removeNode(NodeHandle handle) {
			nodes.remove(handle);
		}
	}
	
	public class ServiceVersionData {
		private long lastUpdateTime;
		private Set<ServiceVersion> versions = new HashSet<ServiceVersion>();
		private String serviceName;
		
		public ServiceVersionData(String name) {
			this.serviceName = name;
			this.lastUpdateTime = System.currentTimeMillis() / 1000L;
		}
		
		public String getServiceName() {
			return this.serviceName;
		}
		
		public void setVersions(String[] versions) {
			Set<ServiceVersion> set = new HashSet<ServiceVersion>();
			
			for (String v : versions) {
				set.add(new ServiceVersion(v));
			}
			
			this.versions = set;
		}
		
		public ServiceVersion[] getVersions() {
			return this.versions.toArray(new ServiceVersion[0]);
		}
		
		public long getLastUpdateTime() {
			return lastUpdateTime;
		}

		public void setLastUpdateTime(long lastUpdateTime) {
			this.lastUpdateTime = lastUpdateTime;
		}
	}

}
