package i5.las2peer.registry.data;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ServiceReleaseData that = (ServiceReleaseData) o;
		return Objects.equals(serviceName, that.serviceName) &&
				Objects.equals(versionMajor, that.versionMajor) &&
				Objects.equals(versionMinor, that.versionMinor) &&
				Objects.equals(versionPatch, that.versionPatch) &&
				Arrays.equals(signature, that.signature);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(serviceName, versionMajor, versionMinor, versionPatch);
		result = 31 * result + Arrays.hashCode(signature);
		return result;
	}
}
