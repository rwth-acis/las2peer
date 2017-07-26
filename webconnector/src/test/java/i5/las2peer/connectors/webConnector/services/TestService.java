package i5.las2peer.connectors.webConnector.services;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;

@ServicePath("test")
public class TestService extends RESTService {

	@Override
	protected void initResources() {
		getResourceConfig().register(Resource.class);
	}

	@Path("/")
	public static class Resource {

		@GET
		@Path("/ok")
		public String getOk() {
			return "OK";
		}

		@GET
		@Path("/exception")
		public String getException() {
			throw new NullPointerException();
		}

		@GET
		@Path("/empty")
		public Response getEmptyResponse() {
			return Response.ok().build();
		}

		@GET
		@Path("/headers")
		public String getHeaders(@Context HttpHeaders headers) {
			String response = "";

			for (String key : headers.getRequestHeaders().keySet()) {
				response += key + " = ";
				for (String value : headers.getRequestHeader(key)) {
					response += value + ", ";
				}
				response += "\n";
			}

			return response;
		}

		@GET
		@Path("/requesturi")
		public String getRequestUri(@Context UriInfo uriInfo) {
			return uriInfo.getRequestUri().toString();
		}

		@GET
		@Path("/encoding")
		@Produces("text/plain;charset=utf-8")
		public String getSmiley() {
			return "☺";
		}

		@POST
		@Path("/body")
		@Consumes("text/plain")
		public String getBody(String body) {
			return body;
		}

	}

}
