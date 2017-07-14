package i5.las2peer.connectors.nodeAdminConnector;

import java.util.Date;

import i5.las2peer.security.PassphraseAgentImpl;

public class AgentSession {

	private final String sessionId;
	private final PassphraseAgentImpl agent;
	private Date lastActive;

	public AgentSession(String sessionId, PassphraseAgentImpl agent) {
		this.sessionId = sessionId;
		this.agent = agent;
		lastActive = new Date();
	}

	public String getSessionId() {
		return sessionId;
	}

	public PassphraseAgentImpl getAgent() {
		return agent;
	}

	public Date getLastActive() {
		return lastActive;
	}

	public void touch() {
		lastActive = new Date();
	}

}
