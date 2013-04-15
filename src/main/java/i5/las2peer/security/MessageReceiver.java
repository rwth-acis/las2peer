package i5.las2peer.security;

import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.p2p.Node;


/**
 * an interface for receiving messages in the p2p network.
 * Used by {@link Agent}s and {@link Mediator}s.
 * 
 * @author Holger Janssen
 * @version $Revision: 1.2 $, $Date: 2013/02/12 17:42:24 $
 *
 */
public interface MessageReceiver {

	/**
	 * receive a message
	 * Will be called by a {@link i5.las2peer.p2p.Node} implementation on reception of messages
	 * which this receiver is responsible for.
	 * 
	 * @param message
	 * @param c
	 * @throws MessageException
	 */
	public void receiveMessage(Message message, Context c) throws MessageException;
	
	
	/**
	 * returns the id of the agent, this mediator is responsible for
	 * 
	 * @return id of the agent, this receiver is responsible for
	 */
	public long getResponsibleForAgentId ();
	
	
	/**
	 * called by a {@link i5.las2peer.p2p.Node} on registering a MessageReceiver to the network
	 *  
	 * @param node the node this receiver has been registered to
	 * @throws AgentException
	 */
	public void notifyRegistrationTo ( Node node ) throws AgentException;

	

	/**
	 * called by a {@link i5.las2peer.p2p.Node} on unregistering a MessageReceiver from the network
	 */ 
	public void notifyUnregister();
}
