package i5.las2peer.webConnector;


import i5.httpServer.HttpServer;
import i5.httpServer.HttpsServer;
import i5.httpServer.RequestHandler;
import i5.las2peer.api.Connector;
import i5.las2peer.api.ConnectorException;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.Node;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.data.PathTree;
import i5.las2peer.security.Agent;
import i5.las2peer.webConnector.serviceManagement.ServiceRepositoryManager;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;



/**
 * Starter class for registering the Web Connector at the LAS2peer server.
 *
 * @author Holger Jan&szlig;en
 * @author Alexander Ruppert
 */


public class WebConnector extends Connector
{
	
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
	
	public static final int DEFAULT_SOCKET_TIMEOUT = 60*1000; // 1 minute
	protected int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
	
	public static final String DEFAULT_CROSS_ORIGIN_RESOURCE_DOMAIN = "*";
	protected String crossOriginResourceDomain = DEFAULT_CROSS_ORIGIN_RESOURCE_DOMAIN;
	
	public static final int DEFAULT_CROSS_ORIGIN_RESOURCE_MAX_AGE = 60;
	protected int crossOriginResourceMaxAge = DEFAULT_CROSS_ORIGIN_RESOURCE_MAX_AGE;

	public static final boolean DEFAULT_ENABLE_CROSS_ORIGIN_RESOURCE_SHARING = true;
	protected boolean enableCrossOriginResourceSharing = DEFAULT_ENABLE_CROSS_ORIGIN_RESOURCE_SHARING;
	
	public static final boolean DEFAULT_PREFER_LOCAL_SERVICES = true;
	protected boolean preferLocalServices = DEFAULT_PREFER_LOCAL_SERVICES;

    protected String xmlPath;
	
	private HttpServer http;
	private HttpsServer https;

	
	private Node myNode = null;
	
	
	private final static String DEFAULT_LOGFILE = "./log/webConnector.log";
	
	
	private PrintStream logStream = null;
	private DateFormat dateFormat = DateFormat.getDateTimeInstance();
	

	//--
	private PathTree tree=new PathTree();
	
	public PathTree getMappingTree()
	{
		return tree;
	}
	//--
	/**
	 * create a new web connector instance. 	
	 * @throws FileNotFoundException
	 */
	public WebConnector () throws Exception
    {
		super.setFieldValues();
        ServiceRepositoryManager.setTree(tree);
        ServiceRepositoryManager.setConnector(this);

	}
	
	/**
	 * create a new web connector instance. 	
	 * @throws FileNotFoundException
	 */
	public WebConnector (boolean http, int httpPort, boolean https, int httpsPort) throws Exception
    {
		this();
		enableHttpHttps(http,https);
		setHttpPort(httpPort);
		setHttpsPort(httpsPort);
        if(this.xmlPath!=null && !this.xmlPath.trim().isEmpty())
            ServiceRepositoryManager.addXML(RESTMapper.readAllXMLFromDir(xmlPath));
	}
	
	/**
	 * create a new web connector instance. 	
	 * @throws Exception 
	 */
	public WebConnector (boolean http, int httpPort, boolean https, int httpsPort, String xmlPath) throws Exception {
		this();
		enableHttpHttps(http,https);
		setHttpPort(httpPort);
		setHttpsPort(httpsPort);
        this.xmlPath=xmlPath;

        if(this.xmlPath!=null && !this.xmlPath.trim().isEmpty())
            ServiceRepositoryManager.addXML(RESTMapper.readAllXMLFromDir(xmlPath));
	}
	/**
	 * set the log file for this connector
	 * 
	 * @param filename
	 * @throws FileNotFoundException
	 */
	public void setLogFile ( String filename) throws FileNotFoundException {
		setLogStream ( new PrintStream ( new FileOutputStream ( filename, true )));
	}
	
