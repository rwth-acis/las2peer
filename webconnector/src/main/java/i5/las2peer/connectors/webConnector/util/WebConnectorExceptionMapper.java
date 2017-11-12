package i5.las2peer.connectors.webConnector.util;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import i5.las2peer.connectors.webConnector.WebConnector;
import i5.las2peer.restMapper.ExceptionEntity;

@Provider
public class WebConnectorExceptionMapper implements ExceptionMapper<Throwable> {

	private final WebConnector connector;

	public WebConnectorExceptionMapper(WebConnector connector) {
		this.connector = connector;
	}

	@Override
	public Response toResponse(Throwable e) {
		if (e instanceof WebApplicationException) {
			connector.logError("WebConnector request failed with: " + e.toString());
			WebApplicationException webException = (WebApplicationException) e;
			return Response.status(webException.getResponse().getStatus())
					.entity(new ExceptionEntity(webException.getResponse().getStatus(), e))
					.type(MediaType.APPLICATION_JSON_TYPE).build();
		} else {
			connector.logError("Internal Server Error: " + e.getMessage(), e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ExceptionEntity(Status.INTERNAL_SERVER_ERROR.getStatusCode(), e))
					.type(MediaType.APPLICATION_JSON_TYPE).build();
		}
	}

}
