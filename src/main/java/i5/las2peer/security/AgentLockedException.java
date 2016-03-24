package i5.las2peer.security;

public class AgentLockedException extends L2pSecurityException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2089410011983336665L;

	public AgentLockedException() {
		super("the main agent of this thread is locked - please unlock to open envelopes!");
	}
	
	public AgentLockedException(String err) {
		super(err);
	}

}
