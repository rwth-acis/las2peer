package i5.las2peer.p2p;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import i5.las2peer.api.exceptions.ArtifactNotFoundException;
import i5.las2peer.communication.Message;
import i5.las2peer.communication.PingPongContent;
import i5.las2peer.p2p.Node.SendMode;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.AgentException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.Mediator;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

public class LocalNodeTest {

	private UserAgent eve;
	private UserAgent adam;
	private UserAgent abel;

	private static int counter;

	@Before
	public void setUp() throws NoSuchAlgorithmException, L2pSecurityException, CryptoException, MalformedXMLException,
			IOException {
		LocalNode.reset();

		eve = MockAgentFactory.getEve();
		adam = MockAgentFactory.getAdam();
		abel = MockAgentFactory.getAbel();

		counter = 0;
		testVariable = false;

	}

	private static boolean testVariable;

	@Test
	public void test() throws AgentAlreadyRegisteredException, EncodingFailedException, L2pSecurityException,
			SerializationException, InterruptedException, AgentException, AgentNotKnownException {
		System.out.println("start: " + new Date());

		LocalNode testee = LocalNode.launchNode();

		try {
			testee.registerReceiver(eve);
			fail("L2pSecurityException expected");
		} catch (L2pSecurityException e) {
		}

		eve.unlockPrivateKey("evespass");
		adam.unlockPrivateKey("adamspass");

		testee.registerReceiver(eve);
		testee.registerReceiver(adam);

		assertFalse(eve.isLocked());
		assertFalse(adam.isLocked());

		System.out.println("check1: " + new Date());

		testVariable = false;
		MessageResultListener listener = new MessageResultListener(10000) {
			@Override
			public void notifySuccess() {
				LocalNodeTest.testVariable = true;
			}

		};

		PingPongContent c = new PingPongContent();
		Message m = new Message(adam, eve, c);

		testee.sendMessage(m, listener);

		listener.waitForAllAnswers();

		assertFalse(listener.isTimedOut());
		assertFalse(listener.hasException());
		assertTrue(listener.isSuccess());
		assertTrue(listener.isFinished());

		Message answer = listener.getResults()[0];
		answer.open(adam, testee);
		assertTrue(c.getTimestamp() < ((PingPongContent) answer.getContent()).getTimestamp());
		assertTrue(testVariable);
	}

	@Test
	public void testTwoNodes() throws L2pSecurityException, EncodingFailedException, SerializationException,
			InterruptedException, AgentException {
		adam.unlockPrivateKey("adamspass");
		eve.unlockPrivateKey("evespass");

		// launch to nodes with one agent each
		LocalNode testee1 = LocalNode.launchAgent(adam);
		LocalNode.launchAgent(eve);

		assertTrue(LocalNode.findAllNodesWithAgent(adam.getId()).length > 0);
		assertTrue(LocalNode.findAllNodesWithAgent(eve.getId()).length > 0);

		MessageResultListener l = new MessageResultListener(10000);
		Message m = new Message(adam, eve, new PingPongContent());

		testee1.sendMessage(m, l);

		l.waitForAllAnswers();

		assertEquals(1, l.getNumberOfExpectedResults());
		assertTrue(l.isFinished());
		assertTrue(l.isSuccess());

	}

	@Test
	public void testTimeout() throws EncodingFailedException, L2pSecurityException, SerializationException,
			InterruptedException, AgentException {
		adam.unlockPrivateKey("adamspass");

		LocalNode testee1 = LocalNode.launchAgent(adam);
		MessageResultListener l = new MessageResultListener(2000) {
			@Override
			public void notifyTimeout() {
				LocalNodeTest.testVariable = true;
			}
		};
		Message m = new Message(adam, eve, new PingPongContent(), 1000);

		LocalNode.setPendingTimeOut(1000);

		testee1.sendMessage(m, l);

		Thread.sleep(30000);

		assertFalse(l.isSuccess());
		assertTrue(l.isTimedOut());
		assertEquals(0, l.getResults().length);
		assertTrue(testVariable);
	}

