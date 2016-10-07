package i5.las2peer.testing;

import i5.las2peer.api.Service;
import i5.las2peer.api.execution.ServiceInvocationException;
import i5.las2peer.api.execution.ServiceNotFoundException;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.security.AgentException;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.L2pServiceException;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.FileContentReader;
import i5.las2peer.tools.SimpleTools;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.Serializable;

import org.junit.After;
import org.junit.Before;

/**
 * Helper class to implement JUnit-Test for services to be published in a las2peer environment.
 * 
 * As standard, for each Test a new agent (coming along with a new encryption key pair and an own passphrase) is
 * generated. If you want to use a specific agent, you can define the static Strings AGENT_XML_FILE and AGENT_PASSPHRASE
 * as final class constants.
 * 
 * If you want to use any other procedure to create or define the agent for your service to test, you can override the
 * method {@link #createServiceAgent}.
 * 
 * In the launching phase of the {@link i5.las2peer.p2p.LocalNode}, the test case looks into the subdirectory
 * <i>startup</i> of the current working directory for XML files containing agents an envelopes to load into the node.
 * 
 * If you want to use any other directory, just override the method {@link #getStartupDir()}.
 * 
 * 
 *
 */
public abstract class LocalServiceTestCase {

	public static final String DEFAULT_STARTUP_DIR = "startup";

	private LocalNode localNode;

	private ServiceAgentImpl agent;

	private String agentPassphrase = null;

	/**
	 * This method creates the ServiceAgent to use with the las2peer node to run the service to test.
	 * 
	 * First it is tested, if the actual test case defines the constants AGENT_XML_FILE and AGENT_PASSPHRASE. If so,
	 * this information is used to load the ServiceAgent.
	 * 
	 * Otherwise a completely fresh ServiceAgent is generated.
	 * 
	 * Test cases may override this method to use any other arbitrary procedure to create a service agent for the
	 * service to test.
	 * 
	 * @return the service agent either created or loaded
	 * @throws CryptoException
	 * @throws L2pSecurityException
	 * @throws AgentException
	 */
	public ServiceAgentImpl createServiceAgent() throws CryptoException, L2pSecurityException, AgentException {
		Class<? extends LocalServiceTestCase> cls = getClass();

		try {
			String xml = (String) cls.getField("AGENT_XML_FILE").get(null);
			agentPassphrase = (String) cls.getField("AGENT_PASSPHRASE").get(null);

			File test = new File(xml);
			if (test.exists() && test.isFile()) {
				agent = ServiceAgentImpl.createFromXml(FileContentReader.read(xml));
			} else {
				InputStream is = getClass().getResourceAsStream(xml);
				if (is == null) {
					throw new AgentException("Neither file nor classpath resource wuth name " + xml + " exists!");
				}
				agent = ServiceAgentImpl.createFromXml(FileContentReader.read(getClass().getResourceAsStream(xml)));
			}

			if (agent.getServiceNameVersion().getName().equals(getServiceClass().getName())) {
				return agent;
			} else {
				throw new AgentException("This agent is not responsible for the testclass " + getServiceClass()
						+ " but for " + agent.getServiceNameVersion().getName());
			}
		} catch (NoSuchFieldException e) {
			agentPassphrase = SimpleTools.createRandomString(10);
			return ServiceAgentImpl.createServiceAgent(new ServiceNameVersion(getServiceClass().getName(),
					getServiceVersion()), agentPassphrase);
		} catch (Exception e) {
			if (e instanceof AgentException) {
				throw (AgentException) e;
			} else {
				throw new AgentException("Unable to load Agent XML file", e);
			}
		}
	}

