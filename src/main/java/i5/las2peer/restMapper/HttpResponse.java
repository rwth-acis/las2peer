package i5.las2peer.restMapper;

import java.util.ArrayList;

import i5.las2peer.restMapper.data.Pair;

/**
 * For compatibility with existing services
 * 
 */
public class HttpResponse extends GeneralResponse {

	private static final long serialVersionUID = 1618302269572403581L;
	public static final String WEBCONNECTOR = "webconnector";
	public static final String HTTP_TYPE = "http";
	public static final String HTTP_STATUS = "http_status";
	public static final String HTTP_HEADER = "http_header";

	/**
	 * constructor
	 * 
	 * @param result string returned as method response
	 */
	public HttpResponse(String result) {
		super(result);
	}

	/**
	 * constructor
	 * 
	 * @param result string returned as method response
	 * @param status http status code
	 */
	public HttpResponse(String result, int status) {
		this(result);
		setStatus(status);
	}

	public void setStatus(int status) {
		addAttribute(new ResponseAttribute(WEBCONNECTOR, HTTP_TYPE, HTTP_STATUS, Integer.toString(status)));
	}

	public int getStatus() {
		return Integer.parseInt(getAttributeByTypeName(WEBCONNECTOR, HTTP_TYPE, HTTP_STATUS));
	}

	public void setHeader(String header, String value) {
		// not yet set
		if (getAttributeByTypeName(WEBCONNECTOR, HTTP_HEADER, header) == null) {
			addAttribute(new ResponseAttribute(WEBCONNECTOR, HTTP_HEADER, header, value));
		} else {
			getAttributesByTypeName(WEBCONNECTOR, HTTP_HEADER, header).get(0).setAttributeValue(value);
		}

	}

	public void removeHeader(String header) {
		ArrayList<ResponseAttribute> temp = getAttributesByTypeName(WEBCONNECTOR, HTTP_HEADER, header);
		if (temp.size() > 0) {
			getAttributes().remove(temp.get(0));
		}

	}

	public Pair<String>[] listHeaders() {
		ArrayList<ResponseAttribute> temp = getAttributesByType(WEBCONNECTOR, HTTP_HEADER);
		@SuppressWarnings("unchecked")
		Pair<String>[] result = new Pair[temp.size()];
		int i = 0;
		for (ResponseAttribute attr : temp) {
			result[i] = new Pair<String>((String) attr.getAttributeName(), (String) attr.getAttributeValue());
			i++;
		}
		return result;
	}

}
