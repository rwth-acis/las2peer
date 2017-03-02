package i5.las2peer.persistency;

import i5.las2peer.api.persistency.Envelope;
import i5.las2peer.api.security.Agent;
import i5.las2peer.security.AgentContext;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

// TODO refactor storage
// EnvelopeVersion should contain a reader list and should already be opened,
// move dealing with EnvelopeVersions to lower level

public class EnvelopeImpl implements Envelope {
	private String identifier;
	private Serializable content;

	private Set<AgentImpl> readerToAdd = new HashSet<>();
	private Set<AgentImpl> readerToRevoke = new HashSet<>();
	private boolean revokeAllReaders = false;
	private String signingAgentId;

	private EnvelopeVersion currentVersion;

	public EnvelopeImpl(String identifier, AgentImpl signingAgent) {
		this.identifier = identifier;
		this.content = null;
		this.currentVersion = null;
		this.signingAgentId = signingAgent.getIdentifier();
		this.readerToAdd.add(signingAgent);
	}

	public EnvelopeImpl(EnvelopeVersion currentVersion, AgentContext context) throws CryptoException,
			L2pSecurityException, SerializationException {
		this.identifier = currentVersion.getIdentifier();
		this.content = currentVersion.getContent(context.getMainAgent(), context);
		this.currentVersion = currentVersion;
		this.signingAgentId = null; // TODO API get signing agent
	}

	@Override
	public String getIdentifier() {
		return this.identifier;
	}

	@Override
	public Serializable getContent() {
		return content;
	}

	@Override
	public void setContent(Serializable content) {
		this.content = content;

	}

	@Override
	public void addReader(Agent agent) {
		this.readerToAdd.add((AgentImpl) agent);
		this.readerToRevoke.remove(agent);
	}

	@Override
	public void revokeReader(Agent agent) {
		this.readerToAdd.remove(agent);
		this.readerToRevoke.add((AgentImpl) agent);
	}

	@Override
	public boolean hasReader(Agent agent) {
		return (currentVersion != null
				&& currentVersion.getReaderKeys().containsKey(((AgentImpl) agent).getPublicKey()) || readerToAdd
					.contains(agent)) && !readerToRevoke.contains(agent) && !revokeAllReaders;
	}

	@Override
	public void setPublic() {
		revokeAllReaders = true;
		readerToAdd.clear();
		readerToRevoke.clear();
	}

	@Override
	public boolean isPrivate() {
		return (currentVersion == null && !readerToAdd.isEmpty())
				|| (currentVersion != null && currentVersion.isEncrypted() && !revokeAllReaders)
				|| (currentVersion != null && !currentVersion.isEncrypted() && !readerToAdd.isEmpty());
	}

	public Set<AgentImpl> getReaderToAdd() {
		return readerToAdd;
	}

	public Set<AgentImpl> getReaderToRevoke() {
		return readerToRevoke;
	}

	public boolean getRevokeAllReaders() {
		return revokeAllReaders;
	}
	
	public EnvelopeVersion getVersion() {
		return this.currentVersion;
	}
	
	public void setVersion(EnvelopeVersion v) {
		this.currentVersion = v;
		this.revokeAllReaders = false;
		this.readerToAdd.clear();
		this.readerToRevoke.clear();
	}
	
	public String getSigningAgentId() {
		return this.signingAgentId;
	}

}
