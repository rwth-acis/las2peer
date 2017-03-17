package i5.las2peer.execution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import i5.las2peer.api.Context;
import i5.las2peer.api.TestService;
import i5.las2peer.api.execution.InternalServiceException;
import i5.las2peer.api.execution.ServiceAccessDeniedException;
import i5.las2peer.api.execution.ServiceMethodNotFoundException;
import i5.las2peer.api.execution.ServiceNotFoundException;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.security.Agent;
import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentLockedException;
import i5.las2peer.api.security.GroupAgent;
import i5.las2peer.api.security.UserAgent;
import i5.las2peer.logging.NodeObserver;
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

	Node node;
	Context context;

	@Before
	public void setup() throws MalformedXMLException, IOException, AgentAccessDeniedException,
			AgentAlreadyRegisteredException, L2pSecurityException, AgentException, CryptoException {
		node = LocalNode.launchNode();

		UserAgentImpl adam = MockAgentFactory.getAdam();
		adam.unlock("adamspass");
		node.storeAgent(adam);

		ServiceAgentImpl serviceAgent = ServiceAgentImpl
				.createServiceAgent(ServiceNameVersion.fromString("i5.las2peer.api.TestService@0.1"), "pass");
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

			result = (String) context.invokeInternally(ServiceNameVersion.fromString("i5.las2peer.api.TestService@0.1"),
					"getCaller");
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

			try {
				result = (String) context.invoke("i5.las2peer.api.TestService@0.1", "accessForbidden");
				fail("ServiceAccessDeniedException expected");
			} catch (ServiceAccessDeniedException e) {

			}

			try {
				result = (String) context.invoke("i5.las2peer.api.TestService@0.1", "exception");
				fail("InternalServiceException expected");
			} catch (InternalServiceException e) {

			}

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	public void testEnvelopes() {
		// TODO API test exec context envelope api

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
				fail("AgentLockedException expected");
			} catch (AgentLockedException e) {
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

	@Test
	public void testMonitoring() {
		final List<String> messages = new ArrayList<>();
		node.addObserver(new NodeObserver() {
			@Override
			public void log(Long timestamp, MonitoringEvent event, String sourceNode, String sourceAgentId,
					String destinationNode, String destinationAgentId, String remarks) {
				messages.add(timestamp + " " + event + " " + sourceNode + " " + sourceAgentId + " " + destinationNode
						+ " " + destinationAgentId + " " + remarks);
			}
		});

		context.monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_1, "testMessage", true);

		assertTrue(messages.get(messages.size() - 1).contains(this.getClass().getSimpleName()));
		assertTrue(messages.get(messages.size() - 1).contains(MonitoringEvent.SERVICE_CUSTOM_ERROR_1.toString()));
		assertTrue(messages.get(messages.size() - 1).contains("testMessage"));
		assertTrue(messages.get(messages.size() - 1).contains(context.getMainAgent().getIdentifier()));
		assertTrue(messages.get(messages.size() - 1).contains(context.getServiceAgent().getIdentifier()));
	}
}
