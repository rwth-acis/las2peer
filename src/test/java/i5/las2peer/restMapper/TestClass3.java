package i5.las2peer.restMapper;

import i5.las2peer.restMapper.annotations.*;

@Path("animals/{id}")
@Consumes("text/plain")
public class TestClass3 {

    public TestClass3()
    {

    }

    @GET
    @Path("food/{foodID}")
    public int b1(@PathParam("id")int id, @PathParam("foodID")int foodID)
    {
        return id+foodID;
    }

    @DELETE
    public int b2(@PathParam("id")int id)
    {
        return id;
    }

    @POST
    public int b3(@PathParam("id")int id)
    {
        return id*2;
    }

    @POST
    @Consumes("text/xml")
    public int b4(@PathParam("id")int id)
    {
        return id*5;
    }

    @POST
    @Consumes({"audio/ogg","audio/mpeg"})
    public int b5(@PathParam("id")int id)
    {
        return id*7;
    }

    @POST
    @Consumes("video/*")
    public int b6(@PathParam("id")int id)
    {
        return id*11;
    }
}
