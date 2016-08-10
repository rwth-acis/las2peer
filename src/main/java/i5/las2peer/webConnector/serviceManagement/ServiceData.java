package i5.las2peer.webConnector.serviceManagement;

import i5.las2peer.p2p.ServiceNameVersion;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.data.PathTree;

public class ServiceData {
	private ServiceNameVersion service;
	private PathTree tree;
	private long lastAccess;

	public ServiceData(ServiceNameVersion service, String xml) throws Exception {
		this.service = service;
		this.tree = RESTMapper.getMappingTree(xml);
		touch();
	}

	public ServiceNameVersion getService() {
		return service;
	}

	public PathTree getTree() {
		return tree;
	}

	public void touch() {
		lastAccess = System.currentTimeMillis();
	}

	public long lastAccess() {
		return lastAccess;
	}

}
