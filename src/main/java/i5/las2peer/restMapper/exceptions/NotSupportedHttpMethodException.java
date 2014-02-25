package i5.las2peer.restMapper.exceptions;

/**
 * Exception is thrown, when there is not a supported HTTP method
 * @author Alexander
 *
 */
public class NotSupportedHttpMethodException extends Exception {

	
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6454667548614471201L;
	String errorCode;
	public NotSupportedHttpMethodException(String code)
	{
		super();
		errorCode=code;
	}
	
	public String getErrorCode()
	{
		return errorCode;
	}
	
	public String getMessage()
	{
		return "HTTP Method: "+errorCode;
	}
}