	@Test
	public void testBroadcast() throws EncodingFailedException, L2pSecurityException, SerializationException,
			InterruptedException, AgentException, AgentNotKnownException {
		adam.unlockPrivateKey("adamspass");
		eve.unlockPrivateKey("evespass");

		// launch three nodes with one agent each
		LocalNode testee1 = LocalNode.launchAgent(adam);
		LocalNode hosting1 = LocalNode.launchAgent(eve);
		assertEquals(1, LocalNode.findAllNodesWithAgent(eve.getId()).length);

		LocalNode hosting2 = LocalNode.launchAgent(eve);

		assertTrue(hosting1.hasLocalAgent(eve));
		assertTrue(hosting2.hasLocalAgent(eve));

		assertNotSame(hosting1.getAgent(eve.getId()), hosting2.getAgent(eve.getId()));

		assertEquals(2, LocalNode.findAllNodesWithAgent(eve.getId()).length);

		MessageResultListener l = new MessageResultListener(10000) {
			@Override
			public void notifySuccess() {
				synchronized (this) {
					System.out.println("result retrieved");
					LocalNodeTest.counter++;
				}
			}
		};
		// l.addRecipient();
		assertEquals(1, l.getNumberOfExpectedResults());

		Message m = new Message(adam, eve, new PingPongContent());
		testee1.sendMessage(m, l, Node.SendMode.BROADCAST);
		assertEquals(2, l.getNumberOfExpectedResults());

		l.waitForAllAnswers();

		assertEquals(2, l.getNumberOfResults());
		assertEquals(counter, 2);
		assertTrue(l.isSuccess());
		assertTrue(l.isFinished());

	}

	@Test
	public void testPending() throws L2pSecurityException, EncodingFailedException, SerializationException,
			InterruptedException, AgentException {
		adam.unlockPrivateKey("adamspass");
		eve.unlockPrivateKey("evespass");

		LocalNode testee = LocalNode.launchAgent(adam);

		MessageResultListener l = new MessageResultListener(8000) {
			@Override
			public void notifySuccess() {
				LocalNodeTest.testVariable = true;
			}
		};

		Message m = new Message(adam, eve, new PingPongContent());
		testee.sendMessage(m, l);

		Thread.sleep(5000);

		assertFalse(testVariable);
		assertFalse(l.isSuccess());
		assertFalse(l.isFinished());

		// launch another node hosting eve
		LocalNode.launchAgent(eve);
		Thread.sleep(LocalNode.getMaxMessageWait() + 6000);

		assertTrue(l.isSuccess());
		assertTrue(l.isFinished());
		assertTrue(testVariable);
	}

	@Test
	public void testRegisteringAgents() throws L2pSecurityException, MalformedXMLException, IOException,
			AgentAlreadyRegisteredException, AgentNotKnownException, AgentException {
		adam.unlockPrivateKey("adamspass");
		// abel.unlockPrivateKey ( "abelspass");

		LocalNode testee = LocalNode.launchAgent(adam);

		// testee.storeAgent(abel);

		try {
			testee.storeAgent(abel);
			fail("L2sSecurityAxception expected");
		} catch (L2pSecurityException e) {
		}

		try {
			testee.storeAgent(adam);
			fail("AgentAlreadyRegistered exception expected");
		} catch (AgentAlreadyRegisteredException e) {
		}

		abel.unlockPrivateKey("abelspass");
		testee.storeAgent(abel);

		LocalNode testee2 = LocalNode.launchNode();

		UserAgent retrieve = (UserAgent) testee2.getAgent(abel.getId());
		assertTrue(retrieve.isLocked());

		try {
			testee2.updateAgent(retrieve);
			fail("SecurtityException expected");
		} catch (L2pSecurityException e) {
		}

		retrieve.unlockPrivateKey("abelspass");
		testee2.updateAgent(retrieve);
	}

