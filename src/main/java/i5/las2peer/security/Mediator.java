package i5.las2peer.security;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Vector;

import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.execution.L2pServiceException;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.TimeoutException;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.tools.SerializationException;

/**
 * A Mediator acts on behalf of an {@link PassphraseAgent}. This necessary e.g. for remote users logged in via a
 * {@link i5.las2peer.api.Connector} to collect incoming messages from the P2P network and transfer it to the connector.
 * <br>
 * Two ways for message handling are provided: Register a {@link MessageHandler} that will be called for each received
 * message. Multiple MessageHandlers are possible (for example for different message contents). The second way to handle
 * messages is to get pending messages from the Mediator directly via the provided methods. Handling then has to be done
 * via the calling entity (for example a service).
 * 
 */
public class Mediator implements MessageReceiver {

	private LinkedList<Message> pending = new LinkedList<Message>();

	private Agent myAgent;

	private Node runningAt = null;

	private Vector<MessageHandler> registeredHandlers = new Vector<MessageHandler>();

	/**
	 * Creates a new mediator.
	 * 
	 * @param a
	 * @throws L2pSecurityException
	 */
	public Mediator(Agent a) throws L2pSecurityException {
		if (a.isLocked())
			throw new L2pSecurityException("You need to unlock the private key of the agent for mediating.");

		myAgent = a;
	}

	/**
	 * Gets (and removes) the next pending message.
	 * 
	 * @return the next collected message
	 */
	public Message getNextMessage() {
		if (pending.size() == 0) {
			return null;
		}

		return pending.pollFirst();
	}

	/**
	 * Does this mediator have pending messages?
	 * 
	 * @return true, if messages have arrived
	 */
	public boolean hasMessages() {
		return pending.size() > 0;
	}

	@Override
	public void receiveMessage(Message message, Context c)
			throws MessageException {
		if (message.getRecipientId() != myAgent.getId())
			throw new MessageException("I'm not responsible for the receiver (something went very wrong)!");

		try {
			message.open(myAgent, c);

			// START
			// This part enables message answering for all messages that were sent to an (UserAgent) mediator.
			// Disable this section to reduce network traffic
			if (getMyNode() != null) { // This line is needed to allow the tests to work (since they do not have a
										// node..)
				try {
					Message response = new Message(message, "thank you");
					response.setSendingNodeId(getMyNode().getNodeId());
					getMyNode().sendMessage(response, null);
				} catch (EncodingFailedException e) {
					throw new MessageException("Unable to send response ", e);
				} catch (SerializationException e) {
					throw new MessageException("Unable to send response ", e);
				}
			}
			// END

		} catch (L2pSecurityException e) {
			throw new MessageException("Unable to open message because of security problems! ", e);
		} catch (AgentNotKnownException e) {
			throw new MessageException(
					"Sender unkown (since this is the receiver). Has the sending node gone offline? ", e);
		}

		if (!workOnMessage(message, c))
			pending.add(message);
	}

	/**
	 * Method for message reception treatment. Will call all registered {@link MessageHandler}s for message handling.
	 * 
	 * A return value of true indicates, that the received message has been treated by a MessageHandler and does not
	 * need further storage for later use (and will not be added to pending messages).
	 * 
	 * @param message
	 * @param context
	 * 
	 * @return true, if a message had been treated successfully
	 */
	public boolean workOnMessage(Message message, Context context) {
		for (int i = 0; i < registeredHandlers.size(); i++) {
			try {
				if (registeredHandlers.get(i).handleMessage(message, context))
					return true;
			} catch (Exception e) {
				runningAt.observerNotice(Event.MESSAGE_FAILED, runningAt.getNodeId(), this,
						"Exception in MessageHandler " + registeredHandlers.get(i) + ": " + e);
			}
		}
		return false;
	}

	/**
	 * Grants access to the node this Mediator is registered to.
	 * 
	 * @return the node this Mediator is running at
	 */
	protected Node getMyNode() {
		return runningAt;
	}

