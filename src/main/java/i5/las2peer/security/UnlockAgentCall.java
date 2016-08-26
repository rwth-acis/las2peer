package i5.las2peer.security;

import java.io.Serializable;

/**
 * a Message that tells the receiving Agent to unlock the calling Agent and contains
 * another Object
 *
 */
public class UnlockAgentCall implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1596043806795793603L;
	
	private Object content;
	
	private String passphrase;
	
	public UnlockAgentCall(Object content, String passphrase) {
		this.content = content;
		this.passphrase = passphrase;
	}
	
	public String getPassphrase() {
		return passphrase;
	}
	
	public Object getContent() {
		return content;
	}
}
