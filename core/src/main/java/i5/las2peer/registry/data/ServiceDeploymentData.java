package i5.las2peer.registry.data;

import i5.las2peer.registry.Util;

import java.math.BigInteger;
import java.util.Objects;

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

	public String getServicePackageName() {
		return serviceName;
	}

	public String getServiceClassName() {
		return serviceClass;
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
		return "ServiceDeploymentData(package: " + this.getServicePackageName()
				+ ", class: " + this.getServiceClassName() + ", version: " + this.getVersion() + ", time: " + getTime()
				+ ", node ID: " + getNodeId() + ")";
	}

	/** Considered equal if all fields but timestamp match. */
	// the timestamp can be considered optional metadata
	// the intent here is to replace "outdated" instances when one with
	// a newer timestamp comes in
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ServiceDeploymentData that = (ServiceDeploymentData) o;
		return Objects.equals(serviceName, that.serviceName) &&
				Objects.equals(serviceClass, that.serviceClass) &&
				Objects.equals(versionMajor, that.versionMajor) &&
				Objects.equals(versionMinor, that.versionMinor) &&
				Objects.equals(versionPatch, that.versionPatch) &&
				Objects.equals(nodeId, that.nodeId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(serviceName, serviceClass, versionMajor, versionMinor, versionPatch, nodeId);
	}
}
