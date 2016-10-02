package i5.las2peer.restMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import i5.las2peer.restMapper.services.TestCustomApplicationService;
import i5.las2peer.restMapper.services.TestService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

public class RESTServiceTest {
	RESTService testee;
	RESTService testeeCustomApplication;

	@Before
	public void setup() {
		this.testee = new TestService();
		this.testeeCustomApplication = new TestCustomApplicationService();
	}

	@Test
	public void testAlias() {
		assertEquals("service1", testee.getAlias());
	}

	private RESTResponse invoke(final RESTService service, final String method, final String relativePath,
			final String body) throws URISyntaxException {

		String basePath = "https://localhost:8080/" + service.getAlias() + "/";
		return testee.handle(new URI(basePath), new URI(basePath + relativePath), method, body.getBytes(),
				new HashMap<String, List<String>>());
	}

	@Test
	public void testHandle() throws URISyntaxException {
		RESTResponse response = invoke(testee, "GET", "hello", "");

		assertEquals(200, response.getHttpCode());
		assertEquals("Hello World!", new String(response.getBody()));
	}

	@Test
	public void testSwagger() throws JsonProcessingException {
		String response = testee.getSwagger();
		assertTrue(response.contains("getHello"));
	}

	@Test
	public void testPath() throws URISyntaxException {
		RESTResponse response = invoke(testee, "GET", "uri/", "");

		assertEquals(200, response.getHttpCode());
		assertEquals("https://localhost:8080/service1/uri/", new String(response.getBody()));
	}

	@Test
	public void testCustomApplication() throws URISyntaxException {
		RESTResponse response = invoke(testeeCustomApplication, "GET", "hello", "");

		assertEquals(200, response.getHttpCode());
		assertEquals("Hello World!", new String(response.getBody()));
	}

	@Test
	public void testJson() throws URISyntaxException {
		RESTResponse response = invoke(testee, "GET", "json", "");

		assertEquals(200, response.getHttpCode());
		assertTrue(new String(response.getBody()).contains("myTestBean"));
	}

}
