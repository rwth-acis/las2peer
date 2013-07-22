package i5.las2peer.security;

import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.execution.ServiceInvocationException;
import i5.las2peer.execution.UnlockNeededException;
import i5.las2peer.mobsos.NodeObserver.Event;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.TimeoutException;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Vector;

/**
 * A Mediator acts on behalf of an {@link PassphraseAgent}. This necessary e.g. for remote 
 * users logged in via a {@link i5.las2peer.api.Connector} to collect incoming messages from the
 * P2P network and transfer it to the connector. 
 * 
 * @author Holger Jan&szlig;en
 *
 */
public class Mediator implements MessageReceiver {

	private LinkedList<Message> pending = new LinkedList<Message>();
	
	private Agent myAgent;

	private Node runningAt = null;
	
	private Vector<MessageHandler> registeredHandlers = new Vector<MessageHandler> ();
	
	
	/**
	 * create a new Mediator
	 * 
	 * @param a
	 * @throws L2pSecurityException 
	 */
	public Mediator ( Agent a ) throws L2pSecurityException {
		if ( a.isLocked())
			throw new L2pSecurityException ("You need to unlock the private key of the agent for mediating.");
		
		myAgent = a;
	}
	
	

	/**
	 * get (and remove) the next pending message
	 * @return	next collected message
	 */
	public Message getNextMessage() {
		if (pending.size() == 0) return null;
		
		return pending.pollFirst();
	}
	
	/**
	 * Does this mediator have pending messages?
	 * @return true, if messages have arrived
	 */
	public boolean hasMessages() {
		return pending.size() > 0;
	}
	
	@Override
	public void receiveMessage(Message message, Context c)
			throws MessageException {
		if ( message.getRecipientId() != myAgent.getId())
			throw new MessageException ( "I'm not responsible for the receiver!?!");
		
		try {
			message.open(myAgent, c);
		} catch (L2pSecurityException e) {
			throw new MessageException ( "Unable to open message because of security problems?!", e);
		} catch (AgentNotKnownException e) {
			throw new MessageException ( "Sender unkown?!?, e");
		}
		
		if ( ! workOnMessage ( message, c ))
			pending.add( message );
	}
	
	
	
	/**
	 * stub method for message reception treatment.
	 * 
	 * Subclasses may implement functionality by overriding this method.
	 * 
	 * A return value of true indicates, that the received message has been treated an 
	 * does not need further storage for later use.
	 *  
	 * @param message
	 * @param context
	 * 
	 * @return true, of a message had been treated successfully
	 */
	public boolean workOnMessage( Message message, Context context ) {
		
		for ( int i=0; i<registeredHandlers.size(); i++ )
			try {
				if ( registeredHandlers.get(i).handleMessage(message, context))
					return true;
			} catch (Exception e) {
				runningAt.observerNotice(Event.MESSAGE_FAILED, "exception in MessageHandler " + registeredHandlers.get(i) + ": " + e );
			}
		
		return false;
	}
	
	/**
	 * access to the node this Mediator is registered to
	 * @return the node this Mediator is running at
	 */
	protected Node getMyNode () {
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
	 * invoke a service method (in the network) for the mediated agent
	 * 
	 * @param service
	 * @param method
	 * @param parameters
	 * 
	 * @return result of the method invocation
	 * 
	 * @throws L2pSecurityException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 * @throws ServiceInvocationException
	 * @throws UnlockNeededException
	 */
	public Serializable invoke ( String service, String method, Serializable[] parameters, boolean preferLocal ) throws L2pSecurityException, InterruptedException, TimeoutException, ServiceInvocationException, UnlockNeededException {
		if ( preferLocal && runningAt.hasService ( service ) )
			try {
				return runningAt.invokeLocally(myAgent.getId(), service,  method, parameters);
			} catch ( Exception e ) {
				// just try globally 
				//e.printStackTrace();
				System.out.println ( "Local access to service " + service + " failed - trying globally");
			}
			
		return runningAt.invokeGlobally(myAgent, service, method, parameters);
	}



	/**
	 * get the number of waiting messages
	 * @return number of waiting messages
	 */
	public int getNumberOfWaiting() {
		return pending.size();
	}

	
	/**
	 * Register a MessageHandler for message processing.
	 * 
	 * Message handlers will be used for handling incoming messages in the order of 
	 * registration.
	 * 
	 * @param handler
	 */
	public void registerMessageHandler( MessageHandler handler ) {
		if ( handler == null)
			throw new NullPointerException ();
		
		if ( registeredHandlers.contains(handler))
			return;
		
		registeredHandlers.add( handler );
	}
	
	
	/**
	 * unregister a handler from this mediator
	 * 
	 * @param handler
	 */
	public void unregisterMessageHandler ( MessageHandler handler ) {
		registeredHandlers.remove(handler);
	}
	

	/**
	 * unregister all handlers of the given class
	 * 
	 * @param cls
	 * @return number of successfully removed message handlers 
	 */
	public int unregisterMessageHandlerClass ( @SuppressWarnings("rawtypes") Class cls) {
		int result = 0;

		Vector<MessageHandler> newHandlers = new Vector<MessageHandler> ();
		
		for ( int i=0; i<registeredHandlers.size(); i++)
			if ( ! cls.isInstance( registeredHandlers.get(i) )  ) {
				newHandlers.add( registeredHandlers.get(i));
			} else
				result ++;
		
		registeredHandlers = newHandlers;
		
		return result;
	}
	
	/**
	 * unregister all handlers of the given class
	 * 
	 * @param classname
	 * 
	 * @return number of successfully removed message handlers
	 */
	public int unregisterMessageHandlerClass ( String classname ) {
		try {
			return unregisterMessageHandlerClass ( Class.forName( classname ));
		} catch (Exception e) {
			// if the class cannot be found, there won't be any instances of it registered here...
			return 0;
		}
	}
	
	/**
	 * Is the given message handler registered at this mediator?
	 * @param handler
	 * @return true, if at least one message handler is registered to this mediator
	 */
	public boolean hasMessageHandler ( MessageHandler handler ) {
		return registeredHandlers.contains( handler );
	}
	
	/**
	 * Has this mediator a registered message handler of the given class?
	 * 
	 * @param cls
	 * @return true, if this mediator has a message handler of the given class
	 */
	public boolean hasMessageHandlerClass ( @SuppressWarnings("rawtypes") Class cls) {
		for ( MessageHandler handler : registeredHandlers)
			if ( cls.isInstance( handler ))
				return true;
		
		return false;
	}
	
}
