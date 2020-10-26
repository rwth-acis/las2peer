package i5.las2peer.connectors.webConnector.handler;

import i5.las2peer.connectors.webConnector.WebConnector;
import i5.las2peer.p2p.EthereumNode;
import i5.las2peer.p2p.Node;
import i5.las2peer.security.DIDDocument;
import i5.las2peer.security.DIDPublicKey;
import i5.las2peer.security.DIDService;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;
import java.util.NoSuchElementException;

@Path(DIDHandler.RESOURCE_PATH)
public class DIDHandler {
	public static final String RESOURCE_PATH = DefaultHandler.ROOT_RESOURCE_PATH + "/did";

	private final WebConnector connector;
	private final EthereumNode node;

	public DIDHandler(WebConnector connector) {
		this.connector = connector;
		Node node = connector.getL2pNode();
		this.node = (node instanceof EthereumNode) ? (EthereumNode) node : null;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/documents/{userName}")
	public Response handleGetDocument(@PathParam("userName") String userName) {
		if (node == null) {
			return Response.status(Status.NOT_IMPLEMENTED).entity("This feature requires an Ethereum-enabled node.").build();
		}

		DIDDocument doc = null;
		try {
			doc = node.getRegistryClient().getDIDDocument(userName);
		} catch (NoSuchElementException e) {
			return Response.status(Status.NOT_FOUND).entity(e.toString()).type(MediaType.TEXT_PLAIN_TYPE).build();
		}
		List<DIDPublicKey> publicKeys = doc.getPublicKeys();
		List<DIDService> services = doc.getServices();

		JSONObject json = new JSONObject();

		json.put("@context", "https://w3id.org/did/v1");
		json.put("id", doc.getDID());

		if (publicKeys.size() > 0) {
			JSONArray array = new JSONArray();
			for(int i = 0; i < publicKeys.size(); i++) {
				JSONObject pk = new JSONObject();
				pk.put("id", String.format("%s#publicKey-%d", doc.getDID(), i+1));
				pk.put("type", "RsaVerificationKey2018");
				pk.put("controller", doc.getDID());
				pk.put("publicKeyBase64", publicKeys.get(i).getEncodedValue());
			}
			json.put("publicKeys", array);
		}
		if (services.size() > 0) {
			JSONArray array = new JSONArray();
			for(int i = 0; i < services.size(); i++) {
				JSONObject svc = new JSONObject();
				svc.put("id", String.format("%s#service-%d", doc.getDID(), i+1));
				svc.put("type", "OpenIdConnectVersion1.0Service");
				svc.put("serviceEndpoint", services.get(i).getServiceEndpoint());
			}
			json.put("services", array);
		}

		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}
}
