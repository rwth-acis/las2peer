package i5.las2peer.execution;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import i5.las2peer.api.Context;
import i5.las2peer.api.TestService;
import i5.las2peer.api.execution.InternalServiceException;
import i5.las2peer.api.execution.ServiceMethodNotFoundException;
import i5.las2peer.api.execution.ServiceNotAvailableException;
import i5.las2peer.api.execution.ServiceNotFoundException;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.security.Agent;
import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentAlreadyExistsException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.api.security.GroupAgent;
import i5.las2peer.api.security.UserAgent;
import i5.las2peer.p2p.AgentAlreadyRegisteredException;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.Node;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.serialization.MalformedXMLException;
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

	@Test
	public void testInvocation() {
		try {
			String result = (String) context.invoke(ServiceNameVersion.fromString("i5.las2peer.api.TestService@0.1"),
					"getCaller");
			assertEquals(context.getMainAgent().getIdentifier(), result);
			result = (String) context.invoke("i5.las2peer.api.TestService@0.1", "getCaller");
			assertEquals(context.getMainAgent().getIdentifier(), result);

			result = (String) context.invokeInternally(
					ServiceNameVersion.fromString("i5.las2peer.api.TestService@0.1"), "getCaller");
			assertEquals(context.getServiceAgent().getIdentifier(), result);
			result = (String) context.invokeInternally("i5.las2peer.api.TestService@0.1", "getCaller");
			assertEquals(context.getServiceAgent().getIdentifier(), result);

			result = (String) context.invoke("i5.las2peer.api.TestService@0.1", "getEcho", "test");
			assertEquals("test", result);

			try {
				result = (String) context.invoke("i5.las2peer.api.TestService@0.2", "getCaller");
				fail("ServiceNotFoundException expected");
			} catch (ServiceNotFoundException e) {

			}

			try {
				result = (String) context.invoke("i5.las2peer.api.TestService@0.1", "doesNotExist");
				fail("ServiceMethodNotFoundException expected");
			} catch (ServiceMethodNotFoundException e) {

			}

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
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

	@Test
	public void testSecurity() {
		try {
			UserAgent userA = context.createUserAgent("passphrase");
			assertFalse(userA.isLocked());

			UserAgent userB = context.createUserAgent("passphrase");
			assertFalse(userB.isLocked());

			GroupAgent group = context.createGroupAgent(new Agent[] { userA, userB });
			assertFalse(group.isLocked());

			context.storeAgent(userA);
			context.storeAgent(userB);
			context.storeAgent(group);

			UserAgent userAFetched = (UserAgent) context.fetchAgent(userA.getIdentifier());
			assertTrue(userAFetched.isLocked());
			userAFetched.unlock("passphrase");
			assertFalse(userAFetched.isLocked());

			UserAgent userBFetched = (UserAgent) context.fetchAgent(userA.getIdentifier());
			assertTrue(userBFetched.isLocked());
			try {
				context.storeAgent(userBFetched);
				fail("AgentAccessDeniedException expected");
			} catch (AgentAccessDeniedException e) {
			}

			GroupAgent groupRequested = (GroupAgent) context.requestAgent(group.getIdentifier(), userA);
			assertFalse(groupRequested.isLocked());
			assertTrue(groupRequested.hasMember(userA));
			assertTrue(groupRequested.hasMember(userB));

			groupRequested.revokeMember(userB);
			context.storeAgent(groupRequested);
			groupRequested = (GroupAgent) context.requestAgent(group.getIdentifier(), userA);
			assertFalse(groupRequested.isLocked());
			assertTrue(groupRequested.hasMember(userA));
			assertFalse(groupRequested.hasMember(userB));

			assertTrue(context.hasAccess(group.getIdentifier(), userA));
			assertFalse(context.hasAccess(group.getIdentifier())); // adam is not in the group
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
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
