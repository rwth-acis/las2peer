package i5.las2peer.restMapper;

import static org.junit.Assert.*;
import i5.las2peer.restMapper.annotations.*;

import org.junit.BeforeClass;
import org.junit.Test;

public class RESTMapperTest {

	public class TestClass1
	{
		@GET
		@Path("")
		public boolean a1()
		{
			return true;
		}
		
		@PUT
		@Path("/users/{userid}")
		public float a2(@PathParam("userid")int userID)
		{
			return userID*0.1f;			
		}
		
		@DELETE
		@Path("users/{userid}/products/{productID}/{likes}")
		public String a3(@PathParam("userid") int userID, @PathParam("productID") short productID,  @PathParam("likes") boolean likes)
		{
			return userID*productID+""+likes+"";			
		}
		
		@POST
		@Path("{a}/b/c")
		public String a4(@PathParam("a") String a, @DefaultValue("5") @QueryParam("d") int d, @DefaultValue("19") @QueryParam("e") int e)
		{
			return a+(d+e);			
		}
		
		@GET
		@Path("a")
		public String a5(@ContentParam() String a)
		{
			return a+a;			
		}
		
		@PUT
		@Path("{a}/{b}/{c}/{d}")
		public String a6(@PathParam("a") String a,@PathParam("b") String b,@PathParam("c") String c,@PathParam("d") int d)
		{
			return a+b+c+d;			
		}
		
	}
	TestClass1 testClass1= new TestClass1();
	static RESTMapper mapper;
	@BeforeClass
	public static void testSetup() 
	{			
		mapper= new RESTMapper(TestClass1.class);
	}
	
	@Test
	public void testA1(){
		try	{
			String result=mapper.parse(testClass1, "get", "", new String[][]{},"");
			assertEquals("a1","true",result);
		}
		catch (Throwable e)	{
			fail(e.getMessage());
		}
	}
	@Test
	public void testA2(){
		try	{
			String result=mapper.parse(testClass1, "put", "/users/205", new String[][]{},"");
			assertEquals("a2","20.5",result);
		}
		catch (Throwable e)	{
			fail(e.getMessage());
		}
	}
	@Test
	public void testA3(){
		try	{
			String result=mapper.parse(testClass1, "delete", "/users/12/products/5/true", new String[][]{},"");
			assertEquals("a3","60true",result);
		}
		catch (Throwable e)	{
			fail(e.getMessage());
		}
	}
	@Test
	public void testA4(){
		try	{
			String result=mapper.parse(testClass1, "post", "hi/b/c", new String[][]{{"d","12"},{"e","4"}},"");
			assertEquals("a4","hi16",result);
		}
		catch (Throwable e)	{
			
		}
	}
	@Test
	public void testA4_default(){
		try	{
			String result=mapper.parse(testClass1, "post", "hi/b/c", new String[][]{{"e","4"}},"");
			assertEquals("a4","hi9",result);
		}
		catch (Throwable e)	{
			
		}
	}
	@Test
	public void testA4_ex(){
		try	{
			mapper.parse(testClass1, "post", "hi/b/", new String[][]{{"d","12"},{"e","4"}},"");
			fail("Wrong path, no exception");
		}
		catch (Throwable e)	{
			//e.printStackTrace();
		}
	}
	@Test
	public void testA5(){
		try	{
			String result=mapper.parse(testClass1, "get", "a", new String[][]{},"t");
			assertEquals("a5","tt",result);
		}
		catch (Throwable e)	{
			fail(e.getMessage());
		}
	}
	@Test
	public void testA6(){
		try	{
			String result=mapper.parse(testClass1, "put", "d/c/b/1", new String[][]{},"");
			assertEquals("a6","dcb1",result);
		}
		catch (Throwable e)	{
			fail(e.getMessage());
		}
	}

}
