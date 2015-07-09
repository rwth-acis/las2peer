package i5.las2peer.restMapper;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("/books/{id}{{}}")
@Consumes("text/plain")
@Produces("text/plain")
public class TestClassBad2 {

	public TestClassBad2() {
	}

	@GET
	public int b1(@PathParam("ida") int id) {
		return id;
	}

	@GET
	@Produces(MediaType.AUDIO_OGG)
	public int b2(@PathParam("id") int id) {
		return id * 2;
	}

	@GET
	@Produces(MediaType.AUDIO_MP4)
	public int b3(@PathParam("id") int id) {
		return id * 3;
	}

}
