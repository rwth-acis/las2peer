package i5.las2peer.communication;

import java.io.Serializable;

/**
 * a simple content class for a {@link Message} indicating a successful execution of an remote invocation task
 * 
 * 
 *
 */
public class RMIResultContent implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4804271575347018920L;
	private Serializable content;

	public RMIResultContent(Serializable content) {
		this.content = content;
	}

	public Serializable getContent() {
		return content;
	}

}
