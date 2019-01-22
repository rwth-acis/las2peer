package i5.las2peer.registry.data;

import i5.las2peer.registry.Util;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

public class ServiceReleaseData {
	private String serviceName;
	private BigInteger versionMajor;
	private BigInteger versionMinor;
	private BigInteger versionPatch;
	private byte[] supplementHash;
	private BigInteger timestamp;

	public ServiceReleaseData(String serviceName, BigInteger versionMajor, BigInteger versionMinor, BigInteger versionPatch, byte[] supplementHash, BigInteger timestamp) {
		this.serviceName = serviceName;
		this.versionMajor = versionMajor;
		this.versionMinor = versionMinor;
		this.versionPatch = versionPatch;
		this.supplementHash = supplementHash;
		this.timestamp = timestamp;
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

	public Instant getTimestamp() {
		return Instant.ofEpochSecond(timestamp.longValue());
	}

	@Override
	public String toString() {
		return "ServiceReleaseData(service name: " + getServiceName() + ", version: " + getVersion()
				+ "; supp. hash: " + Util.bytesToHexString(getSupplementHash()) + "; time " + getTimestamp() + ")";
	}

	// TODO: where is this used, and what equality semantics do we need there?
	// probably does not matter much, there shouldn't ever be duplicates anyway (?)
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ServiceReleaseData that = (ServiceReleaseData) o;
		return Objects.equals(serviceName, that.serviceName) &&
				Objects.equals(versionMajor, that.versionMajor) &&
				Objects.equals(versionMinor, that.versionMinor) &&
				Objects.equals(versionPatch, that.versionPatch) &&
				Arrays.equals(supplementHash, that.supplementHash) &&
				Objects.equals(timestamp, that.timestamp);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(serviceName, versionMajor, versionMinor, versionPatch, timestamp);
		result = 31 * result + Arrays.hashCode(supplementHash);
		return result;
	}
}
