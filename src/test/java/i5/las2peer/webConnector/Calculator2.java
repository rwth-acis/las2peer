package i5.las2peer.webConnector;

import i5.las2peer.api.Service;
import i5.las2peer.restMapper.annotations.Version;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Version("0.1")
public class Calculator2 extends Service {

	@GET
	@Path("/mul/{num1}/{num2}")
	public float mul(@PathParam("num1") float num1, @PathParam("num2") float num2) {
		return num1 * num2;
	}

	@GET
	@Path("/div/{num1}/{num2}")
	public float divide(@PathParam("num1") float num1, @PathParam("num2") float num2) {
		return num1 / num2;
	}

}
