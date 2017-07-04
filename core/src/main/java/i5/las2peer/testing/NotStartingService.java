package i5.las2peer.testing;

import i5.las2peer.api.Service;
import i5.las2peer.api.ServiceException;

/**
 * A simple service for testing a service which has problems in the notification method of node registering.
 * 
 * 
 *
 */
public class NotStartingService extends Service {

	@Override
	public void onStart() throws ServiceException {
		throw new ServiceException("Some error preventing this service from starting");
	}

}
