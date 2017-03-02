package i5.las2peer.security;

import i5.las2peer.api.security.AgentException;
import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.p2p.Node;

/**
 * 
 * An interface for receiving messages in the p2p network. Used by {@link AgentImpl}s and {@link Mediator}s.
 * 
 * 
 *
 */
public interface MessageReceiver {

	/**
	 * 
	 * Receives a message. Will be called by a {@link i5.las2peer.p2p.Node} implementation on reception of messages that
	 * this receiver is responsible for.
	 * 
	 * @param message
	 * @param c
	 * @throws MessageException
	 * 
	 */
	public void receiveMessage(Message message, AgentContext c) throws MessageException;

	/**
	 * 
	 * In case a {@link Mediator} implements this interface, this method will return the id of the agent, this mediator
	 * is responsible for.
	 * 
	 * Otherwise it will just return the {@link AgentImpl}s id.
	 * 
	 * @return id of the agent, this receiver is responsible for
	 * 
	 */
	public String getResponsibleForAgentSafeId();

	/**
	 * 
	 * Called by a {@link i5.las2peer.p2p.Node} on registering a MessageReceiver to the network.
	 * 
	 * @param node the node this receiver has been registered to
	 * @throws AgentException
	 * 
	 */
	public void notifyRegistrationTo(Node node) throws AgentException;

	/**
	 * 
	 * Called by a {@link i5.las2peer.p2p.Node} on unregistering a MessageReceiver from the network.
	 * 
	 */
	public void notifyUnregister();

}
