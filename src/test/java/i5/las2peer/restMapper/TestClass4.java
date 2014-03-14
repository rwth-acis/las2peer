package i5.las2peer.restMapper;

import i5.las2peer.restMapper.annotations.*;
/**
 * @author Alexander
 */
@Path("books/{id}")
@Consumes("text/plain")
@Produces("text/plain")
public class TestClass4
{
    public TestClass4()
    {

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
