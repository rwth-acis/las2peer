package i5.las2peer.connectors.nodeAdminConnector.services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import i5.las2peer.restMapper.RESTService;

public class TestService extends RESTService {

	@GET
	@Path("/ok")
	public String getOk() {
		return "OK";
	}

}
