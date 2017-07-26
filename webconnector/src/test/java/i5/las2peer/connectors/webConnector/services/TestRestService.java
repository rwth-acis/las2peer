package i5.las2peer.connectors.webConnector.services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import i5.las2peer.restMapper.RESTService;

public class TestRestService extends RESTService {

	@GET
	@Path("/ok")
	public String getOk() {
		return "OK";
	}

}
