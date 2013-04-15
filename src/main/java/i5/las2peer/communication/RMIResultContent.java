package i5.las2peer.communication;

import java.io.Serializable;


/**
 * a simple content class for a {@link Message} indicating a successful execution of
 * an remote invocation task
 * 
 * @author Holger Janssen
 * @version $Revision: 1.2 $, $Date: 2013/02/12 18:10:24 $
 *
 */
public class RMIResultContent implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4804271575347018920L;
	private Serializable content;

	public RMIResultContent ( Serializable content ) {
		this.content = content;
	}
	
	public Serializable getContent () { return content; } 
	
}
