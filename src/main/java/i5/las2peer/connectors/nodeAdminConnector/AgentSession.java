package i5.las2peer.connectors.nodeAdminConnector;

import java.util.Date;

import i5.las2peer.security.UserAgentImpl;

public class AgentSession {

	private final String sessionId;
	private final UserAgentImpl agent;
	private Date lastActive;

	public AgentSession(String sessionId, UserAgentImpl agent) {
		this.sessionId = sessionId;
		this.agent = agent;
		lastActive = new Date();
	}

	public String getSessionId() {
		return sessionId;
	}

	public UserAgentImpl getAgent() {
		return agent;
	}

	public Date getLastActive() {
		return lastActive;
	}

	public void touch() {
		lastActive = new Date();
	}

}
