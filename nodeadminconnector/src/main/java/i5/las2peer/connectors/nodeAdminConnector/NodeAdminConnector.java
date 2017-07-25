package i5.las2peer.connectors.nodeAdminConnector;

import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import com.sun.net.httpserver.HttpServer;

import i5.las2peer.connectors.Connector;
import i5.las2peer.connectors.ConnectorException;
import i5.las2peer.connectors.nodeAdminConnector.handler.AgentsHandler;
import i5.las2peer.connectors.nodeAdminConnector.handler.WebappHandler;
import i5.las2peer.connectors.nodeAdminConnector.handler.AuthHandler;
import i5.las2peer.connectors.nodeAdminConnector.handler.DefaultHandler;
import i5.las2peer.connectors.nodeAdminConnector.handler.RMIHandler;
import i5.las2peer.connectors.nodeAdminConnector.handler.ServicesHandler;
import i5.las2peer.connectors.nodeAdminConnector.handler.SwaggerUIHandler;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.restMapper.GenericExceptionMapper;
import i5.las2peer.security.PassphraseAgentImpl;
import i5.las2peer.tools.SimpleTools;

public class NodeAdminConnector extends Connector {

	public static final String COOKIE_SESSIONID_KEY = "sessionid";
	// other context names, see
	// https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#SSLContext
	public static final String SSL_INSTANCE_NAME = "TLSv1.2";

	private static final String KEYSTORE_DIRECTORY = "etc/";

	private static final L2pLogger logger = L2pLogger.getInstance(NodeAdminConnector.class);

	public static final int DEFAULT_PORT = 14577;
	private int port;
	public static final int DEFAULT_MAX_ACTIVE_CONNECTIONS = 20;
	private int maxActiveConnections;
	public static final int DEFAULT_SESSION_TIMEOUT = 24 * 60; // minutes = 24 hours
	private int sessionTimeout;
	private final SecureRandom secureRandom;
	private PastryNodeImpl node;
	private HttpServer httpServer;
	private X509Certificate caCert;
	private X509Certificate cert;
	private boolean persistentKeystore;
	private final HashMap<String, String> agentIdToSessionId;
	private final HashMap<String, AgentSession> sessions;

	public NodeAdminConnector() {
		this(DEFAULT_PORT, DEFAULT_MAX_ACTIVE_CONNECTIONS, DEFAULT_SESSION_TIMEOUT, true);
	}

	public NodeAdminConnector(Integer port, boolean persistentKeystore) {
		this(port, DEFAULT_MAX_ACTIVE_CONNECTIONS, DEFAULT_SESSION_TIMEOUT, persistentKeystore);
	}

	public NodeAdminConnector(Integer port, int maxActiveConnections, int sessionTimeout, boolean persistentKeystore) {
		if (port == null || port < 1) {
			this.port = SimpleTools.getSystemDefinedPort();
		} else {
			this.port = port;
		}
		this.maxActiveConnections = maxActiveConnections;
		this.sessionTimeout = sessionTimeout;
		this.secureRandom = new SecureRandom();
		this.persistentKeystore = persistentKeystore;
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
			ResourceConfig config = new ResourceConfig();
			config.register(GenericExceptionMapper.class);
			config.register(JacksonFeature.class);
			config.register(MultiPartFeature.class);
			config.property("jersey.config.server.wadl.disableWadl", true);
			config.register(CORSResponseFilter.class);
			config.register(new DefaultHandler(this));
			config.register(new WebappHandler(this));
			config.register(new AuthHandler(this));
			config.register(new ServicesHandler(this));
			config.register(new AgentsHandler(this));
			config.register(new SwaggerUIHandler(this));
			config.register(new RMIHandler(this));
			String myHostname = node.getBindAddress().getHostName();
			char[] keystoreSecret;
			if (persistentKeystore) {
				keystoreSecret = getOrCreateSecretFromFile("keystore password",
						KEYSTORE_DIRECTORY + "NodeAdminConnector-" + myHostname + ".secret").toCharArray();
			} else {
				keystoreSecret = generateToken().toCharArray();
			}
			KeyStore keystore = KeystoreManager.loadOrCreateKeystore(KEYSTORE_DIRECTORY + "NodeAdminConnector-",
					myHostname, keystoreSecret, persistentKeystore);
			caCert = (X509Certificate) keystore.getCertificate(NodeAdminConnector.class.getSimpleName() + " Root CA");
			cert = (X509Certificate) keystore.getCertificate(NodeAdminConnector.class.getSimpleName());
			if (persistentKeystore) {
				// export CA certificate to file, overwrite existing
				KeystoreManager.writeCertificateToPEMFile(caCert, KEYSTORE_DIRECTORY + myHostname + " Root CA.pem");
			}
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(keystore, keystoreSecret);
			SSLContext sslContext = SSLContext.getInstance(SSL_INSTANCE_NAME);
			sslContext.init(kmf.getKeyManagers(), null, null);
			httpServer = JdkHttpServerFactory.createHttpServer(new URI(getHostname() + "/"), config, sslContext, false);
			httpServer.setExecutor(Executors.newFixedThreadPool(maxActiveConnections));
			httpServer.start();
			logger.info(NodeAdminConnector.class.getSimpleName() + " in HTTPS mode running on port " + port);
			logger.info("Please visit " + getHostname() + " to continue ...");
		} catch (Exception e) {
			if (httpServer != null) {
				// try to cleanup mess
				try {
					httpServer.stop(0);
				} catch (Exception e2) {
					logger.log(Level.SEVERE, "HTTPS server cleanup failed after failed connector start", e2);
				} finally {
					httpServer = null;
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
		if (httpServer != null) {
			synchronized (sessions) {
				httpServer.stop(0);
				httpServer = null;
				caCert = null;
				agentIdToSessionId.clear();
				sessions.clear();
			}
		}
		node = null;
	}

	public PastryNodeImpl getNode() {
		return node;
	}

	public int getPort() {
		return port;
	}

	public X509Certificate getCACertificate() {
		return caCert;
	}

	public X509Certificate getCertificate() {
		return cert;
	}

	public String getHostname() {
		return "https://" + node.getBindAddress().getHostName() + ":" + port;
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

	public AgentSession getSessionById(String sessionid) {
		if (sessionid == null || sessionid.isEmpty()) {
			return null;
		} else {
			synchronized (sessions) {
				AgentSession session = sessions.get(sessionid);
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
		if (sessionId == null || sessionId.isEmpty()) {
			return;
		}
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
