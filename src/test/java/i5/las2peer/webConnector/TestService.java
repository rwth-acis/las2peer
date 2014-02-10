package i5.las2peer.webConnector;


import i5.las2peer.api.Service;

import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.*;
/**
 * Service to test the web connector
 * @author Alexander
 *
 */
public class TestService extends Service
{
	private RESTMapper mapper;
	/**
	 * constructor, initializes RESTMapper
	 */
	public TestService()
	{
		initMapper();		
	}
	/**
	 * get all annotation and method data to allow mapping
	 */
	private void initMapper() 
	{
		mapper=new RESTMapper(this.getClass());
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
			@QueryParam("param1")int param1, @QueryParam("param2") int param2 )
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
			@QueryParam("param1")int param1, @QueryParam("param2") int param2 )
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
			@QueryParam("param1")int param1, @QueryParam("param2") int param2 )
	{
		return num1*num2-param1*param2;
	}
	/**
	 * concats 3 strings
	 * @param a
	 * @param b
	 * @param c
	 * @return abc
	 */
	@POST
	@Path("do/{a}/{b}")
	public String concat(@PathParam("a") String a, @PathParam("b") String b,
			@ContentParam String c )
	{
		return a+b+c;
	}
	
	/**
	 * 'Master' method: retrieves all requests from the web connector and maps them via the RESTMapper onto service methods
	 * @param method HTTP method
	 * @param URI URI path
	 * @param variables variables of the query
	 * @param content content of the HTML body (for POST requests)
	 * @return values/errors for the web connector
	 */
	public String restDecoder(String method, String URI, String[][] variables, String content)
	{
		String response="";
		try 
		{
			
			response=mapper.parse(this, method.toLowerCase(), URI,variables,content);//here the mapping magic happens
		} 
		catch (Throwable e) 
		{			
			response="Error: "+e.getClass().getName()+" "+e.getMessage();//+" "+errors;
		}
		return response;
	}
	
}
