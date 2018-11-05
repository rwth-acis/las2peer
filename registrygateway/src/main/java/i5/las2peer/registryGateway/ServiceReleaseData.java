package i5.las2peer.registryGateway;

import java.math.BigInteger;

class ServiceReleaseData {
	private byte[] serviceName;
	private BigInteger versionMajor;
	private BigInteger versionMinor;
	private BigInteger versionPatch;
	private byte[] signature; // TODO

	public ServiceReleaseData(byte[] serviceName, BigInteger versionMajor, BigInteger versionMinor, BigInteger versionPatch, byte[] signature) {
		this.serviceName = serviceName;
		this.versionMajor = versionMajor;
		this.versionMinor = versionMinor;
		this.versionPatch = versionPatch;
		this.signature = signature;
	}

	public String getServiceName() {
		return Util.recoverString(this.serviceName);
	}

	public String getVersion() {
		return versionMajor + "." + versionMinor + "." + versionPatch;
	}

	@Override
	public String toString() {
		return "ServiceReleaseData(service name: " + this.getServiceName() + ", version: " + this.getVersion() + ")";
	}
}
