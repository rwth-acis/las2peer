package i5.las2peer.execution;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import i5.las2peer.api.Context;
import i5.las2peer.api.TestService;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.classLoaders.LibraryClassLoader;
import i5.las2peer.p2p.AgentAlreadyRegisteredException;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.Node;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.tools.CryptoException;

public class ExecutionContextTest {

	// TODO API test exec context

	Node node;
	Context context;

	@Before
	public void setup() throws MalformedXMLException, IOException, AgentAccessDeniedException,
			AgentAlreadyRegisteredException, L2pSecurityException, AgentException, CryptoException {
		node = LocalNode.launchNode();

		UserAgentImpl adam = MockAgentFactory.getAdam();
		adam.unlock("adamspass");
		node.storeAgent(adam);

		ServiceAgentImpl serviceAgent = ServiceAgentImpl.createServiceAgent(
				ServiceNameVersion.fromString("i5.las2peer.api.TestService@0.1"), "pass");
		serviceAgent.unlock("pass");
		node.storeAgent(serviceAgent);
		node.registerReceiver(serviceAgent);

		context = new ExecutionContext(serviceAgent, node.getAgentContext(adam), node);
	}

	@After
	public void reset() {
		LocalNode.reset();
	}

	@Test
	public void testGetter() throws MalformedXMLException, IOException {
		assertNotNull(context.getExecutor());
		assertNotNull(context.getLogger(this.getClass()));
		assertEquals(context.getMainAgent(), MockAgentFactory.getAdam());
		assertEquals(context.getService().getClass(), TestService.class);
		assertEquals(context.getService(TestService.class).getClass(), TestService.class);
		assertEquals(context.getServiceAgent().getServiceNameVersion(),
				ServiceNameVersion.fromString("i5.las2peer.api.TestService@0.1"));
		assertNotNull(context.getServiceClassLoader());
	}

	public void testInvocation() {
		/*
		 * java.io.Serializable 	invoke(ServiceNameVersion service, java.lang.String method, java.io.Serializable... parameters)
		java.io.Serializable 	invoke(java.lang.String service, java.lang.String method, java.io.Serializable... parameters)
		java.io.Serializable 	invokeInternally(ServiceNameVersion service, java.lang.String method, java.io.Serializable... parameters)
		java.io.Serializable 	invokeInternally(java.lang.String service, java.lang.String method, java.io.Serializable... parameters)

		 */
	}

	public void testEnvelopes() {
		/*
		 Envelope 	createEnvelope(java.lang.String identifier)
			Envelope 	createEnvelope(java.lang.String identifier, Agent using)
		Envelope 	requestEnvelope(java.lang.String identifier)
		Envelope 	requestEnvelope(java.lang.String identifier, Agent using)
		void 	storeEnvelope(Envelope env)
		void 	storeEnvelope(Envelope env, Agent using)
		void 	storeEnvelope(Envelope env, EnvelopeCollisionHandler handler)
		void 	storeEnvelope(Envelope env, EnvelopeCollisionHandler handler, Agent using)
		void 	reclaimEnvelope(java.lang.String identifier)
		void 	reclaimEnvelope(java.lang.String identifier, Agent using)
		 */
	}

	public void testSecurity() {
		/*
		 * GroupAgent 	createGroupAgent(Agent[] members)
		UserAgent 	createUserAgent(java.lang.String passphrase)
		Agent 	fetchAgent(java.lang.String agentId)
		Agent 	requestAgent(java.lang.String agentId)
		Agent 	requestAgent(java.lang.String agentId, Agent using)
		void 	storeAgent(Agent agent)
		 * boolean 	hasAccess(java.lang.String agentId)
		boolean 	hasAccess(java.lang.String agentId, Agent using)
		 */
	}

	public void testMonitoring() {
		/*
		 * 	void 	monitorEvent(MonitoringEvent event, java.lang.String message)
		void 	monitorEvent(MonitoringEvent event, java.lang.String message, Agent actingUser)
		void 	monitorEvent(java.lang.Object from, MonitoringEvent event, java.lang.String message)
		void 	monitorEvent(java.lang.Object from, MonitoringEvent event, java.lang.String message, Agent serviceAgent, Agent actingUser)
		

		 */
	}
}
