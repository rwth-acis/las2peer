package i5.las2peer.security;

import java.io.UnsupportedEncodingException;

import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.ArtifactNotFoundException;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.StorageException;
import i5.las2peer.persistency.DecodingFailedException;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.persistency.EnvelopeException;
import i5.las2peer.tools.SerializationException;

/**
 * Maps usernames and emails to {@link UserAgent}s.
 *
 */
public class UserAgentManager {
	
	private static final String PREFIX_USER_NAME = "USER_NAME-";
	private static final String PREFIX_USER_MAIL = "USER_MAIL-";
	
	private Node node; 
	
	public UserAgentManager(Node node) {
		this.node = node;
	}
	
	/**
	 * Stores login name and email of an user agent to the network
	 * 
	 * @param agent an unlocked UserAgent
	 * @throws DuplicateEmailException
	 * @throws DuplicateLoginNameException
	 * @throws AgentLockedException
	 */
	public void registerUserAgent(UserAgent agent) throws DuplicateEmailException, DuplicateLoginNameException, AgentLockedException {
		if (agent.isLocked())
			throw new AgentLockedException("Only unlocked Agents can be registered!");
		
		Long content = agent.getId();
		
		if (agent.hasLogin()) {
			try {
				Envelope envName = Envelope.createClassIdEnvelope(content, PREFIX_USER_NAME + agent.getLoginName().toLowerCase(), agent);
				envName.open(agent);
				envName.setOverWriteBlindly(true);
				envName.addReader(node.getAnonymous());
				envName.addSignature(agent);
				node.storeArtifact(envName);
				envName.close();
			} catch (L2pSecurityException e) {
				throw new DuplicateLoginNameException();
			} catch (UnsupportedEncodingException | EncodingFailedException
					| SerializationException | StorageException | DecodingFailedException  e) {
				node.observerNotice(Event.NODE_ERROR, "Envelope error while updating user list: " + e);
			}
		}
		
		if (agent.hasEmail()) {
			try {
				Envelope envMail = Envelope.createClassIdEnvelope(content, PREFIX_USER_MAIL + agent.getEmail().toLowerCase(), agent);
				envMail.open(agent);
				envMail.setOverWriteBlindly(true);
				envMail.addReader(node.getAnonymous());
				envMail.addSignature(agent);
				node.storeArtifact(envMail);
				envMail.close();
			} catch (L2pSecurityException e) {
				throw new DuplicateEmailException();
			} catch (UnsupportedEncodingException | EncodingFailedException
					| SerializationException | StorageException | DecodingFailedException e) {
				node.observerNotice(Event.NODE_ERROR, "Envelope error while updating user list: " + e);
			}
		}
	}
	
	/**
	 * updates login name and email of an user
	 * @param agent
	 * @throws AgentLockedException
	 * @throws DuplicateEmailException 
	 * @throws DuplicateLoginNameException 
	 */
	public void updateUserAgent(UserAgent agent) throws AgentLockedException, DuplicateEmailException, DuplicateLoginNameException {
		registerUserAgent(agent);
	}
	
	/**
	 * get an {@link UserAgent}'s id by login name
	 * @param name
	 * @return
	 * @throws AgentNotKnownException
	 */
	public long getAgentIdByLogin(String name) throws AgentNotKnownException {
		try {
			Envelope env = node.fetchArtifact(Envelope.getClassEnvelopeId(Long.class, PREFIX_USER_NAME + name.toLowerCase()));
			env.open(node.getAnonymous());
			Long content = env.getContent(Long.class);
			env.close();
			
			return content;
		} catch (StorageException | ArtifactNotFoundException | EnvelopeException | L2pSecurityException e) {
			throw new AgentNotKnownException("Username not found!", e);
		}
	}
	
	/**
	 * get an {@link UserAgent}'s id by email address
	 * @param email
	 * @return
	 * @throws AgentNotKnownException
	 */
	public long getAgentIdByEmail(String email) throws AgentNotKnownException {
		try {
			Envelope env = node.fetchArtifact(Envelope.getClassEnvelopeId(Long.class, PREFIX_USER_MAIL + email.toLowerCase()));
			env.open(node.getAnonymous());
			Long content = env.getContent(Long.class);
			env.close();
			
			return content;
		} catch (StorageException | ArtifactNotFoundException | EnvelopeException | L2pSecurityException e) {
			throw new AgentNotKnownException("Email not found!", e);
		}
	}
}