	/**
	 * start a node and launch the service given by the implementation of {@link #getServiceClass}
	 * 
	 * @throws Exception
	 */
	@Before
	public void startServer() throws Exception {
		agent = createServiceAgent();

		agent.unlock(agentPassphrase);

		localNode = LocalNode.newNode();

		UserAgentImpl eve = MockAgentFactory.getEve();
		eve.unlock("evespass");
		localNode.storeAgent(eve);
		UserAgentImpl abel = MockAgentFactory.getAbel();
		abel.unlock("abelspass");
		localNode.storeAgent(abel);
		UserAgentImpl adam = MockAgentFactory.getAdam();
		adam.unlock("adamspass");
		localNode.storeAgent(adam);

		loadStartupDir();

		localNode.launch();
		localNode.registerReceiver(agent);
	}

	/**
	 * stop and reset all LocalNodes
	 * 
	 * @throws Exception
	 */
	@After
	public void stopServer() throws Exception {
		localNode.shutDown();

		LocalNode.reset();
	}

	/**
	 * 
	 * @return the node, the test is running on
	 */
	public LocalNode getNode() {
		return localNode;
	}

	/**
	 * 
	 * @return the ServiceAgent responsible for the Service to be tested
	 */
	public ServiceAgentImpl getMyAgent() {
		return agent;
	}

	/**
	 * 
	 * @return the passphrase for the private key of the freshly generated service agent
	 */
	protected String getAgentPassphrase() {
		return agentPassphrase;
	}

	/**
	 * shortcut for getting the actual instance of the service class to test
	 * 
	 * @return the running instance of the Service to test
	 * @throws ServiceNotFoundException
	 */
	protected Service getServiceInstance() throws ServiceNotFoundException {
		return this.getMyAgent().getServiceInstance();
	}

	/**
	 * define a service class to test, this service will be started in a {@link i5.las2peer.p2p.LocalNode} before
	 * starting the actual test
	 * 
	 * @return the service class to be launched and tested
	 */
	public abstract Class<? extends Service> getServiceClass();

	/**
	 * define a service version to test, this service will be started in a {@link i5.las2peer.p2p.LocalNode} before
	 * starting the actual test
	 * 
	 * @return the service version to be launched and tested
	 */
	public abstract String getServiceVersion();

	/**
	 * invoke a method of the service to test
	 * 
	 * @param executing
	 * @param method
	 * @param parameters
	 * @return result of the invocation
	 * @throws L2pServiceException
	 * @throws L2pSecurityException
	 * @throws AgentNotKnownException
	 * @throws InterruptedException
	 * @throws ServiceInvocationException
	 */
	public Serializable invoke(AgentImpl executing, String method, Serializable... parameters)
			throws L2pServiceException, L2pSecurityException, AgentNotKnownException, InterruptedException,
			ServiceInvocationException {
		return getNode().invokeLocally(executing, getMyAgent(), method, parameters);
	}

	/**
	 * load the XML file contained in the directory given by {@link #getStartupDir()} method and initialize the node
	 * with the agents and envelopes generated from these XML files
	 */
	public void loadStartupDir() {
		File dir = new File(getStartupDir());

		if (!dir.exists() || !dir.isDirectory()) {
			System.out.println("Hint: XML startup dir not found or is not a directory");
			return;
		}

		for (File xml : dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".xml");
			}
		})) {
			try {
				String content = FileContentReader.read(xml);

				if (content.contains("<las2peer:agent")) {
					AgentImpl agent = AgentImpl.createFromXml(content);
					localNode.storeAgent(agent);
					System.out.println("loaded " + xml + " as agent");
				} else {
					System.out.println("Don't known what to do with contents of " + xml);
				}
			} catch (Exception e) {
				System.out.println("File " + xml + " caused an Exception - ignoring");
				e.printStackTrace();
			}
		}

	}

	/**
	 * get the name of a directory, which XML files are to be loaded on server Startup by the {@link #loadStartupDir()}
	 * method.
	 * 
	 * May be overridden in actual test cases.
	 * 
	 * @return a directory name
	 */
	public String getStartupDir() {
		return DEFAULT_STARTUP_DIR;
	}

}
