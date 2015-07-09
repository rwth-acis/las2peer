package i5.las2peer.restMapper;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import i5.las2peer.restMapper.annotations.ContentParam;

public class TestClass1
{

	public TestClass1()
	{
	}

	@GET
	@Path("/")
	public boolean a1()
	{
		return true;
	}

	@PUT
	@Path("/users/{userid}")
	public float a2(@PathParam("userid") int userID)
	{
		return userID * 0.1f;
	}

	@DELETE
	@Path("/users/{userid}/products/{productID}/{likes}")
	public String a3(@PathParam("userid") int userID, @PathParam("productID") short productID,
			@PathParam("likes") boolean likes)
	{
		return userID * productID + "" + likes + "";
	}

	@POST
	@Path("/{a}/b/c")
	public String a4(@PathParam("a") String a, @DefaultValue("5") @QueryParam(value = "d") int d,
			@QueryParam(value = "e") @DefaultValue(value = "19") int e)
	{
		return a + (d + e);
	}

	@GET
	@Path("/a")
	public String a5(@ContentParam() String a)
	{
		return a + a;
	}

	@PUT
	@Path("/{a}/{b}/{c}/{d}")
	public String a6(@PathParam("a") String a, @PathParam("b") String b, @PathParam("c") String c, @PathParam("d") int d)
	{
		return a + b + c + d;
	}

	@PUT
	@Path("/asdasfafda")
	public String a7() // throws Exception
	{
		return "1";
	}

}
