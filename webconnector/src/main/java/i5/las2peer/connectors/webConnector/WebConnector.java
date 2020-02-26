package i5.las2peer.connectors.webConnector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.StreamHandler;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;

import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.connectors.Connector;
import i5.las2peer.connectors.ConnectorException;
import i5.las2peer.connectors.webConnector.handler.AgentsHandler;
import i5.las2peer.connectors.webConnector.handler.AuthHandler;
import i5.las2peer.connectors.webConnector.handler.DefaultHandler;
import i5.las2peer.connectors.webConnector.handler.EthereumHandler;
import i5.las2peer.connectors.webConnector.handler.ServicesHandler;
import i5.las2peer.connectors.webConnector.handler.SwaggerUIHandler;
import i5.las2peer.connectors.webConnector.handler.WebappHandler;
import i5.las2peer.connectors.webConnector.util.AgentSession;
import i5.las2peer.connectors.webConnector.util.AuthenticationManager;
import i5.las2peer.connectors.webConnector.util.CORSResponseFilter;
import i5.las2peer.connectors.webConnector.util.KeystoreManager;
import i5.las2peer.connectors.webConnector.util.NameLock;
import i5.las2peer.connectors.webConnector.util.WebConnectorExceptionMapper;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.PassphraseAgentImpl;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

/**
 * Starter class for registering the Web Connector at the las2peer server.
 *
 */
public class WebConnector extends Connector {

	/* configuration parameters */
	public static final int DEFAULT_HTTP_PORT = 8080;
	public static final String WEB_CONNECTOR = "WebConnector: ";
	protected int httpPort = DEFAULT_HTTP_PORT;

	public static final int DEFAULT_HTTPS_PORT = 8090;
	protected int httpsPort = DEFAULT_HTTPS_PORT;

	public static final boolean DEFAULT_START_HTTP = true;
	protected boolean startHttp = DEFAULT_START_HTTP;

	public static final boolean DEFAULT_START_HTTPS = false;
	protected boolean startHttps = DEFAULT_START_HTTPS;

	private static final String KEYSTORE_DIRECTORY = "etc/";

	// default: sslKeystore = "etc/WebConnector-{{hostname}}.jks"
	protected String sslKeystore;

	// default: stored and generated in file "etc/WebConnector-{{hostname}}.secret"
	protected String sslKeyPassword;

	public static final String DEFAULT_CROSS_ORIGIN_RESOURCE_DOMAIN = "*";
	protected String crossOriginResourceDomain = DEFAULT_CROSS_ORIGIN_RESOURCE_DOMAIN;

	public static final int DEFAULT_CROSS_ORIGIN_RESOURCE_MAX_AGE = 60;
	protected int crossOriginResourceMaxAge = DEFAULT_CROSS_ORIGIN_RESOURCE_MAX_AGE;

	public static final boolean DEFAULT_ENABLE_CROSS_ORIGIN_RESOURCE_SHARING = true;
	protected boolean enableCrossOriginResourceSharing = DEFAULT_ENABLE_CROSS_ORIGIN_RESOURCE_SHARING;

	public static final boolean DEFAULT_ONLY_LOCAL_SERVICES = false;
	protected boolean onlyLocalServices = DEFAULT_ONLY_LOCAL_SERVICES;

    public static final String SESSION_COOKIE = "cookie";

	public static final String DEFAULT_DEFAULT_OIDC_PROVIDER = "https://api.learning-layers.eu/o/oauth2";
	public String defaultOIDCProvider = DEFAULT_DEFAULT_OIDC_PROVIDER;
	public ArrayList<String> oidcProviders = new ArrayList<>();

	{
		oidcProviders.add(DEFAULT_DEFAULT_OIDC_PROVIDER);
	}

	protected String oidcClientId;

	protected String oidcClientSecret;

	public static final int DEFAULT_MAX_CONNECTIONS = 500;
	protected int maxConnections = DEFAULT_MAX_CONNECTIONS;

