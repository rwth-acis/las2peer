package i5.las2peer.connectors.webConnector.services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;

@ServicePath("UPPERCASE")
public class TestCapsService extends RESTService {

	@Override
	protected void initResources() {
		getResourceConfig().register(Resource.class);
	}

	@Path("/")
	public static class Resource {

		@GET
		@Path("/test")
		public String getVersion() {
			return "success";
		}
	}

}
