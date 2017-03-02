package i5.las2peer.testing;

import i5.las2peer.api.Service;
import i5.las2peer.api.ServiceException;
import i5.las2peer.security.L2pServiceException;

/**
 * A simple <i>Service</i> for testing failures in the constructor of a service.
 * 
 * 
 *
 */
public class NotCreatableService extends Service {

	/**
	 * simple constructor just throwing an Exception
	 * 
	 * @throws ServiceException
	 */
	public NotCreatableService() throws ServiceException {
		throw new ServiceException("Constructor is throwing an exception!");
	}

}
