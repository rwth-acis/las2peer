package i5.las2peer.connectors.webConnector.services;

import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import io.swagger.annotations.Api;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@ServicePath("swaggertest")
public class TestSwaggerService extends RESTService {

	@Override
	protected void initResources() {
		getResourceConfig().registerClasses(RootResource.class);

	}

	@Api
	@Path("/")
	public static class RootResource {
		@GET
		@Path("/")
		public String login() {
			return "OK";
		}

		@PUT
		@Path("/create/{id}")
		public Response createSomething(@PathParam("id") String id) {
			if (id.equals("notfound")) {
				return Response.status(Response.Status.NOT_FOUND).entity("Not found!").build();
			}
			String json = "{number: 1}";
			return Response.ok(json, MediaType.APPLICATION_JSON).build();
		}

		@Path("/subresource")
		public SubResource getSubResource() {
			return new SubResource("test");
		}
	}

	public static class SubResource {

		private String content;

		SubResource(String content) {
			this.content = content;
		}

		@GET
		@Path("content")
		public String getContent() {
			return content;
		}

	}

}
