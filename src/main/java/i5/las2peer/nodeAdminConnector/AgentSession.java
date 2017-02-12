package i5.las2peer.nodeAdminConnector;

import java.util.Date;

import i5.las2peer.security.UserAgent;

public class AgentSession {

	private final String sessionId;
	private final UserAgent agent;
	private Date lastActive;

	public AgentSession(String sessionId, UserAgent agent) {
		this.sessionId = sessionId;
		this.agent = agent;
		lastActive = new Date();
	}

	public String getSessionId() {
		return sessionId;
	}

	public UserAgent getAgent() {
		return agent;
	}

	public Date getLastActive() {
		return lastActive;
	}

	public void touch() {
		lastActive = new Date();
	}

}
