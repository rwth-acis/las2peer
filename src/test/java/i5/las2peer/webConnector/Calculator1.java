package i5.las2peer.webConnector;

import i5.las2peer.api.Service;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.Version;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Version("0.1")
public class Calculator1 extends Service {

	@GET
	@Path("/add/{num1}/{num2}")
	public float add(@PathParam("num1") float num1, @PathParam("num2") float num2) {
		return num1 + num2;
	}

	@GET
	@Path("/sub/{num1}/{num2}")
	public float subtract(@PathParam("num1") float num1, @PathParam("num2") float num2) {
		return num1 - num2;
	}

	public String getRESTMapping() {
		String result = "";
		try {
			result = RESTMapper.getMethodsAsXML(this.getClass());
		} catch (Exception e) {

			e.printStackTrace();
		}
		return result;
	}

}
