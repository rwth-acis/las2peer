package i5.las2peer.webConnector;

import i5.las2peer.api.Service;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.*;

@Version("0.1")
public class Calculator1 extends Service
{
	//@PUT
	//@Path("add/{number1}/{number2}")
	//public int add(@PathParam("number1") int num1, @PathParam("number2") int num2)
	
	@GET
	@Path("add/{num1}/{num2}")
	public float add(@PathParam("num1")float num1, @PathParam("num2") float num2)
	{
		return num1+num2;
	}
	
	@GET
	@Path("sub/{num1}/{num2}")
	public float subtract(@PathParam("num1")float num1, @PathParam("num2") float num2)
	{
		return num1-num2;
	}
    public String getRESTMapping()
    {
        String result="";
        try {
            result= RESTMapper.getMethodsAsXML(this.getClass());
        } catch (Exception e) {

            e.printStackTrace();
        }
        return result;
    }
}
