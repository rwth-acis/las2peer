package i5.las2peer.testing;

import i5.las2peer.api.Service;
import i5.las2peer.execution.L2pServiceException;


/**
 * a simple <i>Service</i> for testing failures in the constructor of a service
 * 
 * @author Holger Janssen
 * @version $Revision: 1.1 $, $Date: 2013/02/21 12:15:17 $
 *
 */
public class NotCreatableService extends Service {

	/**
	 * simple constructor just throwing an Exception
	 * 
	 * @throws L2pServiceException
	 */
	public NotCreatableService () throws L2pServiceException {
		throw new L2pServiceException ( "Constructor is throwing an exception!");
	}
	
	
}
