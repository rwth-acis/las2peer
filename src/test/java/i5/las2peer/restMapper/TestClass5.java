package i5.las2peer.restMapper;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("/books/{id}")
@Consumes("text/plain")
@Produces("text/plain")
public class TestClass5
{

	public TestClass5()
	{
	}

	@GET
	public int b1(@PathParam("id") int id)
	{
		return id;
	}

}
