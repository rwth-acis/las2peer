package i5.las2peer.httpConnector.coder;

/**
 * Exception thrown by a {@link ParamDecoder} subclass, if the coding of
 * the type of a parameter delivered to the coder has not been implemented (yet).
 *
 * @author Holger Jan&szlig;en
 */

public class ParameterTypeNotImplementedException extends CodingException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7798835052258208311L;

	public ParameterTypeNotImplementedException ( String classname ) {
		super ( "The parameter type " + classname + " is not implemented for this protocol!" );
	}
}

