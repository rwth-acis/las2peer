package i5.las2peer.connectors.webConnector.services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import i5.las2peer.api.security.ServiceAgent;
import i5.las2peer.execution.ExecutionContext;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;

@ServicePath("version")
public class TestVersionService extends RESTService {

	@Override
	protected void initResources() {
		getResourceConfig().register(Resource.class);
	}

	@Path("/")
	public static class Resource {

		@GET
		@Path("/test")
		public String getVersion() {
			ServiceAgent serviceAgent = ExecutionContext.getCurrent().getServiceAgent();
			return serviceAgent.getServiceNameVersion().getVersion().toString();
		}

		@GET
		@Path("/path")
		public String getPath(@Context UriInfo uriInfo) {
			return uriInfo.getBaseUri().getPath();
		}
	}

}
