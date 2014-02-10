package i5.las2peer.webConnector;
/**
 * Exception is thrown, whenever the server responses with an error
 * @author Alexander
 *
 */
public class HttpErrorException extends Exception {

	
	private static final long serialVersionUID = 1883775136912293664L;
	
	int errorCode;
	public HttpErrorException(int code)
	{
		super();
		errorCode=code;
	}
	/**
	 *  
	 * @return HTTP error code
	 */
	public int getErrorCode()
	{
		return errorCode;
	}
	/**
	 * returns HTTP error code as string
	 */
	public String getMessage()
	{
		return "HTTP Error: "+Integer.toString(errorCode);
	}
}
