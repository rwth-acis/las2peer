package i5.las2peer.webConnector;


import i5.las2peer.api.Service;

import i5.las2peer.restMapper.MediaType;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.*;

/**
 * Service to test the web connector
 * @author Alexander
 *
 */
@Version("0.2")
@Path("books/{id}")
@Consumes(MediaType.TEXT_PLAIN)
@Produces(MediaType.TEXT_PLAIN)
public class TestService4 extends Service
{

    /**
     * constructor
     */
    public TestService4()
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
    public int a1(@PathParam("id") int id)
    {
        return id;
    }

    @POST
    @Consumes({"audio/ogg","audio/mpeg"})
    public int a2(@PathParam("id")int id)
    {
        return id*7;
    }

    @GET
    public int b1(@PathParam("id")int id)
    {
        return id;
    }

    @GET
    @Produces("audio/ogg")
    public int b2(@PathParam("id")int id)
    {
        return id*2;
    }
    @GET
    @Produces("audio/mp4")
    public int b3(@PathParam("id")int id)
    {
        return id*3;
    }


}