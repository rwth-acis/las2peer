package i5.las2peer.connectors.webConnector.services;

import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

@ServicePath("deep/path")
public class TestDeepPathService extends RESTService {
	@Override
	protected void initResources() {
		getResourceConfig().register(Resource.class);
	}

	@Path("/")
	public static class Resource {
		@GET
		@Path("/")
		public String getPath(@Context UriInfo uriInfo) {
			return uriInfo.getBaseUri().getPath();
		}

		@GET
		@Path("/test")
		public String getPath2(@Context UriInfo uriInfo) {
			return uriInfo.getBaseUri().getPath();
		}
	}
}