	/**
	 * set the port for the HTTP connector to listen to
	 * 
	 * @param port
	 */
	public void setHttpPort ( int port ) {
		if ( port < 80 )
			throw new IllegalArgumentException ( "illegal port number: " + port );
		if ( myNode != null )
			throw new IllegalStateException ( "change of port only before startup!");
		
		httpPort = port;
	}
	
	
	/**
	 * set the port for the web connector to listen to for the secure line
	 * 
	 * @param port
	 */
	public void setHttpsPort ( int port ) {
		if ( port < 80 )
			throw new IllegalArgumentException ( "illegal port number: " + port );
		if ( myNode != null )
			throw new IllegalStateException ( "change of port only before startup!");
		
		httpsPort = port;
	}
	
	
	/**
	 * enables/disables HTTP/HTTPs
	 * @param http enable HTTP
	 * @param https enable HTTPS
	 */
	public void enableHttpHttps (boolean http, boolean https) {		
		startHttp=http;
		startHttps=https;
	}
	
	/**
	 * set the socket timeout for the underlying http server
	 * (only at configuration not during runtime)
	 * @param timeoutInMs
	 */
	public void setSocketTimeout ( int timeoutInMs ) {
		socketTimeout = timeoutInMs;
	}
	
	/**
	 * set a stream to log all messages to
	 * 
	 * @param stream
	 */
	public void setLogStream ( PrintStream stream ) {
		logStream = stream;
	}
	
	/**
	 * set the SSL key password
	 * @param password
	 */
	public void setSslKeyPassword ( String password) {
		sslKeyPassword = password;
	}
	
	/**
	 * set the location of the SSL keystore
	 * @param keystore
	 */
	public void setSslKeystore ( String keystore) {
		sslKeystore = keystore;
	}
	
	/**
	 * set the cross origin resource domain
	 * @param cord
	 */
	public void setCrossOriginResourceDomain ( String cord) {
		crossOriginResourceDomain = cord;
	}
	/**
	 * allow cross origin resource sharing 
	 * @param enable
	 */
	public void setCrossOriginResourceSharing ( boolean enable){
		enableCrossOriginResourceSharing=enable;
	}
	/**
	 * prefer local services
	 * @param enable
	 */
	public void setPreferLocalServices ( boolean enable){
		preferLocalServices=enable;
	}
	
	
	@Override
	public void start ( Node node ) throws ConnectorException {
		if (  ! startHttp && ! startHttps )
			throw new ConnectorException ( "either the http connector of the https connector have to be started!" );
		
		if ( logStream == null)
			try {
				setLogFile ( DEFAULT_LOGFILE );
			} catch (FileNotFoundException e) {
				throw new ConnectorException ( "cannot initialize standard log file at " + DEFAULT_LOGFILE, e);
			}
		
		myNode = node;
        try
        {
            ServiceRepositoryManager.start(myNode);
        }
        catch(Exception e)
        {
            logError("Could not start ServiceRepositoryManager: "+e.getMessage());
        }
        if ( startHttp ) {
			runServer(false);	
		}
		
		if ( startHttps ) {	
			runServer(true);
		}
	}
    public void updateServiceList()
    {
        try
        {
        ServiceRepositoryManager.manualUpdate(this.myNode);
        }
        catch(Exception e)
        {
            logError("Could not update services: "+e.getMessage());
        }
    }
	/**
	 * Starts either HTTP server or HTTPS Server
	 * @param isHttps true to run the HTTPS server, false to run the HTTP server
	 * @throws ConnectorException
	 */
	private void runServer(boolean isHttps) throws ConnectorException {
		if(isHttps){
			if (enableCrossOriginResourceSharing)
				https = new HttpsServer ( sslKeystore, sslKeyPassword, WebConnectorRequestHandler.class.getName(), httpsPort, crossOriginResourceDomain, crossOriginResourceMaxAge);
			else 
				https = new HttpsServer ( sslKeystore, sslKeyPassword, WebConnectorRequestHandler.class.getName(), httpsPort );
			https.setSocketTimeout( socketTimeout );
			https.start();
		}
		else{
			// start the HTTP listener
			if (enableCrossOriginResourceSharing) {
				http = new HttpServer (WebConnectorRequestHandler.class.getName(), httpPort, crossOriginResourceDomain, crossOriginResourceMaxAge);
			} else 
				http = new HttpServer (WebConnectorRequestHandler.class.getName(), httpPort);
			http.setSocketTimeout( socketTimeout );
			http.start();
		}
		
		RequestHandler handler;
		do {
			try {
				Thread.sleep( 500 );
			} catch (InterruptedException e) {
				throw new ConnectorException ( "Startup has been interrupted!", e);
			}
			if(isHttps)
				handler = https.getHandler();
			else
				handler = http.getHandler();
		} while ( handler == null );
		
		((WebConnectorRequestHandler) handler).setConnector( this );
		if(isHttps)
			logMessage("Web-Connector in HTTPS mode running on port " + httpsPort);
		else
			logMessage("Web-Connector in HTTP mode running on port " + httpPort);
	}
	
	
	@Override
	public void stop () throws ConnectorException {
		
		// stop the listener
		if ( http != null )
			http.stopServer();
		
		if ( https != null )
			https.stopServer();
		
		try {
			if ( http != null  ) {
				http.join ();
				logMessage("Web-Connector in HTTP mode has been stopped");

			}
			if ( https != null ) {
				https.join ();
				logMessage("Web-Connector in HTTPS mode has been stopped");
			}
		} catch (InterruptedException e) {
			logError("Joining has been interrupted!");
		}
		this.myNode = null;
		
	}
	
	
	/**
	 * send an interrupt to all sub servers
	 * (mainly for hard test shutdown)
	 */
	public void interrupt() {
		if ( http != null )
			http.interrupt();
		if ( https != null)
			https.interrupt();
		System.out.println ( "interrupted!");
	}
	
	
	/**
	 * get the node, this connector is running at / for
	 * 
	 * @return	the Las2Peer node of this connector
	 */
	public Node getL2pNode () {
		return myNode;
	}
	
	

