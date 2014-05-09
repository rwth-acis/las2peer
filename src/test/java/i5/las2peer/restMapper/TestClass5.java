package i5.las2peer.restMapper;

import i5.las2peer.restMapper.annotations.*;
/**
 * @author Alexander
 */
@Path("books/{id}")
@Consumes("text/plain")
@Produces("text/plain")
public class TestClass5
{
    public TestClass5()
    {

    }

    @GET
    public int b1(@PathParam("id")int id)
    {
        return id;
    }




}