	public static final String COOKIE_SESSIONID_KEY = "sessionid";

	public static final int DEFAULT_SESSION_TIMEOUT = 24 * 60; // minutes = 24 hours
	private int sessionTimeout = DEFAULT_SESSION_TIMEOUT;

	public static final int DEFAULT_MAX_THREADS = 10;
	protected int maxThreads = DEFAULT_MAX_THREADS;

	public static final int DEFAULT_MAX_REQUEST_BODY_SIZE = 10 * 1000 * 1000; // = 10 MB
	protected int maxRequestBodySize = DEFAULT_MAX_REQUEST_BODY_SIZE;

	// other context names, see
	// https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#SSLContext
	public static final String SSL_INSTANCE_NAME = "TLSv1.2";

	private HttpServer http;
	private HttpsServer https;

	private Node myNode = null;

	private static final String DEFAULT_LOGFILE = "webConnector.log";

	private final L2pLogger logger = L2pLogger.getInstance(WebConnector.class.getName());

	private Handler logHandler = null;

	// information on Open ID Connect server, including configuration, according
	// to Open ID Connect Discovery (cf. http://openid.net/specs/openid-connect-discovery-1_0.html)
	public Map<String, JSONObject> oidcProviderInfos = new HashMap<>();

	private NameLock lockOidc = new NameLock();

	private X509Certificate caCert;
	private final HashMap<String, String> agentIdToSessionId;
	private final HashMap<String, AgentSession> sessions;
	private final SecureRandom secureRandom;
	private AuthenticationManager authenticationManager;

	/**
	 * create a new web connector instance.
	 */
	public WebConnector() {
		super.setFieldValues();
		agentIdToSessionId = new HashMap<>();
		sessions = new HashMap<>();
		secureRandom = new SecureRandom();
	}

	/**
	 * create a new web connector instance.
	 * 
	 * @param http
	 * @param httpPort
	 * @param https
	 * @param httpsPort
	 */
	public WebConnector(boolean http, int httpPort, boolean https, int httpsPort) {
		this();
		enableHttpHttps(http, https);
		setHttpPort(httpPort);
		setHttpsPort(httpsPort);
	}

	public WebConnector(Integer httpPort) {
		this();
		enableHttpHttps(true, false);
		setHttpPort(httpPort);
	}

	/**
	 * set the log file for this connector
	 * 
	 * @param filename
	 * @throws IOException
	 */
	public void setLogFile(String filename) throws IOException {
		logger.setLogfilePrefix(filename);
	}

	/**
	 * set the port for the HTTP connector to listen to
	 * 
	 * @param port
	 */
	public void setHttpPort(Integer port) {
		if (myNode != null) {
			throw new IllegalStateException("change of port only before startup!");
		}
		if (port == null || port < 1) {
			httpPort = 0;
		} else {
			httpPort = port;
		}
	}

	/**
	 * set the port for the web connector to listen to for the secure line
	 * 
	 * @param port
	 */
	public void setHttpsPort(Integer port) {
		if (myNode != null) {
			throw new IllegalStateException("change of port only before startup!");
		}
		if (port == null || port < 1) {
			httpsPort = 0;
		} else {
			httpsPort = port;
		}
	}

	/**
	 * enables/disables HTTP/HTTPs
	 * 
	 * @param http enable HTTP
	 * @param https enable HTTPS
	 */
	public void enableHttpHttps(boolean http, boolean https) {
		startHttp = http;
		startHttps = https;
	}

	/**
	 * @deprecated This method is no longer supported and will be removed in the future.
	 * 
	 * @param timeoutInMs
	 */
	@Deprecated
	public void setSocketTimeout(int timeoutInMs) {
		logError("Socket timeouts are not supported anymore! This method is stub!");
	}