	/**
	 * Logs a message.
	 * 
	 * @param message
	 */
	public void logMessage (String message) {
		logStream.println( dateFormat.format ( new Date() ) + "\t" + message);
		myNode.observerNotice(Event.CONNECTOR_MESSAGE, myNode.getNodeId(), WEB_CONNECTOR +message);
	}
	
	
	
	/**
	 * Logs a request.
	 * 
	 * @param request
	 */
	public void logRequest (String request) {
		logStream.println( dateFormat.format ( new Date() ) + "\t Request:" + request);
		
		int lastServiceClassNamePosition = request.lastIndexOf("/");
		if(lastServiceClassNamePosition > 0){
			String serviceClass = request.substring(1, lastServiceClassNamePosition);
			Agent service = null;
			try {
				service = myNode.getServiceAgent(serviceClass);
			} catch (AgentNotKnownException e) {
				// Should be known..
				e.printStackTrace();
			}
			myNode.observerNotice(Event.CONNECTOR_REQUEST, myNode.getNodeId(), service, WEB_CONNECTOR + request);
		}
		//Not a service call
		else{
			myNode.observerNotice(Event.CONNECTOR_REQUEST, myNode.getNodeId(), WEB_CONNECTOR + request);
		}
	}
	
	
	/**
	 * Logs an error.
	 * 
	 * @param error
	 */
	public void logError (String error) {
		logStream.println( dateFormat.format ( new Date() ) + "\t Error: " + error);
		myNode.observerNotice(Event.CONNECTOR_ERROR, myNode.getNodeId(), WEB_CONNECTOR +error);
	}
	
	
	/**
	 * 
	 * @return true, if local running versions of services are preferred before broadcasting 
	 */
	boolean preferLocalServices () {
		return preferLocalServices;
	}
	
	
}
