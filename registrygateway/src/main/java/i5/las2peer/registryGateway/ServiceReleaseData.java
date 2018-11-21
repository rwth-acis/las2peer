package i5.las2peer.registryGateway;

import java.math.BigInteger;

public class ServiceReleaseData {
	private String serviceName;
	private BigInteger versionMajor;
	private BigInteger versionMinor;
	private BigInteger versionPatch;
	private byte[] signature; // TODO

	public ServiceReleaseData(String serviceName, BigInteger versionMajor, BigInteger versionMinor, BigInteger versionPatch, byte[] signature) {
		this.serviceName = serviceName;
		this.versionMajor = versionMajor;
		this.versionMinor = versionMinor;
		this.versionPatch = versionPatch;
		this.signature = signature;
	}

	public String getServiceName() {
		return serviceName;
	}

	public String getVersion() {
		return versionMajor + "." + versionMinor + "." + versionPatch;
	}

	public Integer getVersionMajor() {
		return versionMajor.intValue();
	}

	public Integer getVersionMinor() {
		return versionMinor.intValue();
	}

	public Integer getVersionPatch() {
		return versionPatch.intValue();
	}

	@Override
	public String toString() {
		return "ServiceReleaseData(service name: " + this.getServiceName() + ", version: " + this.getVersion() + ")";
	}
}