	/**
	 * set a stream to log all messages to
	 * 
	 * @param stream
	 */
	public void setLogStream(OutputStream stream) {
		closeLogHandler();
		logHandler = new StreamHandler(stream, L2pLogger.getGlobalConsoleFormatter());
		logHandler.setLevel(Level.ALL);
		logger.addHandler(logHandler);
	}

	/**
	 * Closes an existing log handler instance, if it exists.
	 */
	private void closeLogHandler() {
		if (logHandler != null) {
			logger.removeHandler(logHandler);
			logHandler.close();
			logHandler = null;
		}
	}

	/**
	 * set the SSL key password
	 * 
	 * @param password
	 */
	public void setSslKeyPassword(String password) {
		sslKeyPassword = password;
	}

	/**
	 * set the location of the SSL keystore
	 * 
	 * @param keystore
	 */
	public void setSslKeystore(String keystore) {
		sslKeystore = keystore;
	}

	/**
	 * set the cross origin resource domain
	 * 
	 * @param cord
	 */
	public void setCrossOriginResourceDomain(String cord) {
		crossOriginResourceDomain = cord;
	}

	public String getCrossOriginResourceDomain() {
		return crossOriginResourceDomain;
	}

	public void setCrossOriginResourceMaxAge(int maxAge) {
		crossOriginResourceMaxAge = maxAge;
	}

	public int getCrossOriginResourceMaxAge() {
		return crossOriginResourceMaxAge;
	}

	/**
	 * allow cross origin resource sharing
	 * 
	 * @param enable
	 */
	public void setCrossOriginResourceSharing(boolean enable) {
		enableCrossOriginResourceSharing = enable;
	}

	public boolean isCrossOriginResourceSharing() {
		return enableCrossOriginResourceSharing;
	}

	/**
	 * prefer local services
	 * 
	 * @param enable
	 */
	public void setPreferLocalServices(boolean enable) {
		onlyLocalServices = enable;
	}

