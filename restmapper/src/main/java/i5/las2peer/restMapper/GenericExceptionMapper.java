package i5.las2peer.restMapper;

import java.util.logging.Level;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import i5.las2peer.api.execution.ResourceNotFoundException;
import i5.las2peer.api.execution.ServiceAccessDeniedException;
import i5.las2peer.api.execution.InvocationBadArgumentException;
import i5.las2peer.api.execution.ServiceInvocationException;
import i5.las2peer.logging.L2pLogger;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

	private final L2pLogger logger = L2pLogger.getInstance(GenericExceptionMapper.class);

	@Override
	public Response toResponse(Throwable e) {
		// FIXME use format from WebConnector#sendUnexpectedErrorResponse
		logger.log(Level.INFO, "Request failed: " + e.toString(), e);
		int code = Status.INTERNAL_SERVER_ERROR.getStatusCode();
		// map ServiceInvocationExceptions to WebApplicationExceptions
		if (e instanceof ServiceInvocationException) {
			if (e instanceof InvocationBadArgumentException) {
				e = new BadRequestException(e.getMessage(), e.getCause());
			} else if (e instanceof ResourceNotFoundException) {
				e = new NotFoundException(e.getMessage(), e.getCause());
			} else if (e instanceof ServiceAccessDeniedException) {
				e = new ForbiddenException(e.getMessage(), e.getCause());
			} else {
				e = new InternalServerErrorException("Exception during RMI call", e);
			}
		}
		if (e instanceof WebApplicationException) {
			WebApplicationException webEx = (WebApplicationException) e;
			code = webEx.getResponse().getStatus();
		}
		return Response.status(code).entity(new ExceptionEntity(code, e)).type(MediaType.APPLICATION_JSON_TYPE).build();
	}

}
