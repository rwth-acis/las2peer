package i5.las2peer.restMapper.services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import io.swagger.annotations.Api;

@ServicePath("service3")
@Api
public class TestResourceService extends RESTService {
	@GET
	@Path("hello")
	@Produces("text/plain")
	public String getHello() {
		return "Hello World!";
	}
}
