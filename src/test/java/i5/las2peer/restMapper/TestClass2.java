package i5.las2peer.restMapper;

import i5.las2peer.restMapper.annotations.*;

public class TestClass2 {

	public TestClass2()
	{
		
	}
	
	@PUT
	@Path("/visitors/{userid}")
	public float b1(@PathParam("userid")int userID)
	{
		return userID*0.2f;			
	}
	
	@DELETE
	@Path("users/{userid}/cart/{productID}/{price}")
	public String b2(@PathParam("userid") int userID, @PathParam("productID") short productID,  @PathParam("price") float price)
	{
		return userID*productID+""+price+"";			
	}
}
