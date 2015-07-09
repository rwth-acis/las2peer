package i5.las2peer.restMapper;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import io.swagger.jaxrs.Reader;
import io.swagger.models.Swagger;
import io.swagger.util.Json;

import org.junit.Test;

public class RESTMapperSwaggerTest {

	/**
	 * This testcase uses a Swagger annotated class to check if the annotations can get
	 * parsed and printed as JSON String correctly.
	 */
	@Test
	public void testGetSwaggerJSON() {
		try {
			Swagger swagger = new Reader(new Swagger()).read(SwaggerAnnotatedService.class);
			assertNotNull(swagger);
			String result = Json.mapper().writeValueAsString(swagger);
			System.out.println(result);
			assertNotNull(result);
			assertTrue(result.length() > 0);
		} catch (Exception e) {
			e.printStackTrace();
			fail("failed to extract Swagger Resource Listing. Cause: " + e.getMessage());
		}
	}

}
