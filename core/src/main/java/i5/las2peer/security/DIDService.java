package i5.las2peer.security;

import java.nio.charset.StandardCharsets;

public class DIDService {
	private String type;
	private String serviceEndpoint;

	protected DIDService(String type, String serviceEndpoint) {
		this.type = type;
		this.serviceEndpoint = serviceEndpoint;
	}

	protected DIDService(String name, byte[] value) {
		String[] tokens = name.split("/", 3);
		if (tokens.length != 5 || !tokens[0].equals("did") || !tokens[1].equals("svc")) {
			throw new IllegalArgumentException("Input not in correct format.");
		}
		String type = tokens[2];

		this.type = type;
		this.serviceEndpoint = new String(value, StandardCharsets.UTF_8);
	}

	public String getType() {
		return type;
	}

	public String getServiceEndpoint() {
		return serviceEndpoint;
	}

	public void setServiceEndpoint(String serviceEndpoint) {
		this.serviceEndpoint = serviceEndpoint;
	}

	public String getEncodedName() {
		return String.format("did/svc/%s", getType());
	}

	public byte[] getEncodedValue() {
		return serviceEndpoint.getBytes();
	}
}
