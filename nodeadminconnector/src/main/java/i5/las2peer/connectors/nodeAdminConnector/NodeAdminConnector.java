package i5.las2peer.connectors.nodeAdminConnector;

import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpsServer;

import i5.las2peer.connectors.Connector;
import i5.las2peer.connectors.ConnectorException;
import i5.las2peer.connectors.nodeAdminConnector.handler.DefaultHandler;
import i5.las2peer.connectors.nodeAdminConnector.handler.FrontendHandler;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.security.UserAgentImpl;

public class NodeAdminConnector extends Connector {

	private static final String ADMIN_SECRET_FILENAME = "etc/webadmin.secret";
	private static final String KEYSTORE_FILENAME = "etc/NodeAdminConnector.jks";
	private static final String KEYSTORE_SECRET_FILENAME = "etc/keystore.secret";

	private static final L2pLogger logger = L2pLogger.getInstance(NodeAdminConnector.class);

	public static final int DEFAULT_PORT = 14577;
	private final int port;
	public static final int DEFAULT_MAX_ACTIVE_CONNECTIONS = 20;
	private final int maxActiveConnections;
	public static final int DEFAULT_MAX_WAITING_CONNECTIONS = 10;
	private final int maxWaitingConnections;
	public static final int DEFAULT_SESSION_TIMEOUT = 24 * 60; // minutes = 24 hours
	private final int sessionTimeout;
	private final SecureRandom secureRandom;
	private PastryNodeImpl node;
	private HttpsServer https;
	private final HashMap<String, String> agentIdToSessionId;
	private final HashMap<String, AgentSession> sessions;

	public NodeAdminConnector() {
		this(DEFAULT_PORT, DEFAULT_MAX_ACTIVE_CONNECTIONS, DEFAULT_MAX_WAITING_CONNECTIONS, DEFAULT_SESSION_TIMEOUT);
	}

	public NodeAdminConnector(int port) {
		this(port, DEFAULT_MAX_ACTIVE_CONNECTIONS, DEFAULT_MAX_WAITING_CONNECTIONS, DEFAULT_SESSION_TIMEOUT);
	}

	public NodeAdminConnector(int port, int maxActiveConnections, int maxWaitingConnections, int sessionTimeout) {
		this.port = port;
		this.maxActiveConnections = maxActiveConnections;
		this.maxWaitingConnections = maxWaitingConnections;
		this.sessionTimeout = sessionTimeout;
		this.secureRandom = new SecureRandom();
		this.agentIdToSessionId = new HashMap<>();
		this.sessions = new HashMap<>();
	}

	@Override
	public synchronized void start(Node runningAt) throws ConnectorException {
		if (!(runningAt instanceof PastryNodeImpl)) {
			throw new ConnectorException("This connector is not compatible to your node type");
		}
		// usual connector start
		node = (PastryNodeImpl) runningAt;
		try {
			https = HttpsServer.create(new InetSocketAddress(port), maxWaitingConnections);
			https.setHttpsConfigurator(KeystoreManager.loadOrCreateKeystore(KEYSTORE_FILENAME,
					getOrCreateSecretFromFile("keystore password", KEYSTORE_SECRET_FILENAME)));
			https.createContext("/", new DefaultHandler(this));
			HttpContext frontendContext = https.createContext("/" + FrontendHandler.ROOT_NAME,
					new FrontendHandler(this, getOrCreateSecretFromFile("webadmin token", ADMIN_SECRET_FILENAME)));
			frontendContext.getFilters().add(new ParameterFilter());
			https.setExecutor(Executors.newFixedThreadPool(maxActiveConnections));
			https.start();
			logger.info(NodeAdminConnector.class.getSimpleName() + " in HTTPS mode running on port " + port);
			logger.info(
					"Please visit https://" + node.getBindAddress().getHostAddress() + ":" + port + " to continue ...");
		} catch (Exception e) {
			if (https != null) {
				// try to cleanup mess
				try {
					https.stop(0);
				} catch (Exception e2) {
					logger.log(Level.SEVERE, "HTTPS server cleanup failed after failed connector start", e2);
				} finally {
					https = null;
				}
			}
			throw new ConnectorException("Connector start failed", e);
		}
		// FIXME start sessions cleanup thread
	}

	private String getOrCreateSecretFromFile(String passwordName, String filename) {
		String result = null;
		File secretFile = new File(filename);
		if (!secretFile.exists()) { // create new token
			result = generateToken();
			try {
				File parent = secretFile.getParentFile();
				if (parent != null) {
					parent.mkdirs();
				}
				FileWriter fw = new FileWriter(secretFile);
				fw.write(result);
				fw.close();
				logger.info("Generated " + passwordName + " in '" + filename + "'");
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Could not store " + passwordName + " in " + secretFile.getAbsolutePath(), e);
			}
		} else { // read token from file
			try {
				result = new String(Files.readAllBytes(secretFile.toPath()));
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Could not read " + passwordName + " from " + secretFile.getAbsolutePath(), e);
			}
		}
		return result;
	}

	public String generateToken() {
		// src: https://stackoverflow.com/a/41156
		return new BigInteger(260, secureRandom).toString(32);
	}

	@Override
	public synchronized void stop() throws ConnectorException {
		if (https != null) {
			https.stop(0);
			https = null;
		}
		node = null;
	}

	public PastryNodeImpl getNode() {
		return node;
	}

	public AgentSession getOrCreateSession(UserAgentImpl agent) {
		synchronized (sessions) {
			final String agentId = agent.getIdentifier();
			String sessionId = agentIdToSessionId.get(agentId);
			if (sessionId == null) {
				sessionId = generateToken();
				agentIdToSessionId.put(agentId, sessionId);
			}
			AgentSession agentSession = sessions.get(sessionId);
			if (agentSession == null) {
				agentSession = new AgentSession(sessionId, agent);
				sessions.put(sessionId, agentSession);
			}
			agentSession.touch();
			return agentSession;
		}
	}

	public AgentSession getSessionFromCookies(List<String> cookies) {
		synchronized (sessions) {
			if (cookies != null) {
				for (String pairs : cookies) {
					for (String cookie : pairs.split(";")) {
						cookie = cookie.trim();
						int equalPos = cookie.indexOf("=");
						if (equalPos > 0) {
							String key = cookie.substring(0, equalPos).trim();
							String value = cookie.substring(equalPos + 1).trim();
							if (key.equalsIgnoreCase("agent-session-id")) {
								AgentSession session = sessions.get(value);
								if (session != null) {
									Calendar cal = Calendar.getInstance();
									cal.add(Calendar.MINUTE, -sessionTimeout);
									if (session.getLastActive().after(cal.getTime())) {
										// session is still active
										session.touch();
										return session;
									}
								}
							}
						}
					}
				}
			}
			return null;
		}
	}

	public void destroySession(String sessionId) {
		synchronized (sessions) {
			AgentSession removed = sessions.remove(sessionId);
			if (removed != null) {
				if (agentIdToSessionId.remove(removed.getAgent().getIdentifier()) == null) {
					logger.warning(
							"Session " + sessionId + " destroyed, but did not find agent in agentid to sessionid map");
				}
			}
		}
	}

}
