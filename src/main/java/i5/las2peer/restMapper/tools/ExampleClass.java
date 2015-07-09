package i5.las2peer.restMapper.tools;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.ContentParam;

@Path("/books/{id}{}")
@Consumes("text/plain")
@Produces("text/plain")
public class ExampleClass
{

	// example how to get the mapping, if a class object is available
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

	// simplest example
	@GET
	@Path("/pages")
	public int getPageCount(@PathParam("id") int id)
	{
		return 12;
	}

	// usage of additional path parameter and query parameters
	@GET
	@Path("/pages/{page}")
	public String getPageContent(@PathParam("id") int id, @PathParam("page") int page,
			@QueryParam("fullpage") @DefaultValue("false") boolean fullPage)
	{
		return "Once upon a time...";
	}

	// usage of produces, if the client wants ogg (Accept header), this method is chosen
	@GET
	@Path("/audiobooks/{audiobookname}")
	@Produces("audio/ogg")
	public String getAudioBookTrackOgg(@PathParam("id") int id, @PathParam("audiobookname") String name)
	{
		return "encodedOgg";
	}

	// usage of produces, if client wants mp4 (Accept header), this method is chosen
	@GET
	@Path("/audiobooks/{audiobookname}")
	@Produces("audio/mp4")
	public String getAudioBookTrackMp4(@PathParam("id") int id, @PathParam("audiobookname") String name)
	{
		return "encodedMp4";
	}

	// usage of header parameters
	@PUT
	public boolean createNewBook(@PathParam("id") int id,
			@HeaderParam(value = "User-Agent") @DefaultValue(value = "") String agent)
	{
		return true;
	}

	// usage of consumes, accepts only ogg and mpeg
	@POST
	@Path("/audiobooks/{audiobookname}")
	@Consumes({ "audio/ogg", "audio/mpeg" })
	public boolean addAudioBookTrack(@PathParam("id") int id, @PathParam("audiobookname") String name,
			@ContentParam String content)
	{
		return true;
	}

	// delete example
	@DELETE
	public boolean deleteBook(@PathParam("id") int id)
	{
		return true;
	}

	// usage of HttpResponse, where response code and headers can be set.
	@GET
	@Path("/special")
	public HttpResponse special(@PathParam("id") int id)
	{
		HttpResponse response = new HttpResponse("", 200);
		response.setHeader("myheader", "myHeaderValue");
		return response;
	}

}