	@Override
	public long getResponsibleForAgentId() {
		return myAgent.getId();
	}

	@Override
	public void notifyRegistrationTo(Node node) {
		runningAt = node;
	}

	@Override
	public void notifyUnregister() {
		runningAt = null;
	}

	/**
	 * Invokes a service method (in the network) for the mediated agent.
	 * 
	 * @param service
	 * @param method
	 * @param parameters
	 * @param preferLocal if a local running service should be preferred
	 * 
	 * @return result of the method invocation
	 * 
	 * @throws L2pSecurityException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 * @throws L2pServiceException
	 * @throws AgentNotKnownException
	 */
	public Serializable invoke(String service, String method, Serializable[] parameters, boolean preferLocal)
			throws L2pSecurityException, InterruptedException, TimeoutException, AgentNotKnownException,
			L2pServiceException {
		if (runningAt.hasService(service)) {
			if (!preferLocal && runningAt.isBusy()) {
				// local node is not prefered and busy so we try global
				// this invocation may come back if there is no other node
				return runningAt.invokeGlobally(myAgent, service, method, parameters);
			} else {
				// local node is prefered or not busy and got the service
				// this means there is no need to produce networking overhead
				return runningAt.invokeLocally(myAgent.getId(), service, method, parameters);
			}
		} else {
			// service is not known locally => invokeGlobally
			return runningAt.invokeGlobally(myAgent, service, method, parameters);
		}
	}

	/**
	 * Gets the number of waiting messages.
	 * 
	 * @return number of waiting messages
	 */
	public int getNumberOfWaiting() {
		return pending.size();
	}

	/**
	 * Registers a MessageHandler for message processing.
	 * 
	 * Message handlers will be used for handling incoming messages in the order of registration.
	 * 
	 * @param handler
	 */
	public void registerMessageHandler(MessageHandler handler) {
		if (handler == null)
			throw new NullPointerException();

		if (registeredHandlers.contains(handler))
			return;

		registeredHandlers.add(handler);
	}

	/**
	 * Unregisters a handler from this mediator.
	 * 
	 * @param handler
	 */
	public void unregisterMessageHandler(MessageHandler handler) {
		registeredHandlers.remove(handler);
	}

	/**
	 * Unregisters all handlers of the given class.
	 * 
	 * @param cls
	 * 
	 * @return number of successfully removed message handlers
	 */
	public int unregisterMessageHandlerClass(@SuppressWarnings("rawtypes") Class cls) {
		int result = 0;

		Vector<MessageHandler> newHandlers = new Vector<MessageHandler>();

		for (int i = 0; i < registeredHandlers.size(); i++)
			if (!cls.isInstance(registeredHandlers.get(i))) {
				newHandlers.add(registeredHandlers.get(i));
			} else
				result++;

		registeredHandlers = newHandlers;

		return result;
	}

	/**
	 * Unregisters all handlers of the given class.
	 * 
	 * @param classname
	 * 
	 * @return number of successfully removed message handlers
	 */
	public int unregisterMessageHandlerClass(String classname) {
		try {
			return unregisterMessageHandlerClass(Class.forName(classname));
		} catch (Exception e) {
			// if the class cannot be found, there won't be any instances of it registered here...
			return 0;
		}
	}

	/**
	 * Is the given message handler registered at this mediator?
	 * 
	 * @param handler
	 * 
	 * @return true, if at least one message handler is registered to this mediator
	 */
	public boolean hasMessageHandler(MessageHandler handler) {
		return registeredHandlers.contains(handler);
	}

	/**
	 * Has this mediator a registered message handler of the given class?
	 * 
	 * @param cls
	 * @return true, if this mediator has a message handler of the given class
	 */
	public boolean hasMessageHandlerClass(@SuppressWarnings("rawtypes") Class cls) {
		for (MessageHandler handler : registeredHandlers)
			if (cls.isInstance(handler))
				return true;

		return false;
	}

}
