package i5.las2peer.security;

import i5.las2peer.logging.L2pLogger;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class DIDDocument {

	public static final String L2P_METHOD_NAME = "l2p";

	private static final L2pLogger logger = L2pLogger.getInstance(EthereumAgent.class);

	// las2peer's method name is "l2p".
	// This string is important to be able to resolve DIDs.
	private String methodName;

	// las2peer requires unique, immutable usernames.
	// These are used to generate a method specific ID.
	private String methodSpecificID;

	private List<DIDPublicKey> publicKeys;
	private List<DIDService> services;

	public DIDDocument(String username) {
		publicKeys = new ArrayList();
		services = new ArrayList();

		methodName = L2P_METHOD_NAME;
		methodSpecificID = username;
	}

	public String getDID() {
		return "did:" + this.methodName + ":" + this.methodSpecificID;
	}

	protected void setDID(String did) throws IllegalArgumentException {
		String[] tokens = did.split(":", 3);
		if (tokens.length != 2) {
			throw new IllegalArgumentException("Format of DID is wrong.");
		}

		String type = tokens[0];
		String method = tokens[1];
		String ID = tokens[2];

		if (!type.equals("did")) {
			throw new IllegalArgumentException("First token must be equal to \"DID\".");
		}

		if (!method.equals(L2P_METHOD_NAME)) {
			throw new IllegalArgumentException("Only las2peer DIDs are supported.");
		}

		this.methodName = method;
		this.methodSpecificID = ID;
	}

	public List<DIDPublicKey> getPublicKeys() {
		return this.publicKeys;
	}

	public DIDPublicKey getPublicKey(String name) throws NoSuchElementException {
		for (DIDPublicKey pk : publicKeys) {
			if (pk.getEncodedName().equals(name)) {
				return pk;
			}
		}
		throw new NoSuchElementException("There is no attribute with that name.");
	}

	protected void addPublicKey(DIDPublicKey publicKey) {
		this.publicKeys.add(publicKey);
	}

	protected void removePublicKey(DIDPublicKey publicKey) {
		// TODO(Julius): implement
	}

	public List<DIDService> getServices() {
		return this.services;
	}

	public DIDService getService(String name) throws NoSuchElementException {
		for (DIDService svc : services) {
			if (svc.getEncodedName().equals(name)) {
				return svc;
			}
		}
		throw new NoSuchElementException("There is no attribute with that name.");
	}

	protected void addService(DIDService service) {
		this.services.add(service);
	}

	protected void removeService(DIDService service) {
		// TODO(Julius): implement
	}

//	public String toJSONString() {
//		JSONObject json = new JSONObject();
//
//		json.put("@context", "https://w3id.org/did/v1");
//		json.put("id", getDID());
//
//		if (publicKeys.length > 0) {
//			JSONArray array = new JSONArray();
//			publicKeys.forEach(publicKey => {
//				array.add(publicKeys.toJSON);
//			});
//			json.put("publicKeys", array);
//		}
//		if (serviceEndpoints.length > 0) {
//			JSONArray array = new JSONArray();
//			services.forEach(service => {
//				array.add(services.toJSON());
//			});
//			json.put("services", array);
//		}
//
//		return json.toString();
//	}

	/**
	 * Parses an attribute change from the blockchain into their respective type then adding them to the document.
	 *
	 * @param name  The name of the attribute (contains metadata like key type and encoding)
	 * @param value The key or the service endpoint
	 * @throws IllegalArgumentException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	public void addAttribute(String name, byte[] value) throws IllegalArgumentException, NoSuchAlgorithmException, InvalidKeySpecException {
		if (!name.startsWith("did/")) {
			throw new IllegalArgumentException("DID attribute name must start with \"did/\".");
		}

		String[] tokens = name.split("/");
		if (tokens.length < 2) {
			throw new IllegalArgumentException("DID attribute name has wrong format.");
		}

		String attrType = tokens[1];
		switch (attrType) {
			case "pub":
				this.publicKeys.add(new DIDPublicKey(name, value));
				break;
			case "svc":
				this.services.add(new DIDService(name, value));
				break;
			default:
				logger.warning(String.format("Unknown DID attribute type \"%s\".", attrType));
				break;
		}
	}

	/**
	 * Removes an attribute from the DID document
	 *
	 * @param name  The name of the attribute (contains metadata like key type and encoding)
	 * @param value The key or the service endpoint
	 * @throws IllegalArgumentException
	 * @throws NoSuchElementException
	 */
	public void removeAttribute(String name, byte[] value) throws IllegalArgumentException, NoSuchElementException {
		if (!name.startsWith("did/")) {
			throw new IllegalArgumentException("DID attribute name must start with \"did/\".");
		}

		String[] tokens = name.split("/");
		if (tokens.length < 2) {
			throw new IllegalArgumentException("DID attribute name has wrong format.");
		}

		String attrType = tokens[1];
		switch (attrType) {
			case "pub":
				DIDPublicKey pk = getPublicKey(name);
				if (pk.getPublicKey().getEncoded().equals(value)) {
					publicKeys.remove(pk);
				}
				break;
			case "svc":
				DIDService svc = getService(name);
				if (svc.getServiceEndpoint().equals(new String(value, StandardCharsets.UTF_8))) {
					services.remove(svc);
				}
				break;
			default:
				logger.warning(String.format("Unknown DID attribute type \"%s\".", attrType));
				break;
		}
	}
}
