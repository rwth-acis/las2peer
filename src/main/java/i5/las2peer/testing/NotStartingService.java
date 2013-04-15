package i5.las2peer.testing;

import i5.las2peer.api.Service;
import i5.las2peer.execution.L2pServiceException;
import i5.las2peer.p2p.Node;

/**
 * a simple service for testing a service which has problems in the 
 * notification method of node registering
 * 
 * @author Holger Janssen
 * @version $Revision: 1.1 $, $Date: 2013/02/21 12:15:17 $
 *
 */
public class NotStartingService extends Service {

	
	@Override
	public void launchedAt ( Node node ) throws L2pServiceException {
		super.launchedAt(node);
		
		throw new L2pServiceException ("Some error preventing this service from starting");
	}
	
	
}
