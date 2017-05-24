package i5.las2peer.connectors.nodeAdminConnector;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import i5.las2peer.connectors.nodeAdminConnector.multipart.MultipartHelper;

public class ParameterFilter extends Filter {

	public static final int MAX_REQUEST_BODY_SIZE = 10 * 1000 * 1000; // 10 MB

	@Override
	public String description() {
		return "Parses the requested URI for parameters";
	}

	@Override
	public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
		ParameterMap parameters = new ParameterMap();
		exchange.setAttribute("parameters", parameters);
		parseGetParameters(exchange, parameters);
		parsePostParameters(exchange, parameters);
		chain.doFilter(exchange);
	}

	private void parseGetParameters(HttpExchange exchange, ParameterMap parameters)
			throws UnsupportedEncodingException {
		URI requestedUri = exchange.getRequestURI();
		String query = requestedUri.getRawQuery();
		parseQuery(query, parameters);
	}

	private void parsePostParameters(HttpExchange exchange, ParameterMap parameters) throws IOException {
		if ("post".equalsIgnoreCase(exchange.getRequestMethod())) {
			String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
			if (MultipartHelper.MULTIPART_BOUNDARY_PATTERN.matcher(contentType).find()) {
				InputStream is = exchange.getRequestBody();
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				int nRead;
				byte[] data = new byte[4096];
				boolean overflow = false;
				while ((nRead = is.read(data, 0, data.length)) != -1) {
					if (buffer.size() < MAX_REQUEST_BODY_SIZE - data.length) {
						// still space left in local buffer
						buffer.write(data, 0, nRead);
					} else {
						overflow = true;
						// no break allowed otherwise the client gets an exception (like connection closed)
						// so we have to read all given content
					}
				}
				if (overflow) {
					byte[] content = ("Given request body exceeds limit of " + MAX_REQUEST_BODY_SIZE + " bytes")
							.getBytes(StandardCharsets.UTF_8);
					exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
					exchange.sendResponseHeaders(HttpURLConnection.HTTP_ENTITY_TOO_LARGE, content.length);
					OutputStream os = exchange.getResponseBody();
					os.write(content);
					os.close();
					throw new IOException("HTTP content body too large");
				} else {
					parameters.putAll(MultipartHelper.getParts(buffer.toByteArray(), contentType));
				}
			} else {
				Charset charset = StandardCharsets.UTF_8;
				String encoding = exchange.getRequestHeaders().getFirst("Content-Encoding");
				if (encoding != null && !encoding.isEmpty()) {
					charset = Charset.forName(encoding);
				}
				InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), charset);
				BufferedReader br = new BufferedReader(isr);
				// XXX read more lines?
				String query = br.readLine();
				parseQuery(query, parameters);
			}
		}
	}

	private void parseQuery(String query, ParameterMap parameters) throws UnsupportedEncodingException {
		if (query != null) {
			String pairs[] = query.split("[&]");

			for (String pair : pairs) {
				String param[] = pair.split("[=]");

				String key = null;
				String value = null;
				if (param.length > 0) {
					key = URLDecoder.decode(param[0], System.getProperty("file.encoding"));
				}

				if (param.length > 1) {
					value = URLDecoder.decode(param[1], System.getProperty("file.encoding"));
				}

				if (parameters.containsKey(key)) {
					Object obj = parameters.get(key);
					if (obj instanceof ParameterList) {
						ParameterList values = (ParameterList) obj;
						values.add(value);
					} else if (obj instanceof String) {
						List<String> values = new ArrayList<>();
						values.add((String) obj);
						values.add(value);
						parameters.put(key, values);
					} else {
						throw new IllegalStateException(
								"Neither a String nor a list, but got a " + obj.getClass().getCanonicalName());
					}
				} else {
					parameters.put(key, value);
				}
			}
		}
	}

	public static class ParameterMap extends HashMap<String, Object> {

		private static final long serialVersionUID = 1L;

		public String getSingle(String key) {
			Object obj = get(key);
			if (obj == null) {
				return null;
			} else if (obj instanceof String) {
				return (String) obj;
			} else {
				throw new IllegalArgumentException(
						"The value for '" + key + "' is not a single value, but " + obj.getClass().getCanonicalName());
			}
		}

	}

	public static interface ParameterList extends List<String> {

	}

}