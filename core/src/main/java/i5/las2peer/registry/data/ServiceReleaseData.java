package i5.las2peer.registry.data;

import i5.las2peer.registry.Util;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

public class ServiceReleaseData {
	private String serviceName;
	private BigInteger versionMajor;
	private BigInteger versionMinor;
	private BigInteger versionPatch;
	private byte[] supplementHash;

	public ServiceReleaseData(String serviceName, BigInteger versionMajor, BigInteger versionMinor, BigInteger versionPatch, byte[] supplementHash) {
		this.serviceName = serviceName;
		this.versionMajor = versionMajor;
		this.versionMinor = versionMinor;
		this.versionPatch = versionPatch;
		this.supplementHash = supplementHash;
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

	public byte[] getSupplementHash() {
		return supplementHash;
	}

	@Override
	public String toString() {
		return "ServiceReleaseData(service name: " + getServiceName() + ", version: " + getVersion()
				+ "; supp. hash: " + Util.bytesToHexString(getSupplementHash()) + ")";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ServiceReleaseData that = (ServiceReleaseData) o;
		return Objects.equals(serviceName, that.serviceName) &&
				Objects.equals(versionMajor, that.versionMajor) &&
				Objects.equals(versionMinor, that.versionMinor) &&
				Objects.equals(versionPatch, that.versionPatch) &&
				Arrays.equals(supplementHash, that.supplementHash);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(serviceName, versionMajor, versionMinor, versionPatch);
		result = 31 * result + Arrays.hashCode(supplementHash);
		return result;
	}
}
