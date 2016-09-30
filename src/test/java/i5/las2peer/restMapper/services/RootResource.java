package i5.las2peer.restMapper.services;

import io.swagger.annotations.Api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

@Api
@Path("/")
public class RootResource {
	@GET
	@Path("hello")
	@Produces("text/plain")
	public String getHello() {
		return "Hello World!";
	}

	@GET
	@Path("uri")
	@Produces("text/plain")
	public String getUri(@Context UriInfo ui) {
		return ui.getAbsolutePath().toString();
	}
}
