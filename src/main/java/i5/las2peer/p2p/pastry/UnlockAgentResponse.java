package i5.las2peer.p2p.pastry;

import rice.p2p.commonapi.Message;

public class UnlockAgentResponse implements Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6417868965128084833L;

	private Exception storedException = null;
	
	private long responseToId;
	
	
	/**
	 * create a new respone indicating success
	 * @param responseTo
	 */
	public UnlockAgentResponse( long responseTo ) {
		responseToId = responseTo;
	}
	
	/**
	 * create a new response indicating failure
	 * @param responseTo
	 * @param e
	 */
	public UnlockAgentResponse ( long responseTo, Exception e ) {
		this ( responseTo);
		storedException = e;
	}
	
	
	/**
	 * 
	 * @return true, if an exception is stored here
	 */
	public boolean hasException () { return storedException != null; }
	
	
	/**
	 * 
	 * @return the (possibly) stored exception
	 */
	public Exception getException () { return storedException; }
	
	/**
	 * 
	 * @return id of the message, this one is a response to
	 */
	public long getOriginalMessageId ()  { return responseToId; }
	
	
	
	
	
	@Override
	public int getPriority() {
		// TODO Auto-generated method stub
		return 0;
	}

}
