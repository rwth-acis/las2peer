package i5.las2peer.restMapper;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.jaxrs.Reader;
import io.swagger.models.Swagger;
import io.swagger.util.Json;

import java.net.HttpURLConnection;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;

@Path("/example")
@Api
@SwaggerDefinition(
		info = @Info(
				title = "las2peer Template Service",
				version = "0.1",
				description = "A las2peer Template Service for demonstration purposes.",
				termsOfService = "http://your-terms-of-service-url.com",
				contact = @Contact(
						name = "John Doe",
						url = "provider.com",
						email = "john.doe@provider.com"),
				license = @License(
						name = "your software license name",
						url = "http://your-software-license-url.com")))
public class SwaggerAnnotatedService {

	public SwaggerAnnotatedService() {
		// constructor stub
	}

	// //////////////////////////////////////////////////////////////////////////////////////
	// Service methods.
	// //////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Simple function to validate a user login. Basically it only serves as a "calling point" and does not really
	 * validate a user (since this is done previously by las2peer itself, the user does not reach this method if he or
	 * she is not authenticated).
	 * 
	 */
	@GET
	@Path("/validation")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "Validation Confirmation"), @ApiResponse(
					code = HttpURLConnection.HTTP_UNAUTHORIZED,
					message = "Unauthorized") })
	@ApiOperation(
			value = "User Validation",
			notes = "Simple function to validate a user login.")
	public HttpResponse validateLogin() {
		// annotated method stub
		return null;
	}

	/**
	 * Example method that returns a phrase containing the received input.
	 * 
	 * @param myInput
	 * 
	 */
	@POST
	@Path("/myResourcePath/{input}")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiOperation(
			value = "Sample Resource",
			notes = "Example method that returns a phrase containing the received input.")
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "Input Phrase"), @ApiResponse(
					code = HttpURLConnection.HTTP_UNAUTHORIZED,
					message = "Unauthorized") })
	public HttpResponse exampleMethod(@PathParam("input") String myInput) {
		// annotated method stub
		return null;
	}

	/**
	 * Example method that shows how to retrieve a user email address from a database and return an HTTP response
	 * including a JSON object.
	 * 
	 * WARNING: THIS METHOD IS ONLY FOR DEMONSTRATIONAL PURPOSES!!! IT WILL REQUIRE RESPECTIVE DATABASE TABLES IN THE
	 * BACKEND, WHICH DON'T EXIST IN THE TEMPLATE.
	 * 
	 */
	@GET
	@Path("/userEmail/{username}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(
			value = "Email Address Administration",
			notes = "Example method that retrieves a user email address from a database."
					+ " WARNING: THIS METHOD IS ONLY FOR DEMONSTRATIONAL PURPOSES!!! "
					+ "IT WILL REQUIRE RESPECTIVE DATABASE TABLES IN THE BACKEND, WHICH DON'T EXIST IN THE TEMPLATE.")
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "User Email"), @ApiResponse(
					code = HttpURLConnection.HTTP_UNAUTHORIZED,
					message = "Unauthorized"), @ApiResponse(
					code = HttpURLConnection.HTTP_NOT_FOUND,
					message = "User not found"), @ApiResponse(
					code = 500,
					message = "Internal Server Error") })
	public HttpResponse getUserEmail(@PathParam("username") String username) {
		// annotated method stub
		return null;
	}

	/**
	 * Example method that shows how to change a user email address in a database.
	 * 
	 * WARNING: THIS METHOD IS ONLY FOR DEMONSTRATIONAL PURPOSES!!! IT WILL REQUIRE RESPECTIVE DATABASE TABLES IN THE
	 * BACKEND, WHICH DON'T EXIST IN THE TEMPLATE.
	 * 
	 */
	@POST
	@Path("/userEmail/{username}/{email}")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiOperation(
			value = "Email Address Administration",
			notes = "Example method that changes a user email address in a database."
					+ " WARNING: THIS METHOD IS ONLY FOR DEMONSTRATIONAL PURPOSES!!! "
					+ "IT WILL REQUIRE RESPECTIVE DATABASE TABLES IN THE BACKEND, WHICH DON'T EXIST IN THE TEMPLATE.")
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "Update Confirmation"), @ApiResponse(
					code = HttpURLConnection.HTTP_UNAUTHORIZED,
					message = "Unauthorized"), @ApiResponse(
					code = 500,
					message = "Internal Server Error") })
	public HttpResponse setUserEmail(@PathParam("username") String username, @PathParam("email") String email) {
		// annotated method stub
		return null;
	}

	// //////////////////////////////////////////////////////////////////////////////////////
	// Methods providing a Swagger documentation of the service API.
	// //////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the API documentation for this annotated class for purposes of the Swagger 2.0 documentation.
	 * 
	 * Note: If you do not intend to use Swagger for the documentation of your Service API, this method may be removed.
	 * 
	 * @return The resource's documentation.
	 */
	@GET
	@Path("/swagger.json")
	@Produces(MediaType.APPLICATION_JSON)
	public HttpResponse getSwaggerJSON() {
		Swagger swagger = new Reader(new Swagger()).read(this.getClass());
		if (swagger == null) {
			return new HttpResponse("Swagger API declaration not available!", HttpURLConnection.HTTP_NOT_FOUND);
		}
		try {
			return new HttpResponse(Json.mapper().writeValueAsString(swagger), HttpURLConnection.HTTP_OK);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return new HttpResponse(e.getMessage(), 500);
		}
	}

}
