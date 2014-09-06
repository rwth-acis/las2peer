package i5.las2peer.webConnector;



import i5.las2peer.api.Service;

import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.*;
/**
 * Service to test the web connector
*
 *
 */
@Version("0.2")
public class TestService extends Service
{
	
	/**
	 * constructor, initializes RESTMapper
	 */
	public TestService()
	{
		
	}
	/**
	 * get all annotation and method data to allow mapping
	 */
	
	
	public String getRESTMapping()
	{
		String result="";
		try {
			result=RESTMapper.getMethodsAsXML(this.getClass());
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Simple test whether the login was successful
	 * @return
	 */
	@GET
	@Path("")
	public String login()
	{
		return "OK";
	}
	/**
	 * add two numbers
	 * @param num1
	 * @param num2
	 * @return num1+num2
	 */
	@PUT
	@Path("add/{number1}/{number2}")
	public int add(@PathParam("number1") int num1, @PathParam("number2") int num2)
	{
		return num1+num2;
	}
	/**
	 * subtract two numbers
	 * @param num1
	 * @param num2
	 * @return num1-num2
	 */
	@POST
	@Path("sub/{number1}/{number2}")
	public int subtract(@PathParam("number1") int num1, @PathParam("number2") int num2)
	{
		return num1-num2;
	}
	/**
	 * divides two numbers
	 * @param num1
	 * @param num2
	 * @return num1/num2
	 */
	@DELETE
	@Path("div/{number1}/{number2}")
	public int divide(@PathParam("number1") int num1, @PathParam("number2") int num2)
	{
		return num1/num2;
	}
	/**
	 * adds 4 values
	 * @param num1
	 * @param num2
	 * @param param1
	 * @param param2
	 * @return num1+num2+param1+param2
	 */
	@GET
	@Path("do/{number1}/it/{number2}")
	public int doIt(@PathParam("number1") int num1, @PathParam("number2") int num2,
			@QueryParam(name="param1",defaultValue = "0")int param1, @QueryParam(name="param2", defaultValue = "0") int param2 )
	{
		return num1+num2+param1+param2;
	}
	/**
	 * subtracts 2 values from the first one
	 * @param num1
	 * @param num2
	 * @param param1
	 * @param param2
	 * @return num1-num2-param1-param2
	 */
	@GET
	@Path("do/{number1}/it/{number2}/not")
	public int doItNot(@PathParam("number1") int num1, @PathParam("number2") int num2,
			@QueryParam(name="param1",defaultValue="0")int param1, @QueryParam(name="param2",defaultValue = "0") int param2 )
	{
		return num1-num2-param1-param2;
	}
	/**
	 * some computations
	 * @param num1
	 * @param num2
	 * @param param1
	 * @param param2
	 * @return num1*num2-param1*param2
	 */
	@GET
	@Path("do/{number1}/this/{number2}/not")
	public int doThisNot(@PathParam("number1") int num1, @PathParam("number2") int num2,
			@QueryParam(name="param1",defaultValue = "0")int param1, @QueryParam(name="param2",defaultValue = "0") int param2 )
	{
		return num1*num2-param1*param2;
	}

    @GET
    @Path("books/{id}/test")
    public int doubleMethod1()
    {
        return 1;
    }
	
	
	
	
}
