package i5.las2peer.p2p;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

import rice.pastry.NodeHandle;

public class ServiceNodeList implements Serializable {

	private static final long serialVersionUID = 6785204387598553837L;

	HashSet<NodeHandle> availableNodes = new HashSet<NodeHandle>();
	private String serviceClass = "";

	public ServiceNodeList() {
	}

	public ServiceNodeList(String serviceClass) {
		this.serviceClass = serviceClass;
	}

	public String getServiceClass() {
		return serviceClass;
	}

	public ArrayList<NodeHandle> getNodes() {

		return new ArrayList<NodeHandle>(availableNodes);
		// return (NodeHandle[]) availableNodes.toArray();
	}

	public boolean hasNodes() {
		return availableNodes.size() > 0;
	}

	public void addNode(NodeHandle handle) {
		availableNodes.add(handle);
	}

	/**
	 * Removes a node from the list.
	 * 
	 * @param handle
	 * @return true, if new list is empty
	 */
	public boolean removeNode(NodeHandle handle) {
		availableNodes.remove(handle);
		return availableNodes.size() == 0;
	}

}
