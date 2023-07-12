package i5.las2peer.execution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
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
import i5.las2peer.api.persistency.Envelope;
import i5.las2peer.api.persistency.EnvelopeAccessDeniedException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.api.persistency.EnvelopeOperationFailedException;
import i5.las2peer.api.security.Agent;
import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentAlreadyExistsException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentLockedException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.api.security.EmailAlreadyTakenException;
import i5.las2peer.api.security.GroupAgent;
import i5.las2peer.api.security.LoginNameAlreadyTakenException;
import i5.las2peer.api.security.UserAgent;
import i5.las2peer.logging.NodeObserver;
import i5.las2peer.p2p.AgentAlreadyRegisteredException;
import i5.las2peer.p2p.LocalNodeManager;
import i5.las2peer.p2p.Node;
import i5.las2peer.security.AnonymousAgentImpl;
import i5.las2peer.security.InternalSecurityException;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.tools.CryptoException;

public class ExecutionContextTest {

	Node node;
	Context context;
	UserAgentImpl adam;
	UserAgentImpl eve;

	@Before
	public void setup() throws MalformedXMLException, IOException, AgentAccessDeniedException,
			AgentAlreadyRegisteredException, InternalSecurityException, AgentException, CryptoException {
		node = new LocalNodeManager().launchNode();

		adam = MockAgentFactory.getAdam();
		adam.unlock("adamspass");
		node.storeAgent(adam);

		eve = MockAgentFactory.getEve();
		eve.unlock("evespass");
		node.storeAgent(eve);

		ServiceAgentImpl serviceAgent = node
				.startService(ServiceNameVersion.fromString("i5.las2peer.api.TestService@0.1"), "pass");
		context = new ExecutionContext(serviceAgent, node.getAgentContext(adam), node);
	}

