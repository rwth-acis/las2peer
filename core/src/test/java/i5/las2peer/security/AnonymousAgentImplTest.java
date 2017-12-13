package i5.las2peer.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Serializable;

import org.junit.Before;
import org.junit.Test;

import i5.las2peer.api.Context;
import i5.las2peer.api.execution.ServiceInvocationException;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.persistency.Envelope;
import i5.las2peer.api.persistency.EnvelopeAccessDeniedException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.api.persistency.EnvelopeOperationFailedException;
import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.api.security.AnonymousAgent;
import i5.las2peer.communication.Message;
import i5.las2peer.execution.ExecutionContext;
import i5.las2peer.p2p.AgentAlreadyRegisteredException;
import i5.las2peer.p2p.LocalNodeManager;
import i5.las2peer.p2p.Node;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.tools.CryptoException;

public class AnonymousAgentImplTest {

	Node node;
	Node node2;
	ServiceAgentImpl serviceAgent;
	AnonymousAgentImpl anonymousAgent;
	ServiceNameVersion service = ServiceNameVersion.fromString("i5.las2peer.api.TestService@0.1");

	@Before
	public void setup() throws MalformedXMLException, IOException, AgentAccessDeniedException,
			AgentAlreadyRegisteredException, InternalSecurityException, AgentException, CryptoException {
		LocalNodeManager manager = new LocalNodeManager();
		node = manager.launchNode();
		node.getNodeServiceCache().setTimeoutMs(10000);
		node2 = manager.launchNode();

		anonymousAgent = AnonymousAgentImpl.getInstance();

		serviceAgent = node2.startService(service, "pass");
	}

	@Test
	public void testCreation() {
		assertFalse(anonymousAgent.isLocked());
	}

	@Test
	public void testMessages()
			throws EncodingFailedException, InternalSecurityException, SerializationException, AgentException {
		// anonymous as sender
		Message m = new Message(anonymousAgent, serviceAgent, "myContent");
		m.open(serviceAgent, node);
		m.verifySignature();
		assertEquals("myContent", m.getContent());

		// send answer
		Message answer = new Message(m, "myAnswer");
		answer.open(anonymousAgent, node);
		assertEquals("myAnswer", answer.getContent());

	}

	@Test
	public void testMediatorAndRMI() throws InternalSecurityException, ServiceInvocationException, AgentException {
		// create mediator
		Mediator mediator = node.createMediatorForAgent(anonymousAgent);

		// invoke
		Serializable s = mediator.invoke(service, "getEcho", new Serializable[] { "myString" }, false);
		assertEquals("myString", s);
	}

	@Test
	public void testStorage()
			throws EnvelopeOperationFailedException, EnvelopeAccessDeniedException, EnvelopeNotFoundException {
		// create context
		Context context = new ExecutionContext(serviceAgent, node.getAgentContext(anonymousAgent), node);

		// try to store with anonymous
		Envelope env;
		try {
			env = context.createEnvelope("myEnv");
			context.storeEnvelope(env);
			fail("Exception expected");
		} catch (EnvelopeAccessDeniedException e) {
		}

		// create envelope
		env = context.createEnvelope("myEnv", serviceAgent);
		env.setContent("myContent");
		env.setPublic();
		context.storeEnvelope(env, serviceAgent);

		// read envelope
		env = context.requestEnvelope("myEnv");
		assertEquals("myContent", env.getContent());

		// create encrypted envelope
		env = context.createEnvelope("myEnvEnc", serviceAgent);
		env.setContent("myContent");
		context.storeEnvelope(env, serviceAgent);

		// read encrypted envelope
		try {
			context.requestEnvelope("myEnvEnc");
			fail("EnvelopeAccessDeniedException expected");
		} catch (EnvelopeAccessDeniedException e) {
			// expected
		}
	}

	@Test
	public void testOperations() throws AgentNotFoundException, AgentException, InternalSecurityException {
		// load anonymous
		AnonymousAgent a = (AnonymousAgent) node.getAgent(AnonymousAgent.IDENTIFIER);
		assertEquals(a.getIdentifier(), AnonymousAgent.IDENTIFIER);

		a = (AnonymousAgent) node.getAgent(AnonymousAgent.LOGIN_NAME);
		assertEquals(a.getIdentifier(), AnonymousAgent.IDENTIFIER);

		// store anonymous
		try {
			node.storeAgent(anonymousAgent);
			fail("Exception expected");
		} catch (AgentException e) {

		}
	}

}
