package i5.las2peer.connectors.webConnector.services.classLoaderTest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("test")
public class TestResource {
	@GET
	public String getOk() {
		return "OK";
	}
}
