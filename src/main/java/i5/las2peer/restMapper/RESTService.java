package i5.las2peer.restMapper;

import i5.las2peer.api.Service;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;

public abstract class RESTService extends Service {
	/**
	 * Returns the service alias.
	 * 
	 * @return the alias
	 */
	public String getAlias() {
		try {
			return RESTMapper.getFirstPathFragment(this.getClass());
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * Generates the REST mapping.
	 * 
	 * @return the mapping
	 */
	public String getRESTMapping() {
		String result = "";
		try {
			result = RESTMapper.getMethodsAsXML(this.getClass());
		} catch (Exception e) {
			// create and publish a monitoring message
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
		}
		return result;
	}
}
