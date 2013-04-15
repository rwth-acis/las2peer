package i5.las2peer.security;

import i5.las2peer.communication.Message;


/**
 * a simple interface to register message handlers to {@link Mediator}s
 * 
 * @author Holger Janssen
 * @version $Revision: 1.2 $, $Date: 2013/02/12 17:42:24 $
 *
 */
public interface MessageHandler {
	
	/**
	 * get an already opened message from an agent mediator and try to process it.
	 * return true, if this handle has successfully handled the message, false otherwise.
	 * 
	 * @param message
	 * @param context
	 * @return	true, if the received message has been handled and can be removed
	 */
	public boolean handleMessage ( Message message, Context context ) throws Exception;

}
