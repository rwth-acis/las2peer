package i5.las2peer.webConnector.services.classLoaderTest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("test")
public class TestResource {
	@GET
	@Path("/")
	public String getOk() {
		return "OK";
	}
}
