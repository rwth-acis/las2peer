package i5.las2peer.restMapper.exceptions;


/**
 * Exception is thrown, when there is not a supported URI path
 * @author Alexander
 *
 */
public class NotSupportedUriPathException extends Exception {

	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8124228440016225003L;
	String errorCode;
	public NotSupportedUriPathException(String code)
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
		return "Path: "+errorCode;
	}
}