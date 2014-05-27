package i5.las2peer.webConnector;


import i5.las2peer.api.Service;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.GET;
import i5.las2peer.restMapper.annotations.Path;
import i5.las2peer.restMapper.annotations.Version;

/**
 * Service to test the web connector
 * @author Alexander
 *
 */
@Version("0.2")
@Path("exception")
public class TestService5 extends Service
{

    /**
     * constructor
     */
    public TestService5()
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

           result="<service name=\"i5.las2peer.webConnector.TestService5\" version=\"0.1\">"+
            "<methods>"+
            "<method httpMethod=\"get\" name=\"b4\" path=\"exception\" type=\"int\">"+
            "<parameters/>"+
            "</method>"+
            "</methods>"+
            "</service>";
        }
        return result;
    }



    @GET
    public int b4() throws Exception
    {
        throw new Exception();
    }

}