package i5.las2peer.webConnector;

import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.HttpHeaders;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * Service to test the web connector
 *
 */
@Path("service3")
public class TestService3 extends RESTService {

	/**
	 * constructor
	 */
	public TestService3() {

	}

	@GET
	@Path("/test1/{a}/{b}")
	public HttpResponse test1(@PathParam("a") String a, @PathParam("b") String b, @HeaderParam(
			value = "c") @DefaultValue("") String c, @HttpHeaders String headers) {
		String result = a + b + c;
		HttpResponse response = new HttpResponse(result, 200);
		response.setHeader("hi", "ho");
		response.setHeader("Content-Type", "text/plain");
		response.setHeader("Server-Name", "foo");
		return response;
	}

	@GET
	@Path("/test2/{a}/{b}")
	public HttpResponse test2() {
		String result = "5";
		HttpResponse response = new HttpResponse(result, 412);

		return response;
	}

}
