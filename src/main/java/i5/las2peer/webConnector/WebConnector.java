package i5.las2peer.webConnector;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.StreamHandler;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import i5.las2peer.api.Connector;
import i5.las2peer.api.ConnectorException;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.Node;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.data.PathTree;
import i5.las2peer.security.Agent;
import i5.las2peer.webConnector.serviceManagement.ServiceRepositoryManager;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

/**
 * Starter class for registering the Web Connector at the LAS2peer server.
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

	public static final String DEFAULT_SSL_KEYSTORE = "keys/ssl";
	protected String sslKeystore = DEFAULT_SSL_KEYSTORE;

	public static final String DEFAULT_SSL_KEY_PASSWORD = "123456";
	protected String sslKeyPassword = DEFAULT_SSL_KEY_PASSWORD;

	public static final String DEFAULT_CROSS_ORIGIN_RESOURCE_DOMAIN = "*";
	protected String crossOriginResourceDomain = DEFAULT_CROSS_ORIGIN_RESOURCE_DOMAIN;

	public static final int DEFAULT_CROSS_ORIGIN_RESOURCE_MAX_AGE = 60;
	protected int crossOriginResourceMaxAge = DEFAULT_CROSS_ORIGIN_RESOURCE_MAX_AGE;

	public static final boolean DEFAULT_ENABLE_CROSS_ORIGIN_RESOURCE_SHARING = true;
	protected boolean enableCrossOriginResourceSharing = DEFAULT_ENABLE_CROSS_ORIGIN_RESOURCE_SHARING;

	public static final boolean DEFAULT_PREFER_LOCAL_SERVICES = false;
	protected boolean preferLocalServices = DEFAULT_PREFER_LOCAL_SERVICES;

	public static final int DEFAULT_SERVICE_REPOSITORY_UPDATE_INTERVAL_SECONDS = 300;
	protected int serviceRepositoryUpdateIntervalSeconds = DEFAULT_SERVICE_REPOSITORY_UPDATE_INTERVAL_SECONDS;

	public static final String DEFAULT_DEFAULT_OIDC_PROVIDER = "https://api.learning-layers.eu/o/oauth2";
	protected String defaultOIDCProvider = DEFAULT_DEFAULT_OIDC_PROVIDER;
	protected ArrayList<String> oidcProviders = new ArrayList<String>();

	{
		oidcProviders.add(DEFAULT_DEFAULT_OIDC_PROVIDER);
	}

	protected String defaultLoginUser = "";
	protected String defaultLoginPassword = "";

	public static final int DEFAULT_MAX_CONNECTIONS = 500;
	protected int maxConnections = DEFAULT_MAX_CONNECTIONS;

	protected String xmlPath;

	private HttpServer http;
	private HttpsServer https;

	private Node myNode = null;

	private static final String DEFAULT_LOGFILE = "webConnector.log";

	private final L2pLogger logger = L2pLogger.getInstance(WebConnector.class.getName());

	private Handler logHandler = null;

	// information on Open ID Connect server, including configuration, according
	// to Open ID Connect Discovery (cf. http://openid.net/specs/openid-connect-discovery-1_0.html)
	protected Map<String, JSONObject> oidcProviderInfos = new HashMap<String, JSONObject>();

	private HashMap<Long, Integer> openUserRequests = new HashMap<>();
	private PathTree tree = new PathTree();

	public PathTree getMappingTree() {
		return tree;
	}

	/**
	 * create a new web connector instance.
	 * 
	 * @throws FileNotFoundException
	 */
	public WebConnector() throws Exception {
		super.setFieldValues();

		ServiceRepositoryManager.setTree(tree);
		ServiceRepositoryManager.setConnector(this);
	}

	/**
	 * create a new web connector instance.
	 * 
	 * @throws FileNotFoundException
	 */
	public WebConnector(boolean http, int httpPort, boolean https, int httpsPort) throws Exception {
		this();
		enableHttpHttps(http, https);
		setHttpPort(httpPort);
		setHttpsPort(httpsPort);
		if (this.xmlPath != null && !this.xmlPath.trim().isEmpty()) {
			ServiceRepositoryManager.addXML(RESTMapper.readAllXMLFromDir(xmlPath));
		}
	}

	/**
	 * create a new web connector instance.
	 * 
	 * @throws Exception
	 */
	public WebConnector(boolean http, int httpPort, boolean https, int httpsPort, String xmlPath) throws Exception {
		this();
		enableHttpHttps(http, https);
		setHttpPort(httpPort);
		setHttpsPort(httpsPort);
		this.xmlPath = xmlPath;

		if (this.xmlPath != null && !this.xmlPath.trim().isEmpty()) {
			ServiceRepositoryManager.addXML(RESTMapper.readAllXMLFromDir(xmlPath));
		}
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
	public void setHttpPort(int port) {
		if (port < 80) {
			throw new IllegalArgumentException("illegal port number: " + port);
		}
		if (myNode != null) {
			throw new IllegalStateException("change of port only before startup!");
		}

		httpPort = port;
	}

	/**
	 * set the port for the web connector to listen to for the secure line
	 * 
	 * @param port
	 */
	public void setHttpsPort(int port) {
		if (port < 80) {
			throw new IllegalArgumentException("illegal port number: " + port);
		}
		if (myNode != null) {
			throw new IllegalStateException("change of port only before startup!");
		}

		httpsPort = port;
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

	/**
	 * allow cross origin resource sharing
	 * 
	 * @param enable
	 */
	public void setCrossOriginResourceSharing(boolean enable) {
		enableCrossOriginResourceSharing = enable;
	}

	/**
	 * prefer local services
	 * 
	 * @param enable
	 */
	public void setPreferLocalServices(boolean enable) {
		preferLocalServices = enable;
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
		try {
			ServiceRepositoryManager.start(myNode, serviceRepositoryUpdateIntervalSeconds);
		} catch (Exception e) {
			logError("Could not start ServiceRepositoryManager: " + e.getMessage());
		}
		if (startHttp) {
			runServer(false);
		}

		if (startHttps) {
			runServer(true);
		}
	}

	public void updateServiceList() {
		try {
			ServiceRepositoryManager.manualUpdate(this.myNode);
		} catch (Exception e) {
			logError("Could not update services: " + e.getMessage());
		}
	}

	/**
	 * Starts either HTTP server or HTTPS Server
	 * 
	 * @param isHttps true to run the HTTPS server, false to run the HTTP server
	 * @throws ConnectorException
	 */
	private void runServer(boolean isHttps) throws ConnectorException {
		try {
			if (isHttps) {
				https = HttpsServer.create(new InetSocketAddress(httpsPort), maxConnections);
				// apply ssl certificates and key
				SSLContext sslContext = SSLContext.getInstance("TLS");
				char[] keystorePassword = sslKeyPassword.toCharArray();
				KeyStore ks = KeyStore.getInstance("JKS");
				ks.load(new FileInputStream(sslKeystore), keystorePassword);
				KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
				kmf.init(ks, keystorePassword);
				sslContext.init(kmf.getKeyManagers(), null, null);
				HttpsConfigurator configurator = new HttpsConfigurator(sslContext);
				https.setHttpsConfigurator(configurator);
			} else {
				http = HttpServer.create(new InetSocketAddress(httpPort), maxConnections);
			}
		} catch (IOException e) {
			throw new ConnectorException("Startup has been interrupted!", e);
		} catch (GeneralSecurityException e) {
			throw new ConnectorException("SSL encryption not possible!", e);
		}
		WebConnectorRequestHandler handler = new WebConnectorRequestHandler(this);
		if (isHttps) {
			https.setExecutor(Executors.newCachedThreadPool());
			https.createContext("/", handler);
			https.start();
			logMessage("Web-Connector in HTTPS mode running on port " + httpsPort);
		} else {
			http.setExecutor(Executors.newCachedThreadPool());
			http.createContext("/", handler);
			http.start();
			logMessage("Web-Connector in HTTP mode running on port " + httpPort);
		}
	}

	@Override
	public void stop() throws ConnectorException {
		// stop the timer
		ServiceRepositoryManager.stop();
		// stop the HTTP server
		if (https != null) {
			https.stop(0);
		}
		if (http != null) {
			http.stop(0);
		}
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
		logger.fine(message);
		myNode.observerNotice(Event.CONNECTOR_MESSAGE, myNode.getNodeId(), WEB_CONNECTOR + message);
		if (logHandler != null) { // StreamHandler don't auto flush
			logHandler.flush();
		}
	}

	/**
	 * Logs a request.
	 * 
	 * @param request
	 */
	/* commented out because lacking documentation of the request parameter and no calling methods thus impossbile to adapt to las2peer changes
	public void logRequest(String request) {
		logger.finer(request);
		int lastServiceClassNamePosition = request.lastIndexOf("/");
		if (lastServiceClassNamePosition > 0) {
			String serviceClass = request.substring(1, lastServiceClassNamePosition);
			Agent service = null;
			try {
				service = myNode.getServiceAgent(serviceClass);
			} catch (AgentNotKnownException e) {
				// Should be known..
				e.printStackTrace();
			}
			myNode.observerNotice(Event.CONNECTOR_REQUEST, myNode.getNodeId(), service, WEB_CONNECTOR + request);
		} else { // Not a service call
			myNode.observerNotice(Event.CONNECTOR_REQUEST, myNode.getNodeId(), WEB_CONNECTOR + request);
		}
		if (logHandler != null) { // StreamHandler don't auto flush
			logHandler.flush();
		}
	}
	*/

	/**
	 * Logs an error with throwable.
	 * 
	 * @param message
	 * @param throwable
	 */
	public void logError(String message, Throwable throwable) {
		logger.log(Level.SEVERE, message, throwable);
		if (myNode != null) {
			myNode.observerNotice(Event.CONNECTOR_ERROR, myNode.getNodeId(), WEB_CONNECTOR + message);
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
	boolean preferLocalServices() {
		return preferLocalServices;
	}

	public HashMap<Long, Integer> getOpenUserRequests() {
		return openUserRequests;
	}

	/**
	 * Fetches Open ID Connect provider configuration, according to the OpenID Connect discovery specification (cf.
	 * http://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfig)
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

}
