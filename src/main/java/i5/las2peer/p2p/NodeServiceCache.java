package i5.las2peer.p2p;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import i5.las2peer.persistency.EnvelopeException;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.ServiceInfoAgent;
import rice.pastry.NodeHandle;

/**
 * Caching class to manage the knowledge of a node about existing services
 */
public class NodeServiceCache {

	private final Node runningAt;

	private long lifeTimeSeconds = 10;
	private HashMap<String, ServiceAgentNodeData> data = new HashMap<String, ServiceAgentNodeData>();
	private Random random = new Random();

	public NodeServiceCache(Node parent, long lifeTime) {
		this.runningAt = parent;
		this.lifeTimeSeconds = lifeTime;
	}

	public long getLifeTimeSeconds() {
		return lifeTimeSeconds;
	}

	public ServiceAgent getServiceAgent(String serviceName, String version) {
		ServiceAgentNodeData serviceData = update(serviceName, version);

		if (serviceData != null) {
			return serviceData.getServiceAgent();
		} else {
			return null;
		}
	}

	private ServiceAgentNodeData getServiceAgentNodeData(String serviceName, String version) {
		String name = ServiceNameVersion.toString(serviceName, version);
		return data.get(name);
	}

	public NodeHandle getRandomServiceNode(String serviceName, String version) {
		ArrayList<NodeHandle> nodes = getServiceNodes(serviceName, version);
		if (nodes == null || nodes.size() == 0) {
			return null;
		}
		return nodes.get(random.nextInt(nodes.size()));
	}

	public ArrayList<NodeHandle> getServiceNodes(String serviceName, String version) {
		ServiceAgentNodeData serviceData = update(serviceName, version);
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

	private ServiceAgentNodeData update(String serviceName, String version) {
		ServiceAgentNodeData serviceData = getServiceAgentNodeData(serviceName, version);
		if (serviceData == null) {
			serviceData = new ServiceAgentNodeData(serviceName, version);
			try {
				updateServiceAgentNodeData(serviceData);
				data.put(ServiceNameVersion.toString(serviceName, version), serviceData);
				return serviceData;
			} catch (AgentNotKnownException e) {
				// do nothing
			} catch (EnvelopeException e) {
				// do nothing
			}
			return null;
		} else {
			if (needsUpdate(serviceData.getLastUpdateTime()) == false) {
				return serviceData;
			}

			try {
				updateServiceAgentNodeData(serviceData);

			} catch (AgentNotKnownException e) {
				removeEntry(serviceData);
			} catch (EnvelopeException e) {
				removeEntry(serviceData);
			}
			return serviceData;
		}
	}

	private void updateServiceAgentNodeData(ServiceAgentNodeData serviceData)
			throws AgentNotKnownException, EnvelopeException {
		ServiceAgent agent = this.runningAt.getServiceAgent(serviceData.getServiceClass());
		ArrayList<NodeHandle> nodes = ServiceInfoAgent.getNodes(serviceData.getServiceClass(),
				serviceData.getVersion());
		serviceData.setServiceAgent(agent);
		serviceData.setNodes(nodes);
		serviceData.setLastUpdateTime(System.currentTimeMillis() / 1000L);
	}

	public void removeEntry(String serviceName, String version) {
		data.remove(ServiceNameVersion.toString(serviceName, version));
	}

	public void removeEntry(ServiceAgentNodeData serviceData) {
		removeEntry(serviceData.getServiceClass(), serviceData.getVersion());
	}

	public void removeEntryNode(String serviceName, String version, NodeHandle handle) {
		ServiceAgentNodeData serviceData = getServiceAgentNodeData(serviceName, version);
		if (serviceData != null) {
			serviceData.removeNode(handle);
		}
	}

	public class ServiceAgentNodeData {

		private ServiceAgent serviceAgent = null;
		private ArrayList<NodeHandle> nodes = new ArrayList<NodeHandle>();
		private long lastUpdateTime;
		private String serviceClass = "";
		private String version = "";

		public String getVersion() {
			return version;
		}

		public String getServiceClass() {
			return serviceClass;
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

		public ServiceAgentNodeData(String serviceClass, String version) {
			this.serviceClass = serviceClass;
			this.version = version;
			this.lastUpdateTime = System.currentTimeMillis() / 1000L;
		}

		public void removeNode(NodeHandle handle) {
			nodes.remove(handle);
		}
	}

}
