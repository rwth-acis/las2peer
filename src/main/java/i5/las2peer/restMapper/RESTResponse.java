package i5.las2peer.restMapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for communication between WebConnector and RESTMapper.
 *
 */
public class RESTResponse implements Serializable {

	private static final long serialVersionUID = 1L;

	byte[] body;
	Map<String, List<String>> headers = new HashMap<>();
	int httpCode;

	public RESTResponse(int httpCode) {
		this.httpCode = httpCode;
	}

	public void addHeader(String name, String value) {
		if (!headers.containsKey(name)) {
			headers.put(name, new ArrayList<String>());
		}
		headers.get(name).add(value);
	}

	public void setBody(byte[] response) {
		this.body = response;
	}

	public byte[] getBody() {
		return body;
	}

	public Map<String, List<String>> getHeaders() {
		return headers;
	}

	public int getHttpCode() {
		return httpCode;
	}
}
