package i5.las2peer.restMapper;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import i5.las2peer.restMapper.annotations.ContentParam;

@Path("/books/{id}")
@Consumes("text/plain")
@Produces("text/plain")
public class TestClass4 {

	public TestClass4() {
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

	@PUT
	public String b4(@ContentParam String c) {
		return c;
	}

}
