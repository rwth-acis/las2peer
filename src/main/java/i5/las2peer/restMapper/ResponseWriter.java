package i5.las2peer.restMapper;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;

final class ResponseWriter implements ContainerResponseWriter {

	private final AtomicBoolean closed;
	private RESTResponse response = null;
	private ByteArrayOutputStream outputStream;

	ResponseWriter() {
		this.closed = new AtomicBoolean(false);
	}

	@Override
	public OutputStream writeResponseStatusAndHeaders(final long contentLength, final ContainerResponse context)
			throws ContainerException {

		// create response with status
		this.response = new RESTResponse(context.getStatus());

		// write header
		final MultivaluedMap<String, String> responseHeaders = context.getStringHeaders();
		for (final Map.Entry<String, List<String>> e : responseHeaders.entrySet()) {
			for (final String value : e.getValue()) {
				this.response.addHeader(e.getKey(), value);
			}
		}

		// create output stream
		outputStream = new ByteArrayOutputStream(contentLength < 0 ? 0 : (int) contentLength);
		return outputStream;
	}

	@Override
	public boolean suspend(final long timeOut, final TimeUnit timeUnit, final TimeoutHandler timeoutHandler) {
		throw new UnsupportedOperationException("Method suspend is not supported");
	}

	@Override
	public void setSuspendTimeout(final long timeOut, final TimeUnit timeUnit) throws IllegalStateException {
		throw new UnsupportedOperationException("Method setSuspendTimeout is not supported");
	}

	@Override
	public void failure(final Throwable error) {
		this.response = new RESTResponse(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
		outputStream = new ByteArrayOutputStream(0);
		commit();
		rethrow(error);
	}

	@Override
	public boolean enableResponseBuffering() {
		return true;
	}

	@Override
	public void commit() {
		if (closed.compareAndSet(false, true)) {
			this.response.setBody(outputStream.toByteArray());
		}
	}

	/**
	 * Rethrow the original exception as required by JAX-RS, 3.3.4
	 *
	 * @param error throwable to be re-thrown
	 */
	private void rethrow(final Throwable error) {
		if (error instanceof RuntimeException) {
			throw (RuntimeException) error;
		} else {
			throw new ContainerException(error);
		}
	}

	public RESTResponse getResponse() {
		if (closed.get() == false) {
			throw new IllegalStateException("Response has not been commited.");
		}

		return this.response;
	}
}
