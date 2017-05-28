package i5.las2peer.connectors.nodeAdminConnector.multipart;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;

public class FormDataPart {

	private final Map<String, FormDataHeader> headers;
	private final byte[] content;

	public FormDataPart(String name, Map<String, FormDataHeader> headers, byte[] content) {
		this.headers = headers;
		this.content = content;
	}

	/**
	 * Gets the header with the given name.
	 * 
	 * @param name header name like Content-Disposition or Content-Type
	 * @return Returns the header or {@code null} if the header does not exist.
	 */
	public FormDataHeader getHeader(String name) {
		return headers.get(name);
	}

	/**
	 * Gets the content as raw binary data.
	 * 
	 * @return Returns the raw binary data or {@code null} if no content is available.
	 */
	public byte[] getContentRaw() {
		return content;
	}

	/**
	 * Gets the content UTF-8 encoded as String.
	 * 
	 * @return Returns the UTF-8 encoded String or {@code null} if no content is available.
	 */
	public String getContent() {
		if (content != null) {
			return new String(content, StandardCharsets.UTF_8);
		}
		return null;
	}

	/**
	 * 
	 * @return Returns the content type of this form data part or {@code null} if no content type is set.
	 * @throws MalformedFormDataException If the content type header has more than one parameter.
	 */
	public String getContentType() throws MalformedFormDataException {
		FormDataHeader contentTypeHeader = getHeader(HttpHeaders.CONTENT_TYPE);
		if (contentTypeHeader != null) {
			// there should be only one parameter for this header
			if (contentTypeHeader.countParameter() > 1) {
				throw new MalformedFormDataException("Only one parameter expected");
			}
			String mt = contentTypeHeader.getFirstParameterKey();
			if (mt != null && !mt.isEmpty()) {
				return mt;
			}
		}
		return null;
	}

}
