package i5.las2peer.webConnector.serviceManagement;

import java.io.Serializable;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.ServiceNameVersion;
import i5.las2peer.persistency.EnvelopeException;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.data.PathTree;
import i5.las2peer.security.ServiceInfoAgent;
import i5.las2peer.webConnector.WebConnector;

public class ServiceRepositoryManager {

	private static final L2pLogger logger = L2pLogger.getInstance(ServiceRepositoryManager.class.getName());

	private static ServiceRepositoryManager manager;
	private static HashMap<String, ServiceData> serviceRepository = new HashMap<String, ServiceData>();
	private static PathTree tree;
	private static Timer timer;
	private static final int DEFAULT_TIMER_INTERVAL_SECONDS = 300;
	private static int timerIntervalSeconds = DEFAULT_TIMER_INTERVAL_SECONDS;

	public static final String SERVICE_SELFINFO_METHOD = "getRESTMapping";
	private static WebConnector connector;

	public static ServiceRepositoryManager getManager() {
		if (manager == null) {
			manager = new ServiceRepositoryManager();
		}
		return manager;
	}

	public static void start(Node node) throws Exception {
		startTimer(node);
	}

	public static void start(Node node, int timerIntervalSeconds) throws Exception {
		ServiceRepositoryManager.timerIntervalSeconds = timerIntervalSeconds;
		startTimer(node);
	}

	public static void stop() {
		stopTimer();
	}

	public static boolean hasService(String serviceName) {
		if (!serviceRepository.containsKey(serviceName)) {
			return false;
		}
		return serviceRepository.get(serviceName).isActive();
	}

	public static void manualUpdate(final Node node) throws Exception {
		executeTimer(node, getServiceInfoAgent(node));
	}

	private static void executeTimer(Node node, ServiceInfoAgent finalAgent) {
		ServiceNameVersion[] services;
		try {
			services = ServiceInfoAgent.getServices();
		} catch (EnvelopeException e) {
			// do nothing for now
			e.printStackTrace();
			return;
		}
		logger.info("found " + services.length + " services on the network");
		synchronized (serviceRepository) {
			HashSet<String> checkedServices = new HashSet<>(serviceRepository.keySet());
			for (ServiceNameVersion currentService : services) {
				logger.info("handling service " + currentService.getNameVersion());
				String internalServiceName = getInternalServiceName(currentService.getName(),
						currentService.getVersion());
				if (!serviceRepository.containsKey(internalServiceName)) { // new service
					logger.info("adding new service " + internalServiceName);
					// add dummy element to repo to avoid duplicate scanning
					serviceRepository.put(internalServiceName,
							new ServiceData(currentService.getName(), currentService.getVersion(), false, null));
					try {
						String xml = (String) node.invokeGlobally(finalAgent, currentService, SERVICE_SELFINFO_METHOD,
								new Serializable[] {});
						if (xml == null || xml.isEmpty()) {
							System.err.println("Couldn't get xml mapping for " + currentService.getName()
									+ "! Please see log for details!");
						} else {
							try {
								// tree.merge(RESTMapper.getMappingTree(xml));
								ServiceData data = new ServiceData(currentService.getName(),
										currentService.getVersion(), true, xml);
								serviceRepository.put(internalServiceName, data);

								logger.info("adding xml for " + internalServiceName);
								addXML(new String[] { xml }); // for compatibility services: a service can also give XML
																// definition to other services
							} catch (Exception e) {
								// do nothing for now
								e.printStackTrace();
							}
						}
					} catch (Exception e) {
						// do nothing for now
						e.printStackTrace();
					}
				} else if (!serviceRepository.get(internalServiceName).isActive()) { // enable not active services
					logger.info(internalServiceName + " is enabled again");
					serviceRepository.get(internalServiceName).enable();
				}
				checkedServices.remove(internalServiceName);
			}
			for (String service : checkedServices) {
				logger.info(service + " is disabled, because it seems not active anymore");
				serviceRepository.get(service).disable();
			}
		}
	}

	private static void startTimer(final Node node) throws Exception {
		if (timer != null) {
			// timer is already running
			return;
		}
		ServiceInfoAgent agent;
		agent = getServiceInfoAgent(node);

		final ServiceInfoAgent finalAgent = agent;

		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				executeTimer(node, finalAgent);
			}
		}, 0, // run first occurrence immediately
				timerIntervalSeconds * 1000); // run every x seconds
	}

	private static void stopTimer() {
		if (timer != null) {
			timer.cancel();
		}
		timer = null;
	}

	private static ServiceInfoAgent getServiceInfoAgent(Node node) throws Exception {
		ServiceInfoAgent agent;
		try {
			agent = ServiceInfoAgent.getServiceInfoAgent(ServiceInfoAgent.getDefaultPassphrase());

			if (!node.hasAgent(agent.getId())) {
				node.registerReceiver(agent);
			}
			agent.notifyRegistrationTo(node);
		} catch (Exception e) {
			throw new Exception("Error registering ServiceInfoAgent", e);
		}
		return agent;
	}

	public static void addXML(String[] xmls) throws Exception {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		XPath xPath = XPathFactory.newInstance().newXPath();
		for (String xml : xmls) {
			Document doc = dBuilder.parse(new InputSource(new StringReader(xml)));
			NodeList serviceNodeList = (NodeList) xPath.compile(String.format(".//%s", RESTMapper.SERVICE_TAG))
					.evaluate(doc, XPathConstants.NODESET);
			for (int j = 0; j < serviceNodeList.getLength(); j++) {
				Element serviceNode = (Element) serviceNodeList.item(j);
				String serviceName = serviceNode.getAttribute(RESTMapper.NAME_TAG).trim();
				String serviceVersion = serviceNode.getAttribute(RESTMapper.VERSION_TAG).trim();

				// if tree.merge detects conflicts, output them as an error
				String s = tree.merge(RESTMapper.getMappingTree(xml));
				if (s.length() > 0) {
					connector.logError(s);
				}

				Document d = dBuilder.newDocument();
				d.importNode(serviceNode, true);
				ServiceData data = new ServiceData(serviceName, serviceVersion, true, RESTMapper.XMLtoString(d));
				serviceRepository.put(getInternalServiceName(serviceName, serviceVersion), data);
			}
		}
	}

	private static String getInternalServiceName(String serviceName, String serviceVersion) {
		return String.format("%s@%s", serviceName, serviceVersion);
	}

	public static ServiceData getService(String serviceName, String serviceVersion) {
		return serviceRepository.get(getInternalServiceName(serviceName, serviceVersion));
	}

	public static void setTree(PathTree tree) {
		ServiceRepositoryManager.tree = tree;
	}

	public static void setConnector(WebConnector connector) {
		ServiceRepositoryManager.connector = connector;
	}

	public static WebConnector getConnector() {
		return connector;
	}

}
