package i5.las2peer.registryGateway;

import java.math.BigInteger;

public class ServiceDeploymentData {
	private String serviceName;
	private String serviceClass;
	private BigInteger versionMajor;
	private BigInteger versionMinor;
	private BigInteger versionPatch;
	private BigInteger timestamp;
	private String nodeId;

	public ServiceDeploymentData(String serviceName, String serviceClass, BigInteger versionMajor, BigInteger versionMinor, BigInteger versionPatch, BigInteger timestamp, String nodeId) {
		this.serviceName = serviceName;
		this.serviceClass = serviceClass;
		this.versionMajor = versionMajor;
		this.versionMinor = versionMinor;
		this.versionPatch = versionPatch;
		this.timestamp = timestamp;
		this.nodeId = nodeId;
	}

	public String getServiceName() {
		return serviceName;
	}

	public String getVersion() {
		return versionMajor + "." + versionMinor + "." + versionPatch;
	}

	public String getTime() {
		return Util.unixtimeToString(timestamp.longValue());
	}

	public BigInteger getTimestamp() {
		return timestamp;
	}

	public String getNodeId() {
		return nodeId;
	}

	@Override
	public String toString() {
		return "ServiceDeploymentData(service name: " + this.getServiceName() + ", version: " + this.getVersion() + ", time: " + getTime() + ", node ID: " + getNodeId() + ")";
	}
}
