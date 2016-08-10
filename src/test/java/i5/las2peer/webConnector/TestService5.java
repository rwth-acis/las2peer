package i5.las2peer.webConnector;

import i5.las2peer.restMapper.RESTService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * Service to test the web connector
 *
 */
@Path("/exception")
public class TestService5 extends RESTService {

	/**
	 * constructor
	 */
	public TestService5() {

	}

	@GET
	public int b4() {
		throw new IllegalArgumentException();
	}

}