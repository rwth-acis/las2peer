package i5.las2peer.webConnector;

import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ContentParam;
import i5.las2peer.security.Agent;
import i5.las2peer.security.UserAgent;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * Service to test the web connector
 *
 */
public class TestServiceMissingPath extends RESTService {

	/**
	 * constructor, initializes RESTMapper
	 */
	public TestServiceMissingPath() {

	}

	@POST
	@Path("/do/{a}/{b}")
	public String concat(@PathParam("a") String a, @PathParam("b") String b, @ContentParam String c) {
		return a + b + c;
	}

	@GET
	@Path("/userinfo")
	public String getUserInfo() {
		Agent agent = getContext().getMainAgent();
		if (agent instanceof UserAgent) {
			UserAgent u = (UserAgent) agent;
			return "{'id':'" + u.getId() + ", 'name':'" + u.getLoginName() + "','email':'" + u.getEmail() + "'}";
		} else {
			return "Shit happened...";
		}
	}

}
