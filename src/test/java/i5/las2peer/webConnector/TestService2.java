package i5.las2peer.webConnector;

import i5.las2peer.api.Service;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.ContentParam;
import i5.las2peer.restMapper.annotations.Version;
import i5.las2peer.security.UserAgent;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * Service to test the web connector
 *
 */
@Version("0.2")
public class TestService2 extends Service
{

	/**
	 * constructor, initializes RESTMapper
	 */
	public TestService2()
	{

	}

	/**
	 * get all annotation and method data to allow mapping
	 */

	public String getRESTMapping()
	{
		String result = "";
		try {
			result = RESTMapper.getMethodsAsXML(this.getClass());
		} catch (Exception e) {

			e.printStackTrace();
		}
		return result;
	}

	@POST
	@Path("do/{a}/{b}")
	public String concat(@PathParam("a") String a, @PathParam("b") String b,
			@ContentParam String c)
	{
		return a + b + c;
	}

	@GET
	@Path("userinfo")
	public String getUserInfo() {
		if (this.getActiveAgent() instanceof UserAgent) {
			UserAgent u = (UserAgent) this.getActiveAgent();
			return "{'id':'" + u.getId() + ", 'name':'" + u.getLoginName() + "','email':'" + u.getEmail() + "'}";
		} else {
			return "Shit happened...";
		}
	}

}
