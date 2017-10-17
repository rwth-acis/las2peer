package i5.las2peer.connectors.webConnector.util;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import i5.las2peer.connectors.webConnector.WebConnector;

@Provider
public class WebConnectorExceptionMapper implements ExceptionMapper<Throwable> {

	private final WebConnector connector;

	public WebConnectorExceptionMapper(WebConnector connector) {
		this.connector = connector;
	}

	@Override
	public Response toResponse(Throwable e) {
		connector.logError("Internal Server Error: " + e.getMessage(), e);
		if (e instanceof WebApplicationException) {
			WebApplicationException webException = (WebApplicationException) e;
			return Response.status(webException.getResponse().getStatus()).entity(e.getMessage())
					.type(MediaType.TEXT_PLAIN).build();
		} else {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).type(MediaType.TEXT_PLAIN)
					.build();
		}
	}

}
