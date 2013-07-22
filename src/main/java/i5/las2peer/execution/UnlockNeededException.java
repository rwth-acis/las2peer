package i5.las2peer.execution;

import java.security.PublicKey;

/**
 * Exception thrown in a global invocation, if the remote service tries to open an
 * envelope and is not able to do this, because the private key of the
 * executing agent is not unlocked.
 * 
 * Therefore a Mediator is needed at the foreign node.
 * 
 * @author Holger Jan&szlig;en
 *
 */
public class UnlockNeededException extends L2pServiceException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -254890419510880027L;
	private Object remoteNode;
	private PublicKey nodeKey;
	
	/**
	 * 
	 * @param message
	 * @param remoteNode
	 */
	public UnlockNeededException(String message, Object remoteNode, PublicKey nodeKey ) {
		super(message);
		
		if ( remoteNode == null )
			throw new NullPointerException ( "null not allowed as node handle!" );
		
		this.remoteNode = remoteNode;
		this.nodeKey = nodeKey;
	}
	

	/**
	 * get the handle of the remote to log in to
	 * @return handle of the remote node (where the exception started)
	 */
	public Object getRemoteNode() { return remoteNode; }
	
	/**
	 * get the public key of the foreign node to encrypt the unlocing passphrase to
	 * @return a public key
	 */
	public PublicKey getNodeKey () { return nodeKey; }
	

}
