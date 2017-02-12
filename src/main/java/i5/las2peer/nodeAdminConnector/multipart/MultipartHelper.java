package i5.las2peer.nodeAdminConnector.multipart;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.fileupload.MultipartStream.MalformedStreamException;
import org.apache.commons.fileupload.ParameterParser;

public class MultipartHelper {

	// this header is not known to javax.ws.rs.core.HttpHeaders
	public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
	public static final Pattern MULTIPART_BOUNDARY_PATTERN = Pattern.compile("multipart/form-data;\\s*boundary=(.*)");

	private static final int MULTIPARTSTREAM_BUFSIZE = 4096;

	/**
	 * This methods reads all multipart data from given form data formatted as String.
	 * 
	 * @param formData The form data as multipart encoded.
	 * @param contentType The content type given by the HTTP header.
	 * @return Returns the mulitpart form elements split up into handy objects for further processing.
	 * @throws MalformedStreamException If an error occurs with the input stream.
	 * @throws IOException If an error occurs with the input stream.
	 */
	public static Map<String, FormDataPart> getParts(byte[] formData, String contentType)
			throws MalformedStreamException, IOException {
		Map<String, FormDataPart> result = new HashMap<>();
		InputStream input = new ByteArrayInputStream(formData);
		byte[] boundary = MultipartHelper.getBoundary(contentType);
		MultipartStream multipartStream = new MultipartStream(input, boundary, MULTIPARTSTREAM_BUFSIZE, null);
		boolean hasNextPart = multipartStream.skipPreamble();
		while (hasNextPart) {
			String txtHeaders = multipartStream.readHeaders();
			byte[] multipartContent = MultipartHelper.readData(multipartStream);
			// process headers
			Map<String, FormDataHeader> headerMap = MultipartHelper.parseHeaders(txtHeaders);
			FormDataHeader contentDispositionHeader = headerMap.get(HEADER_CONTENT_DISPOSITION);
			String dispositionName = contentDispositionHeader.getParameter("name");
			if (dispositionName == null || dispositionName.isEmpty()) {
				throw new MalformedFormDataException("no name provided");
			}
			// copy part to result array
			result.put(dispositionName, new FormDataPart(dispositionName, headerMap, multipartContent));
			hasNextPart = multipartStream.readBoundary();
		}
		return result;
	}

	private static byte[] getBoundary(String contentTypeHeader) throws MalformedStreamException {
		final Matcher matcher = MULTIPART_BOUNDARY_PATTERN.matcher(contentTypeHeader);
		matcher.matches();
		if (matcher.groupCount() != 1) {
			throw new MalformedStreamException("found more than one boundary in content type header");
		}
		return matcher.group(1).getBytes();
	}

	private static byte[] readData(MultipartStream stream) throws MalformedStreamException, IOException {
		// create some output stream
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			stream.readBodyData(output);
		} finally {
			try {
				output.close();
			} catch (IOException e) {
				// ignore close exception
			}
		}
		return output.toByteArray();
	}

	private static Map<String, FormDataHeader> parseHeaders(String txtHeaders) throws MalformedFormDataException {
		// maps header to map of attribute names and values
		final Map<String, FormDataHeader> result = new HashMap<>();
		// split lines
		final String[] headers = txtHeaders.split("\\r?\\n");
		// separate header name and value
		for (String header : headers) {
			header = header.trim();
			if (!header.isEmpty()) {
				final int sep = header.indexOf(":");
				if (sep == -1) {
					throw new MalformedFormDataException("missing name value separator for (" + header + ") header");
				}
				final String headerName = header.substring(0, sep).trim();
				final String attributes = header.substring(sep + 1).trim();
				final Map<String, String> parameters = new ParameterParser().parse(attributes, ';');
				result.put(headerName, new FormDataHeader(headerName, parameters));
			} else {
				throw new MalformedFormDataException("empty header");
			}
		}
		return result;
	}

}
