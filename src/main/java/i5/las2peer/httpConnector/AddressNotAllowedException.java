package i5.las2peer.httpConnector;

import i5.las2peer.api.ConnectorException;

/**
 * Exception thrown (and handled) inside the (@see HttpConnectorRequestHandler)
 * when one tries to access a session from different remote addresses.
 *
 * @author Holger Jan�en
 * @version $Revision: 1.1 $, $Date: 2013/01/22 14:06:47 $
 */

public class AddressNotAllowedException extends ConnectorException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3147896844336450962L;

	public AddressNotAllowedException ( String sessionid, String remoteAddr ) {
		super ("Access to session " + sessionid + " not allowed from address " + remoteAddr + "!");
	}
	
}

