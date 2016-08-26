package i5.las2peer.restMapper;

import i5.las2peer.restMapper.annotations.HttpHeaders;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("service2")
public class TestClass2 {

	public TestClass2() {
	}

	@PUT
	@Path("/visitors/{userid}")
	public float b1(@PathParam("userid") int userID) {
		return userID * 0.2f;
	}

	@DELETE
	@Path("/users/{userid}/cart/{productID}/{price}")
	public String b2(@PathParam("userid") int userID, @PathParam("productID") short productID,
			@PathParam("price") float price) {
		return userID * productID + "" + price + "";
	}

	@DELETE
	@Path("/users/{a}")
	public String b3(@PathParam("a") int userID, @HeaderParam(
			value = "productID") @DefaultValue(
			value = "0") short productID, @HeaderParam(
			value = "price") @DefaultValue(
			value = "0") float price) {
		return userID * productID + "" + price + "";
	}

	@DELETE
	@Path("/users/{a}/{b}")
	public String b4(@PathParam("a") int userID, @PathParam("b") int paramB, @HttpHeaders String headers) {
		return userID + headers;
	}

}
