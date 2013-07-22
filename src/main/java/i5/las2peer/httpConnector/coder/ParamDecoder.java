package i5.las2peer.httpConnector.coder;

import java.io.IOException;
import java.io.Serializable;

/**
 * The abstract ParamDecoder class defines all necessary methods to decode
 * parameters coded by a fitting {@link ParamCoder} class.
 *
 * @author Holger Janßen
 */


public abstract class ParamDecoder
{
	
		
	/**
	 * check the header of any message to decode
	 *
	 * @return   number of (top level) values transported
	 *
	 * @exception   InvalidCodingException
	 * @exception   IOException
	 *
	 */
	public abstract int checkHeader () throws InvalidCodingException, IOException;
	
	/**
	 * Method checking the rest of the message after decoding all of the parameters
	 *
	 * @exception   InvalidCodingException
	 * @exception   IOException
	 *
	 */
	public abstract void checkFooter () throws InvalidCodingException, IOException;
	
	/**
	 * decode a single parameter
	 *
	 * @return   an Object
	 *
	 * @exception   InvalidCodingException
	 * @exception   IOException
	 *
	 */
	public abstract Object decodeSingle () throws InvalidCodingException, IOException;
	
	/**
	 * decode an array of parameters
	 *
	 * @return   an Object[]
	 *
	 * @exception   InvalidCodingException
	 * @exception   IOException
	 *
	 */
	public abstract Serializable[] decodeArray () throws InvalidCodingException, IOException;
	
	
	
}

