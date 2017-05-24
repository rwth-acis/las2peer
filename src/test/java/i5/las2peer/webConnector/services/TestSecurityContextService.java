package i5.las2peer.webConnector.services;

import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;

import java.security.Principal;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

@ServicePath("security")
public class TestSecurityContextService extends RESTService {

	@Override
	protected void initResources() {
		getResourceConfig().register(Resource.class);
		getResourceConfig().register(RolesAllowedDynamicFeature.class);
	}

	@Path("/")
	@PermitAll
	public static class Resource {

		@GET
		@Path("/name")
		@Produces(MediaType.TEXT_PLAIN)
		public Response getLogin(@Context SecurityContext context) {
			Principal principal = context.getUserPrincipal();
			if (principal == null) {
				return Response.status(403).entity("no principal").build();
			}
			return Response.ok().entity(principal.getName()).build();
		}

		@GET
		@Path("/anonymous")
		public String getUnauthenticated() {
			return "OK";
		}

		@RolesAllowed("authenticated")
		@GET
		@Path("/authenticated")
		public String getAuthenticated() {
			return "OK";
		}

		@RolesAllowed("authenticated_bot")
		@GET
		@Path("/bot")
		public String getAuthenticatedBot() {
			return "OK";
		}

	}

}
