package i5.las2peer.webConnector;

import i5.las2peer.api.Service;
import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.restMapper.MediaType;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.ContentParam;
import i5.las2peer.restMapper.annotations.Version;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * Service to test the web connector
 *
 */
@Version("0.2")
@Path("/books/{id}")
@Consumes(MediaType.TEXT_PLAIN)
@Produces(MediaType.TEXT_PLAIN)
public class TestService4 extends Service {

	/**
	 * constructor
	 */
	public TestService4() {

	}

	/**
	 * get all annotation and method data to allow mapping
	 * 
	 * @return
	 */

	public String getRESTMapping() {
		String result = "";
		try {
			result = RESTMapper.getMethodsAsXML(this.getClass());
		} catch (Exception e) {

			e.printStackTrace();
		}
		return result;
	}

	@POST
	public int a1(@PathParam("id") int id) {
		return id;
	}

	@POST
	@Consumes({ "audio/ogg", "audio/mpeg" })
	public int a2(@PathParam("id") int id) {
		return id * 7;
	}

	@GET
	public int b1(@PathParam("id") int id) {
		return id;
	}

	@GET
	@Produces("audio/ogg")
	public int b2(@PathParam("id") int id) {
		return id * 2;
	}

	@GET
	@Produces("audio/mp4")
	public int b3(@PathParam("id") int id) {
		return id * 3;
	}

	@GET
	@Path("/test")
	public int doubleMethod1() {
		return 1;
	}

	@PUT
	@Path("/test2")
	public String putTest(@ContentParam String param) {
		return param;
	}

	@GET
	@Path("/test3")
	public HttpResponse errorTest() {
		HttpResponse response = new HttpResponse("hi", 500);
		return response;
	}

}