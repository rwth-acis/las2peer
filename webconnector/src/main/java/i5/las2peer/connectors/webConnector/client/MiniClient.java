package i5.las2peer.connectors.webConnector.client;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;

import i5.las2peer.connectors.webConnector.WebConnector;
import i5.las2peer.tools.SimpleTools;

/**
 * Very simple client to communicate with the las2peer web connector
 * 
 */
public class MiniClient {

	private String authorization;
	private String serverAddress;

	/**
	 * @deprecated Use {@link #setConnectorEndpoint(String)} instead.
	 * 
	 *             set address and port
	 * 
	 * @param address address of the server
	 * @param port if 0 no port is appended to the address
	 */
	@Deprecated
	public void setAddressPort(String address, int port) {
		if (port > 0) {
			setConnectorEndpoint(serverAddress += ":" + Integer.toString(port));
		} else {
			setConnectorEndpoint(address);
		}
	}

	/**
	 * Sets the connectors endpoint URI this client should connect to.
	 * 
	 * @param endpoint A connector endpoint like http://localhost:12345, with no trailing slash
	 */
	public void setConnectorEndpoint(String endpoint) {
		serverAddress = endpoint;
	}

	/**
	 * set login data
	 * 
	 * @param username
	 * @param password
	 */
	public void setLogin(String username, String password) {
		authorization = username + ":" + password;
		authorization = Base64.getEncoder().encodeToString(authorization.getBytes());
	}

	/**
	 * send request to server
	 * 
	 * @param method POST, GET, DELETE, PUT
	 * @param uri REST-URI (server address excluded)
	 * @param content if POST is used information can be embedded here
	 * @param contentType value of Content-Type header
	 * @param accept value of Accept Header
	 * @param headers headers for HTTP request
	 * @return returns server response
	 * 
	 */
	public ClientResponse sendRequest(String method, String uri, String content, String contentType, String accept,
			Map<String, String> headers) {
		String strContent = "[too big " + Integer.toString(content.length()) + " bytes]";
		if (content.length() < WebConnector.DEFAULT_MAX_REQUEST_BODY_SIZE) {
			strContent = content;
		}
		System.out.println("Request: " + method + " URI: " + uri + " Content: " + strContent + " "
				+ HttpHeaders.CONTENT_TYPE + ": " + contentType + " accept: " + accept + " headers: " + headers.size());
		HttpURLConnection connection = null;
		ClientResponse response;
		try {
			// Create connection
			URL url = new URL(String.format("%s/%s", serverAddress, uri));
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(method.toUpperCase());
			if (authorization != null && authorization.length() > 0) {
				connection.setRequestProperty("Authorization", "Basic " + authorization);
			}
			connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, contentType);
			connection.setRequestProperty("Accept", accept);
			connection.setRequestProperty("Content-Length", Integer.toString(content.getBytes().length));

			for (Map.Entry<String, String> header : headers.entrySet()) {
				connection.setRequestProperty(header.getKey(), header.getValue());
			}

			connection.setUseCaches(false);
			if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT")) {
				connection.setDoOutput(true);

				// Send request
				DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
				wr.writeBytes(content);
				wr.close();
			}

			// Get Response
			int code = connection.getResponseCode();
			response = new ClientResponse(code);

			InputStream is = connection.getErrorStream();
			try {
				is = connection.getInputStream();
			} catch (Exception e) {
				// XXX logging
			}

			if (is == null) {
				return response;
			}

			Map<String, List<String>> responseMap = connection.getHeaderFields();
			for (String key : responseMap.keySet()) {
				StringBuilder sb = new StringBuilder();
				List<String> values = responseMap.get(key);
				for (int i = 0; i < values.size(); i++) {
					Object o = values.get(i);
					sb.append(" " + o);
				}
				if (key == null) {
					key = "head";
				}
				response.addHeader(key.trim(), sb.toString().trim());
			}

			response.setRawResponse(SimpleTools.toByteArray(is));
			// TODO use charset from content-type header param
			BufferedReader rd = new BufferedReader(
					new InputStreamReader(new ByteArrayInputStream(response.getRawResponse()), StandardCharsets.UTF_8));
			StringBuilder responseText = new StringBuilder();
			String line;
			while ((line = rd.readLine()) != null) {
				responseText.append(line);
				responseText.append('\r');
			}
			response.setResponse(responseText.toString());
			rd.close();

			return response;

		} catch (Exception e) {

			e.printStackTrace();
			return null;

		} finally {

			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	/**
	 * send request to server
	 * 
	 * @param method POST, GET, DELETE, PUT
	 * @param uri REST-URI (server address excluded)
	 * @param content if POST is used information can be embedded here
	 * @param headers headers for HTTP request
	 * @return returns server response
	 * 
	 */
	public ClientResponse sendRequest(String method, String uri, String content, Map<String, String> headers) {
		return sendRequest(method, uri, content, "text/plain", "*/*", headers);
	}

	/**
	 * send request to server
	 * 
	 * @param method POST, GET, DELETE, PUT
	 * @param uri REST-URI (server address excluded)
	 * @param content if POST is used information can be embedded here
	 * @return returns server response
	 * 
	 */
	public ClientResponse sendRequest(String method, String uri, String content) {
		return sendRequest(method, uri, content, new HashMap<String, String>());
	}

}
