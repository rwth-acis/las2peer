package i5.las2peer.webConnector.services;

import i5.las2peer.execution.L2pThread;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import i5.las2peer.security.ServiceAgent;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

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
			ServiceAgent serviceAgent = ((L2pThread) Thread.currentThread()).getServiceAgent();
			return serviceAgent.getServiceNameVersion().getVersion().toString();
		}

		@GET
		@Path("/path")
		public String getPath(@Context UriInfo uriInfo) {
			return uriInfo.getBaseUri().getPath();
		}
	}
}