	@Test
	public void testRegisteringTopics() throws L2pSecurityException, AgentException, NodeException {
		// start node
		adam.unlockPrivateKey("adamspass");
		abel.unlockPrivateKey("abelspass");
		eve.unlockPrivateKey("evespass");
		LocalNode testee = LocalNode.launchNode();
		testee.storeAgent(adam);
		testee.storeAgent(abel);
		testee.storeAgent(eve);

		// test registering to topic without being registered to the node
		try {
			testee.registerReceiverToTopic(adam, 1);
			fail("AgentNotKnownException expected");
		} catch (AgentNotKnownException e) {
		}

		// test unregsiter without being registered
		testee.unregisterReceiverFromTopic(adam, 1);

		// register agents
		testee.registerReceiver(adam);
		testee.registerReceiver(abel);
		testee.registerReceiver(eve);

		// test register
		assertFalse(testee.hasTopic(1));
		testee.registerReceiverToTopic(adam, 1);
		assertTrue(testee.hasTopic(1));

		// test register to another topic
		testee.registerReceiverToTopic(adam, 2);
		testee.registerReceiverToTopic(adam, 3);
		assertTrue(testee.hasTopic(2));
		assertTrue(testee.hasTopic(3));

		// test register another agent to same topic
		testee.registerReceiverToTopic(abel, 1);
		testee.registerReceiverToTopic(eve, 1);
		testee.registerReceiverToTopic(eve, 2);

		// unregister from topic - topic should be removed
		testee.unregisterReceiverFromTopic(adam, 3);
		assertFalse(testee.hasTopic(3));

		// unregister from topic - should not be removed
		testee.unregisterReceiverFromTopic(eve, 2);
		assertTrue(testee.hasTopic(2));

		// unregister agent - one topic should be removed
		testee.unregisterReceiver(adam);
		assertTrue(testee.hasTopic(1));
		assertFalse(testee.hasTopic(2));

		// test unregsiter without being registered - again
		testee.unregisterReceiverFromTopic(adam, 1);
		assertTrue(testee.hasTopic(1));

		// unregister agent - nothing should happen
		testee.unregisterReceiver(eve);
		assertTrue(testee.hasTopic(1));

		// unregister agent - remove topic
		testee.unregisterReceiver(abel);
		assertFalse(testee.hasTopic(1));

		// test unregsiter without being registered - again
		testee.unregisterReceiverFromTopic(adam, 1);

	}

	@Test
	public void testSendAndReceiveTopics() throws L2pSecurityException, AgentException, EncodingFailedException,
			SerializationException, InterruptedException, TimeoutException {
		// start node
		adam.unlockPrivateKey("adamspass");
		abel.unlockPrivateKey("abelspass");
		eve.unlockPrivateKey("evespass");
		LocalNode node1 = LocalNode.launchNode();
		LocalNode node2 = LocalNode.launchNode();
		node1.storeAgent(adam);
		node1.storeAgent(abel);
		node1.storeAgent(eve);

		// register receiver to topics
		Mediator mAdam = node1.createMediatorForAgent(adam);
		Mediator mAbel = node1.createMediatorForAgent(abel);
		Mediator mEve = node2.createMediatorForAgent(eve);
		node1.registerReceiver(mAdam);
		node1.registerReceiver(mAbel);
		node2.registerReceiver(mEve);
		node1.registerReceiverToTopic(mAdam, 1);
		node1.registerReceiverToTopic(mAbel, 1);
		node2.registerReceiverToTopic(mEve, 1);

		// send msg to unknown topic
		Message noreceiver = new Message(adam, 2, "some content");
		MessageResultListener lst1 = new MessageResultListener(1000);
		node1.sendMessage(noreceiver, lst1, SendMode.BROADCAST);

		// send message
		Message sent = new Message(adam, 1, "some content");
		MessageResultListener lst = new MessageResultListener(1000);
		node1.sendMessage(sent, lst, SendMode.BROADCAST);

		// wait until messages are sent
		Thread.sleep(4000);

		// receive
		Message received1 = mAdam.getNextMessage();
		Message received2 = mAbel.getNextMessage();
		Message received3 = mEve.getNextMessage();

		// messages should be cloned
		assertTrue(received1 != null && received2 != null && received3 != null);
		assertTrue(received1 != received2);

		// check if receiver is set correctly
		assertTrue(received1.getRecipientId() == adam.getId());
		assertTrue(received2.getRecipientId() == abel.getId());
		assertTrue(received3.getRecipientId() == eve.getId());

		assertTrue(received3.getSenderId() == adam.getId());
		assertTrue(received3.getTopicId() == 1);

		// cehck if open
		assertTrue(received1.isOpen());
		assertTrue(received2.isOpen());
		assertTrue(received3.isOpen());

		// open
		// received2.open(abel, node1);
		// assertEquals(received2.getContent(), "some content");

		// answer
		// a mediator always sends an answer...
		Message msg = new Message(adam, 1, "some content");
		Message answer = node1.sendMessageAndWaitForAnswer(msg);

		assertTrue(answer.getResponseToId() == msg.getId());
	}

