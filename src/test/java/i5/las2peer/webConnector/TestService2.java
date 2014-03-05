package i5.las2peer.webConnector;



import i5.las2peer.api.Service;

import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.*;
/**
 * Service to test the web connector
 * @author Alexander
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
		String result="";
		try {
			result=RESTMapper.getMethodsAsXML(this.getClass());
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		return result;
	}
	
	
	@POST
	@Path("do/{a}/{b}")
	public String concat(@PathParam("a") String a, @PathParam("b") String b,
			@ContentParam String c )
	{
		return a+b+c;
	}
	
	
	
	
}
