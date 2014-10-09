package i5.las2peer.webConnector;



import i5.las2peer.api.Service;

import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.*;
import i5.las2peer.restMapper.HttpResponse;
/**
 * Service to test the web connector
*
 *
 */
@Version("0.2")
public class TestService3 extends Service
{

    /**
     * constructor
     */
    public TestService3()
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


    @GET
    @Path("test1/{a}/{b}")
    public HttpResponse test1(@PathParam("a") String a, @PathParam("b") String b,
                         @HeaderParam(name="c", defaultValue="") String c, @HttpHeaders String headers)
    {
        String result=a+b+c;
        HttpResponse response=new HttpResponse(result,200);
        response.setHeader("hi","ho");
        response.setHeader("Content-Type","text/plain");
        response.setHeader("Server-Name","foo");
        return response;
    }

    @GET
    @Path("test2/{a}/{b}")
    public HttpResponse test2()
    {
        String result="5";
        HttpResponse response=new HttpResponse(result,412);



        return response;
    }



}
