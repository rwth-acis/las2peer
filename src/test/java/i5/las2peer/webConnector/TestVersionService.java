package i5.las2peer.webConnector;

import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.restMapper.RESTService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/version")
public class TestVersionService extends RESTService {

	@GET
	@Path("/test")
	public String getVersion() {
		try {
			return getAgent().getServiceNameVersion().getVersion().toString();
		} catch (AgentNotKnownException e) {
			return "-1";
		}
	}
}
