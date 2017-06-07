package i5.las2peer.connectors.nodeAdminConnector;

import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import com.sun.net.httpserver.HttpsServer;

import i5.las2peer.connectors.Connector;
import i5.las2peer.connectors.ConnectorException;
import i5.las2peer.connectors.nodeAdminConnector.handler.AgentsHandler;
import i5.las2peer.connectors.nodeAdminConnector.handler.AppHandler;
import i5.las2peer.connectors.nodeAdminConnector.handler.AuthHandler;
import i5.las2peer.connectors.nodeAdminConnector.handler.DefaultHandler;
import i5.las2peer.connectors.nodeAdminConnector.handler.RMIHandler;
import i5.las2peer.connectors.nodeAdminConnector.handler.ServicesHandler;
import i5.las2peer.connectors.nodeAdminConnector.handler.StatusHandler;
import i5.las2peer.connectors.nodeAdminConnector.handler.SwaggerUIHandler;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.security.PassphraseAgentImpl;

public class NodeAdminConnector extends Connector {

	private static final String KEYSTORE_FILENAME_PREFIX = "etc/NodeAdminConnector-";
	private static final String KEYSTORE_SECRET_FILENAME = "etc/keystore.secret";

	private static final L2pLogger logger = L2pLogger.getInstance(NodeAdminConnector.class);

	public static final int DEFAULT_PORT = 14577;
	private int port;
	public static final int DEFAULT_MAX_ACTIVE_CONNECTIONS = 20;
	private int maxActiveConnections;
	public static final int DEFAULT_MAX_WAITING_CONNECTIONS = 10;
	private int maxWaitingConnections;
	public static final int DEFAULT_SESSION_TIMEOUT = 24 * 60; // minutes = 24 hours
	private int sessionTimeout;
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
		setFieldValues();
	}

	@Override
	public synchronized void start(Node runningAt) throws ConnectorException {
		if (!(runningAt instanceof PastryNodeImpl)) {
			throw new ConnectorException("This connector is not compatible to your node type");
		}
		// usual connector start
		node = (PastryNodeImpl) runningAt;
		try {
			String hostname = node.getBindAddress().getHostName();
			https = HttpsServer.create(new InetSocketAddress(port), maxWaitingConnections);
			String keystoreSecret = getOrCreateSecretFromFile("keystore password", KEYSTORE_SECRET_FILENAME);
			https.setHttpsConfigurator(
					KeystoreManager.loadOrCreateKeystore(KEYSTORE_FILENAME_PREFIX, hostname, keystoreSecret));
			https.createContext("/", new DefaultHandler(this));
			https.createContext("/app", new AppHandler(this)).getFilters().add(new ParameterFilter());
			https.createContext("/status", new StatusHandler(this)).getFilters().add(new ParameterFilter());
			https.createContext("/services", new ServicesHandler(this)).getFilters().add(new ParameterFilter());
			https.createContext("/auth", new AuthHandler(this)).getFilters().add(new ParameterFilter());
			https.createContext("/agents", new AgentsHandler(this)).getFilters().add(new ParameterFilter());
			https.createContext("/swagger-ui", new SwaggerUIHandler(this)).getFilters().add(new ParameterFilter());
			https.createContext("/rmi", new RMIHandler(this)).getFilters().add(new ParameterFilter());
			https.setExecutor(Executors.newFixedThreadPool(maxActiveConnections));
			https.start();
			logger.info(NodeAdminConnector.class.getSimpleName() + " in HTTPS mode running on port " + port);
			logger.info("Please visit https://" + hostname + ":" + port + " to continue ...");
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
		// TODO start sessions cleanup thread
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

	public int getPort() {
		return port;
	}

	public AgentSession getOrCreateSession(PassphraseAgentImpl agent) {
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

	public AgentSession getSessionFromHeader(String headerValue) {
		if (headerValue == null || headerValue.isEmpty()) {
			return null;
		} else {
			synchronized (sessions) {
				AgentSession session = sessions.get(headerValue);
				if (session != null) {
					// check if session is not timed out
					Calendar cal = Calendar.getInstance();
					cal.add(Calendar.MINUTE, -sessionTimeout);
					if (session.getLastActive().after(cal.getTime())) { // session is still active
						session.touch();
					} else { // timed out, destroy session
						logger.info("Destroying timed out session " + session.getSessionId());
						destroySession(session.getSessionId());
						session = null;
					}
				}
				return session;
			}
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
