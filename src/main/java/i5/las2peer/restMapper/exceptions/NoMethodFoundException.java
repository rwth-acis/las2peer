package i5.las2peer.restMapper.exceptions;



/**
 * Exception is thrown, when a service method could not be found
 * @author Alexander
 *
 */
public class NoMethodFoundException extends Exception {

	
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 5388974002513762310L;
	String errorCode;
	public NoMethodFoundException(String code)
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