	@Test
	public void testCollectMessags() throws L2pSecurityException, AgentException, EncodingFailedException,
			SerializationException, InterruptedException, TimeoutException {
		// start node
		adam.unlockPrivateKey("adamspass");
		abel.unlockPrivateKey("abelspass");
		eve.unlockPrivateKey("evespass");
		LocalNode node1 = LocalNode.launchNode();
		LocalNode node2 = LocalNode.launchNode();
		node1.storeAgent(adam);
		node1.storeAgent(abel);
		node1.storeAgent(eve);

		// register receiver to topics
		Mediator mAdam = node1.createMediatorForAgent(adam);
		Mediator mAbel = node1.createMediatorForAgent(abel);
		Mediator mEve = node2.createMediatorForAgent(eve);
		node1.registerReceiver(mAdam);
		node1.registerReceiver(mAbel);
		node2.registerReceiver(mEve);
		node1.registerReceiverToTopic(mAdam, 1);
		node1.registerReceiverToTopic(mAbel, 1);
		node2.registerReceiverToTopic(mEve, 1);

		// collect answers
		Message msg1 = new Message(adam, 1, "collect...", 20000);
		Message[] answers = node1.sendMessageAndCollectAnswers(msg1, 5);
		assertTrue(answers.length == 3);
	}

	@Test
	public void testStartupAgents() throws L2pSecurityException, AgentException, NodeException {

		LocalNode testee = LocalNode.newNode();
		adam.unlockPrivateKey("adamspass");
		testee.storeAgent(adam);

		testee.launch();

		try {
			testee.storeAgent(abel);
			fail("L2pSecurityException expected");
		} catch (L2pSecurityException e) {
			// ok
		}

		abel.unlockPrivateKey("abelspass");
		testee.storeAgent(abel);
	}

	@Test
	public void testSimpleInvocation() throws L2pSecurityException, CryptoException, AgentAlreadyRegisteredException,
			AgentException, SecurityException, IllegalArgumentException, AgentNotKnownException, NoSuchMethodException,
			IllegalAccessException, InvocationTargetException, InterruptedException {
		String serviceClass = "i5.las2peer.api.TestService";
		ServiceAgent testService = ServiceAgent.createServiceAgent(
				ServiceNameVersion.fromString(serviceClass + "@1.0"), "a passphrase");
		testService.unlockPrivateKey("a passphrase");

		LocalNode testee = LocalNode.launchNode();

		eve.unlockPrivateKey("evespass");
		testee.storeAgent(eve);
		// eve.lockPrivateKey();

		testee.storeAgent(testService);
		testee.registerReceiver(testService);

		Serializable result = testee.invokeLocally(eve, testService, "inc", new Serializable[] { new Integer(10) });

		assertEquals(12, result);
	}

	@Test
	public void testUserRegistry() throws CryptoException, L2pSecurityException, AgentException {
		UserAgent a = UserAgent.createUserAgent("a");
		UserAgent b = UserAgent.createUserAgent("b");

		a.unlockPrivateKey("a");
		b.unlockPrivateKey("b");

		a.setLoginName("alpha");
		b.setLoginName("beta");

		LocalNode testee = LocalNode.launchNode();
		testee.storeAgent(a);
		testee.storeAgent(b);

		assertEquals(a.getId(), testee.getUserManager().getAgentIdByLogin("alpha"));
		assertEquals(b.getId(), testee.getUserManager().getAgentIdByLogin("beta"));

		try {
			testee.getUserManager().getAgentIdByLogin("bla");
			fail("AgentNotKnownException expected");
		} catch (AgentNotKnownException e) {
			// corrects
		}
	}

	@Test
	public void testUserRegDistribution() throws L2pSecurityException, AgentException, CryptoException,
			ArtifactNotFoundException {
		LocalNode testee1 = LocalNode.launchNode();

		for (int i = 0; i < 11; i++) {
			UserAgent a = UserAgent.createUserAgent("pass" + i);
			a.unlockPrivateKey("pass" + i);
			a.setLoginName("login_" + i);
			testee1.storeAgent(a);
		}

		LocalNode testee2 = LocalNode.launchNode();

		testee2.getUserManager().getAgentIdByLogin("login_2");
	}

}
