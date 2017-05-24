package i5.las2peer.connectors.webConnector.client;

import java.util.HashMap;

public class ClientResponse {

	String response = "";
	HashMap<String, String> headers = new HashMap<>();
	int httpCode;

	public ClientResponse(int httpCode) {
		this.httpCode = httpCode;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public String getResponse() {
		return response;
	}

	public HashMap<String, String> getHeaders() {
		return headers;
	}

	public int getHttpCode() {
		return httpCode;
	}

	public void addHeader(String name, String value) {
		// HTTP headers are case insensitive
		headers.put(name.toLowerCase(), value);
	}

	public String getHeader(String name) {
		// HTTP headers are case insensitive
		return headers.get(name.toLowerCase());
	}

}
