package i5.las2peer.webConnector;

import i5.las2peer.restMapper.RESTService;
import io.swagger.annotations.Api;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

/**
 * Service to test the web connector
 *
 */
@Api
@Path("service1")
public class TestService extends RESTService {

	/**
	 * constructor, initializes RESTMapper
	 */
	public TestService() {

	}

	/**
	 * Simple test whether the login was successful
	 * 
	 * @return
	 */
	@GET
	@Path("/")
	public String login() {
		return "OK";
	}

	/**
	 * add two numbers
	 * 
	 * @param num1
	 * @param num2
	 * @return num1+num2
	 */
	@PUT
	@Path("/add/{number1}/{number2}")
	public int add(@PathParam("number1") int num1, @PathParam("number2") int num2) {
		return num1 + num2;
	}

	/**
	 * subtract two numbers
	 * 
	 * @param num1
	 * @param num2
	 * @return num1-num2
	 */
	@POST
	@Path("/sub/{number1}/{number2}")
	public int subtract(@PathParam("number1") int num1, @PathParam("number2") int num2) {
		return num1 - num2;
	}

	/**
	 * divides two numbers
	 * 
	 * @param num1
	 * @param num2
	 * @return num1/num2
	 */
	@DELETE
	@Path("/div/{number1}/{number2}")
	public int divide(@PathParam("number1") int num1, @PathParam("number2") int num2) {
		return num1 / num2;
	}

	/**
	 * adds 4 values
	 * 
	 * @param num1
	 * @param num2
	 * @param param1
	 * @param param2
	 * @return num1+num2+param1+param2
	 */
	@GET
	@Path("/do/{number1}/it/{number2}")
	public int doIt(@PathParam("number1") int num1, @PathParam("number2") int num2, @QueryParam(
			value = "param1") @DefaultValue("0") int param1, @QueryParam(
			value = "param2") @DefaultValue("0") int param2) {
		return num1 + num2 + param1 + param2;
	}

	/**
	 * subtracts 2 values from the first one
	 * 
	 * @param num1
	 * @param num2
	 * @param param1
	 * @param param2
	 * @return num1-num2-param1-param2
	 */
	@GET
	@Path("/do/{number1}/it/{number2}/not")
	public int doItNot(@PathParam("number1") int num1, @PathParam("number2") int num2, @QueryParam(
			value = "param1") @DefaultValue("0") int param1, @QueryParam(
			value = "param2") @DefaultValue("0") int param2) {
		return num1 - num2 - param1 - param2;
	}

	/**
	 * some computations
	 * 
	 * @param num1
	 * @param num2
	 * @param param1
	 * @param param2
	 * @return num1*num2-param1*param2
	 */
	@GET
	@Path("/do/{number1}/this/{number2}/not")
	public int doThisNot(@PathParam("number1") int num1, @PathParam("number2") int num2, @QueryParam(
			value = "param1") @DefaultValue("0") int param1, @QueryParam(
			value = "param2") @DefaultValue("0") int param2) {
		return num1 * num2 - param1 * param2;
	}

	@GET
	@Path("/books/{id}/test")
	public int doubleMethod1() {
		return 1;
	}

}