	@Test
	public void testGetter() throws MalformedXMLException, IOException {
		assertNotNull(context.getExecutor());
		assertNotNull(context.getLogger(this.getClass()));
		assertEquals(MockAgentFactory.getAdam(), context.getMainAgent());
		assertEquals(TestService.class, context.getService().getClass());
		assertEquals(TestService.class, context.getService(TestService.class).getClass());
		assertEquals(ServiceNameVersion.fromString("i5.las2peer.api.TestService@0.1"),
				context.getServiceAgent().getServiceNameVersion());
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

	@Test
	public void testEnvelope()
			throws EnvelopeOperationFailedException, EnvelopeAccessDeniedException, EnvelopeNotFoundException {
		try {
			Envelope env = context.createEnvelope("id");
			env.setContent("content");
			assertTrue(env.isPrivate());
			assertTrue(env.hasReader(adam));
			context.storeEnvelope(env);

			try {
				env = context.requestEnvelope("id", eve);
				fail("Exception expected");
			} catch (EnvelopeAccessDeniedException e) {
			}

			try {
				env = context.requestEnvelope("id123");
				fail("Exception expected");
			} catch (EnvelopeNotFoundException e) {
			}

			env = context.requestEnvelope("id");
			assertEquals("content", env.getContent());

			env.setContent("content123");
			context.storeEnvelope(env);
			env = context.requestEnvelope("id");
			assertEquals("content123", env.getContent());

			env.addReader(eve);
			context.storeEnvelope(env);
			env = context.requestEnvelope("id", eve);

			env.setPublic();
			context.storeEnvelope(env);
			assertFalse(env.isPrivate());
			env = context.requestEnvelope("id", eve);
			assertFalse(env.isPrivate());

			env = context.requestEnvelope("id", eve);
			env.setContent("bad");
			try {
				context.storeEnvelope(env, eve);
				fail("Exception expected");
			} catch (EnvelopeAccessDeniedException e) {
			}
			env = context.requestEnvelope("id");
			assertEquals("content123", env.getContent());

			// TODO reclaim operation not (properly) implemented ...
			/*
			 * try { context.reclaimEnvelope("id", eve); fail("Exception expected"); } catch
			 * (EnvelopeAccessDeniedException e) { }
			 * 
			 * context.reclaimEnvelope("id"); try { env = context.requestEnvelope("id");
			 * fail("Exception expected"); } catch (EnvelopeNotFoundException e) { }
			 */
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testEnvelopeReaderGroup() {
		try {
			Envelope env = context.createEnvelope("id");
			env.setContent("content");
			assertTrue(env.isPrivate());
			assertTrue(env.hasReader(adam));
			context.storeEnvelope(env);

			env = context.requestEnvelope("id");
			GroupAgent group = context.createGroupAgent(new Agent[] { adam, eve }, "testEnvelopeReaderGroup");
			context.storeAgent(group);
			env.addReader(group);
			context.storeEnvelope(env);

			env = context.requestEnvelope("id", eve);
			assertTrue(env.hasReader(group));

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testEnvelopeSigningGroup() {
		try {
			GroupAgent group = context.createGroupAgent(new Agent[] { adam, eve }, "testEnvelopeSigningGroup");
			context.storeAgent(group);

			Envelope env = context.createEnvelope("id", group);
			env.setContent("content");
			assertTrue(env.isPrivate());
			assertTrue(env.hasReader(group));
			context.storeEnvelope(env);

			env = context.requestEnvelope("id", eve);
			env.setContent("newContent");
			context.storeEnvelope(env, eve);

			env = context.requestEnvelope("id");
			assertEquals(env.getContent(), "newContent");

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testEnvelopeStore() {
		try {
			// store twice
			Envelope env = context.createEnvelope("id");
			assertTrue(env.isPrivate());
			assertTrue(env.hasReader(adam));
			context.storeEnvelope(env);
			context.storeEnvelope(env);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testEnvelopePublic() {
		try {
			// create
			Envelope env = context.createEnvelope("id");
			assertTrue(env.isPrivate());
			assertTrue(env.hasReader(adam));
			context.storeEnvelope(env);

			// make public
			env = context.requestEnvelope("id");
			assertTrue(env.isPrivate());
			assertTrue(env.hasReader(adam));
			env.setPublic();
			assertFalse(env.isPrivate());
			assertFalse(env.hasReader(adam));
			context.storeEnvelope(env);

			// access with other agent
			env = context.requestEnvelope("id", eve);
			assertFalse(env.isPrivate());
			assertFalse(env.hasReader(adam));
			assertFalse(env.hasReader(eve));

			// add new agent
			env.addReader(adam);
			assertTrue(env.isPrivate());

			// try to store with non singing agent
			try {
				context.storeEnvelope(env, eve);
				fail("Exception expected");
			} catch (EnvelopeAccessDeniedException e) {
			}

			// store with singing agent
			context.storeEnvelope(env);

			// check if private again
			env = context.requestEnvelope("id");
			assertTrue(env.isPrivate());
			assertTrue(env.hasReader(adam));

			// try to fetch with other agent
			try {
				context.requestEnvelope("id", eve);
				fail("Exception expected");
			} catch (EnvelopeAccessDeniedException e) {
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testSecurity() {
		try {
			UserAgent userA = context.createUserAgent("passphrase");
			assertFalse(userA.isLocked());

			UserAgent userB = context.createUserAgent("passphrase");
			assertFalse(userB.isLocked());

			GroupAgent group = context.createGroupAgent(new Agent[] { userA, userB }, "testSecurity");
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
	public void testUserAgents() {
		try {
			UserAgent userA = context.createUserAgent("passphrase");
			userA.setLoginName("loginA");
			userA.setEmail("emaila@asdf.de");
			assertEquals("loginA", userA.getLoginName());
			assertEquals("emaila@asdf.de", userA.getEmail());

			UserAgent userB = context.createUserAgent("passphrase");
			userB.setLoginName("loginB");
			userB.setEmail("emailb@asdf.de");
			assertEquals("loginB", userB.getLoginName());
			assertEquals("emailb@asdf.de", userB.getEmail());

			context.storeAgent(userA);
			context.storeAgent(userB);

			userA = (UserAgent) context.fetchAgent(userA.getIdentifier());
			userA.unlock("passphrase");
			assertEquals("loginA", userA.getLoginName());
			assertEquals("emaila@asdf.de", userA.getEmail());

			userB = (UserAgent) context.fetchAgent(userB.getIdentifier());
			userB.unlock("passphrase");
			assertEquals("loginB", userB.getLoginName());
			assertEquals("emailb@asdf.de", userB.getEmail());

			userB.setLoginName("loginA");

			try {
				context.storeAgent(userB);
				fail("Exception expected");
			} catch (LoginNameAlreadyTakenException e) {
			}

			userA.setEmail("emailb@asdf.de");

			try {
				context.storeAgent(userA);
				fail("Exception expected");
			} catch (EmailAlreadyTakenException e) {
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testGroupAgents() {
		try {
			// create group
			GroupAgent a = context.createGroupAgent(new Agent[] { adam }, "testGroupAgentsa");
			assertEquals(1, a.getMemberList().length);
			context.storeAgent(a);

			// add member
			a = (GroupAgent) context.requestAgent(a.getIdentifier());
			assertEquals(1, a.getMemberList().length);
			a.addMember(eve);
			assertEquals(2, a.getMemberList().length);
			context.storeAgent(a);
			assertEquals(2, a.getMemberList().length);

			// request group
			a = (GroupAgent) context.requestAgent(a.getIdentifier());
			assertTrue(a.hasMember(adam));
			assertTrue(a.hasMember(eve));
			assertEquals(2, a.getMemberList().length);

			// do the same with unlock instead of request:

			// create group
			GroupAgent b = context.createGroupAgent(new Agent[] { adam }, "testGroupAgentsb");
			context.storeAgent(b);

			// add member
			b = (GroupAgent) context.fetchAgent(b.getIdentifier());
			b.unlock(adam);
			b.addMember(eve);
			context.storeAgent(b);

			// request group
			b = (GroupAgent) context.fetchAgent(b.getIdentifier());
			b.unlock(adam);
			assertTrue(b.hasMember(adam));
			assertTrue(b.hasMember(eve));
			assertEquals(2, b.getMemberList().length);

		} catch (AgentOperationFailedException | AgentAccessDeniedException | AgentAlreadyExistsException
				| AgentLockedException | AgentNotFoundException e) {
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

			public void logXESEvent(Long timestamp, MonitoringEvent event, String sourceNode, String sourceAgentId,
					String destinationNode, String destinationAgentId, String remarks, String caseId,
					String activityName,
					String resourceId, String resourceType, String lifecyclePhase, Long timeOfEvent) {
				// TODO Auto-generated method stub
			}
		});

		context.monitorEvent(this, MonitoringEvent.SERVICE_CUSTOM_ERROR_1, "testMessage", true);

		assertTrue(messages.get(messages.size() - 1).contains(this.getClass().getSimpleName()));
		assertTrue(messages.get(messages.size() - 1).contains(MonitoringEvent.SERVICE_CUSTOM_ERROR_1.toString()));
		assertTrue(messages.get(messages.size() - 1).contains("testMessage"));
		assertTrue(messages.get(messages.size() - 1).contains(context.getMainAgent().getIdentifier()));
		assertTrue(messages.get(messages.size() - 1).contains(context.getServiceAgent().getIdentifier()));
	}

	@Test
	public void testStoreAnonymous() {
		try {
			AnonymousAgentImpl anonymous = AnonymousAgentImpl.getInstance();
			context.storeAgent(anonymous);
			Assert.fail("Exception expected");
		} catch (AgentAccessDeniedException e) {
			// expected
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