	@Override
	public void start(Node node) throws ConnectorException {
		if (!startHttp && !startHttps) {
			throw new ConnectorException("Either the HTTP mode or the HTTPS mode have to be enabled!");
		}

		if (logHandler == null) {
			try {
				setLogFile(DEFAULT_LOGFILE);
			} catch (IOException e) {
				throw new ConnectorException("Cannot initialize standard log file at " + DEFAULT_LOGFILE, e);
			}
		}

		for (String uri : oidcProviders) {
			try {
				uri = uri.trim();
				if (uri.endsWith("/")) {
					uri = uri.substring(0, uri.length() - 1);
				}
				JSONObject info = fetchOidcProviderConfig(uri);
				if (info != null) {
					oidcProviderInfos.put(uri, info);
				}
			} catch (Exception e) {
				logError("Could not fetch OIDC provider configuration " + e.getMessage());
			}
		}

		myNode = node;
		authenticationManager = new AuthenticationManager(this);
		try {
			ResourceConfig config = new ResourceConfig();
			config.register(new WebConnectorExceptionMapper(this));
			config.register(JacksonFeature.class);
			config.register(MultiPartFeature.class);
			config.property("jersey.config.server.wadl.disableWadl", true);
			config.register(new CORSResponseFilter(this));
			config.register(new WebConnectorRequestHandler(this));
			config.register(new DefaultHandler(this));
			config.register(new WebappHandler());
			config.register(new AuthHandler(this));
			config.register(new ServicesHandler(this));
			config.register(new AgentsHandler(this));
			config.register(new EthereumHandler(this));
			config.register(new SwaggerUIHandler(this));
			if (startHttp) {
				startHttpServer(config);
			}
			if (startHttps) {
				startHttpsServer(config);
			}
		} catch (IOException e) {
			throw new ConnectorException("Startup has been interrupted!", e);
		} catch (GeneralSecurityException e) {
			throw new ConnectorException("SSL encryption not possible!", e);
		} catch (Exception e) {
			if (http != null) {
				// try to cleanup mess
				try {
					http.stop(0);
				} catch (Exception e2) {
					logger.log(Level.SEVERE, "HTTPS server cleanup failed after failed connector start", e2);
				} finally {
					http = null;
				}
			}
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
	}

	private void startHttpServer(ResourceConfig config) throws Exception {
		http = JdkHttpServerFactory.createHttpServer(new URI(getHttpEndpoint() + "/"), config, null, false);
		httpPort = http.getAddress().getPort();
		http.setExecutor(Executors.newFixedThreadPool(maxThreads));
		http.start();
		logMessage("Web-Connector in HTTP mode running at " + getHttpEndpoint());
	}

	private void startHttpsServer(ResourceConfig config) throws Exception {
		String myHostname = getMyHostname();
		if (!myHostname.contains(".")) { // hacky check for canoncial name,
											// "localhost" or "las2peer" are not valid in SSL context
			throw new SSLException("Invalid hostname! '" + myHostname + "' can't be used with SSL");
		}
		char[] keystoreSecret;
		if (sslKeyPassword == null) {
			sslKeyPassword = getOrCreateSecretFromFile("keystore password",
					KEYSTORE_DIRECTORY + WebConnector.class.getSimpleName() + "-" + myHostname + ".secret");
		}
		keystoreSecret = sslKeyPassword.toCharArray();
		if (sslKeystore == null) {
			sslKeystore = KEYSTORE_DIRECTORY + WebConnector.class.getSimpleName() + "-" + myHostname + ".jks";
		}
		KeyStore keystore = KeystoreManager.loadOrCreateKeystore(sslKeystore, myHostname, keystoreSecret);
		caCert = (X509Certificate) keystore.getCertificate("Node Local las2peer Root CA");
		if (caCert == null) {
			logger.info("CA cert not found in keystore");
		} else {
			// export CA certificate to file, overwrite existing
			KeystoreManager.writeCertificateToPEMFile(caCert, KEYSTORE_DIRECTORY + getRootCAFilename());
		}
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(keystore, keystoreSecret);
		SSLContext sslContext = SSLContext.getInstance(SSL_INSTANCE_NAME);
		sslContext.init(kmf.getKeyManagers(), null, null);
		https = (HttpsServer) JdkHttpServerFactory.createHttpServer(new URI(getHttpsEndpoint() + "/"), config,
				sslContext, false);
		httpsPort = https.getAddress().getPort();
		https.setExecutor(Executors.newFixedThreadPool(maxThreads));
		https.start();
		logMessage("Web-Connector in HTTPS mode running at " + getHttpsEndpoint());
	}

	public String getRootCAFilename() {
		return getMyHostname() + "-RootCA.pem";
	}

	public String getMyHostname() {
		if (myNode instanceof PastryNodeImpl) {
			return ((PastryNodeImpl) myNode).getBindAddress().getCanonicalHostName();
		} else {
			return "localhost";
		}
	}

	public String getHttpsEndpoint() {
		return "https://" + getMyHostname() + ":" + httpsPort;
	}

	public String getHttpEndpoint() {
		return "http://" + getMyHostname() + ":" + httpPort;
	}

	@Override
	public synchronized void stop() throws ConnectorException {
		// stop the HTTP server
		if (https != null) {
			https.stop(0);
		}
		if (http != null) {
			http.stop(0);
		}
		caCert = null;
		agentIdToSessionId.clear();
		sessions.clear();
		logMessage("Web-Connector has been stopped");
		this.myNode = null;
	}

	/**
	 * get the node, this connector is running at / for
	 * 
	 * @return the Las2Peer node of this connector
	 */
	public Node getL2pNode() {
		return myNode;
	}

	/**
	 * Logs a message.
	 * 
	 * @param message
	 */
	public void logMessage(String message) {
		logger.info(message);
		myNode.observerNotice(MonitoringEvent.CONNECTOR_MESSAGE, myNode.getNodeId(), WEB_CONNECTOR + message);
		if (logHandler != null) { // StreamHandler don't auto flush
			logHandler.flush();
		}
	}

	/**
	 * Logs an error with throwable.
	 * 
	 * @param message
	 * @param throwable
	 */
	public void logError(String message, Throwable throwable) {
		logger.log(Level.SEVERE, message, throwable);
		if (myNode != null) {
			myNode.observerNotice(MonitoringEvent.CONNECTOR_ERROR, myNode.getNodeId(), WEB_CONNECTOR + message);
		}
		if (logHandler != null) { // StreamHandler don't auto flush
			logHandler.flush();
		}
	}

	/**
	 * Logs an error.
	 * 
	 * @param message
	 */
	public void logError(String message) {
		logError(message, null);
	}

	/**
	 * 
	 * @return true, if local running versions of services are preferred before broadcasting
	 */
	boolean onlyLocalServices() {
		return onlyLocalServices;
	}

	public NameLock getLockOidc() {
		return lockOidc;
	}

	/**
	 * Fetches Open ID Connect provider configuration, according to the OpenID Connect discovery specification (cf.
	 * http://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfig)
	 * 
	 * @param providerURI
	 * @return
	 * @throws IOException
	 */
	private JSONObject fetchOidcProviderConfig(String providerURI) throws IOException {
		JSONObject result = new JSONObject();

		// send Open ID Provider Config request
		// (cf. http://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfig)
		URL pConfigDocUri = new URL(providerURI + "/.well-known/openid-configuration");
		HTTPRequest pConfigRequest = new HTTPRequest(Method.GET, pConfigDocUri);

		// parse JSON result
		try {
			String configStr = pConfigRequest.send().getContent();
			JSONObject config = (JSONObject) JSONValue.parseWithException(configStr);
			// put JSON result in result table
			result.put("config", config);
		} catch (Exception e) {
			System.out.println("OpenID Connect Provider " + providerURI + " unreachable!");
			System.err.println(
					"Make sure to set a correct OpenID Connect Provider URL in your las2peer Web Connector config!");
			System.out.println("WebConnector will now run in OIDC agnostic mode.");
			logError("Could not retrieve a valid OIDC provider config from " + providerURI + "!");

			return null;
		}

		return result;
	}

	public String getOidcClientId() {
		return oidcClientId;
	}

	public String getOidcClientSecret() {
		return oidcClientSecret;
	}

	public int getHttpPort() {
		return httpPort;
	}

	public int getHttpsPort() {
		return httpsPort;
	}

	/**
	 * Gets the currently used CA certificate.
	 * 
	 * @return Returns the CA certificate or {@code null}, if the connector is not started.
	 * @throws FileNotFoundException If the certificate is not stored in the local keystore.
	 */
	public X509Certificate getCACertificate() throws FileNotFoundException {
		if (caCert == null) {
			throw new FileNotFoundException();
		}
		return caCert;
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

	public AgentImpl authenticateAgent(MultivaluedMap<String, String> requestHeaders, String accessTokenQueryParam) {
	    // if the requestHeader contains a valid sessionId we can just return the corresponding agent
        // check for sessionId in header
        String sessionIdHeader = requestHeaders.getFirst(SESSION_COOKIE);
        String sessionId = extractSessionCookie(sessionIdHeader);
        if (sessionId != null) {
            // check if sessionId is valid
            AgentSession session;
            if ((session = getSessionById(sessionId)) != null)
                return session.getAgent();
        }
		return authenticationManager.authenticateAgent(requestHeaders, accessTokenQueryParam);
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

	public String generateToken() {
		// src: https://stackoverflow.com/a/41156
		return new BigInteger(260, secureRandom).toString(32);
	}

    private String extractSessionCookie(String sessionIdHeader) {
        if (sessionIdHeader != null) {
            // check if value is valid session id format
            if (sessionIdHeader.startsWith("sessionid="))
                // return the session id
                return sessionIdHeader.substring("sessionid=".length());
        }
        return null;
    }
}
