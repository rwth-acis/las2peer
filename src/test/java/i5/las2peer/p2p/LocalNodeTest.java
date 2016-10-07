package i5.las2peer.p2p;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.communication.Message;
import i5.las2peer.communication.PingPongContent;
import i5.las2peer.p2p.Node.SendMode;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.Mediator;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.tools.SerializationException;

import java.io.Serializable;
import java.util.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LocalNodeTest {

	private UserAgentImpl eve;
	private UserAgentImpl adam;
	private UserAgentImpl abel;

	private static int counter;

	@Before
	public void setUp() {
		try {
			LocalNode.reset();

			eve = MockAgentFactory.getEve();
			adam = MockAgentFactory.getAdam();
			abel = MockAgentFactory.getAbel();

			counter = 0;
			testVariable = false;
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	private static boolean testVariable;

	@Test
	public void test() throws EncodingFailedException, L2pSecurityException, SerializationException,
			InterruptedException, AgentException, AgentAccessDeniedException, i5.las2peer.security.AgentException {
		System.out.println("start: " + new Date());

		LocalNode testee = LocalNode.launchNode();

		try {
			testee.registerReceiver(eve);
			fail("L2pSecurityException expected");
		} catch (L2pSecurityException e) {
		}
		eve.unlock("evespass");
		adam.unlock("adamspass");

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
			InterruptedException, AgentException, AgentAccessDeniedException, i5.las2peer.security.AgentException {
		adam.unlock("adamspass");
		eve.unlock("evespass");

		// launch to nodes with one agent each
		LocalNode testee1 = LocalNode.launchAgent(adam);
		LocalNode.launchAgent(eve);

		assertTrue(LocalNode.findAllNodesWithAgent(adam.getSafeId()).length > 0);
		assertTrue(LocalNode.findAllNodesWithAgent(eve.getSafeId()).length > 0);

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
			InterruptedException, AgentException, AgentAccessDeniedException, i5.las2peer.security.AgentException {
		adam.unlock("adamspass");

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
			InterruptedException, AgentException, AgentAccessDeniedException, i5.las2peer.security.AgentException {
		adam.unlock("adamspass");
		eve.unlock("evespass");

		// launch three nodes with one agent each
		LocalNode testee1 = LocalNode.launchAgent(adam);
		LocalNode hosting1 = LocalNode.launchAgent(eve);
		assertEquals(1, LocalNode.findAllNodesWithAgent(eve.getSafeId()).length);

		LocalNode hosting2 = LocalNode.launchAgent(eve);

		assertTrue(hosting1.hasLocalAgent(eve));
		assertTrue(hosting2.hasLocalAgent(eve));

		assertNotSame(hosting1.getAgent(eve.getSafeId()), hosting2.getAgent(eve.getSafeId()));

		assertEquals(2, LocalNode.findAllNodesWithAgent(eve.getSafeId()).length);

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
	public void testPending() {
		try {
			adam.unlock("adamspass");
			eve.unlock("evespass");

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
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testRegisteringAgents() {
		try {
			adam.unlock("adamspass");

			LocalNode testee = LocalNode.launchAgent(adam);

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

			abel.unlock("abelspass");
			testee.storeAgent(abel);

			LocalNode testee2 = LocalNode.launchNode();

			UserAgentImpl retrieve = (UserAgentImpl) testee2.getAgent(abel.getSafeId());
			assertTrue(retrieve.isLocked());

			try {
				testee2.updateAgent(retrieve);
				fail("SecurtityException expected");
			} catch (L2pSecurityException e) {
			}

			retrieve.unlock("abelspass");
			testee2.updateAgent(retrieve);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testRegisteringTopics() {
		try {
			// start node
			adam.unlock("adamspass");
			abel.unlock("abelspass");
			eve.unlock("evespass");
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

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testSendAndReceiveTopics() {
		try {
			// start node
			adam.unlock("adamspass");
			abel.unlock("abelspass");
			eve.unlock("evespass");
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
			assertTrue(received1.getRecipientId().equalsIgnoreCase(adam.getSafeId()));
			assertTrue(received2.getRecipientId().equalsIgnoreCase(abel.getSafeId()));
			assertTrue(received3.getRecipientId().equalsIgnoreCase(eve.getSafeId()));

			assertTrue(received3.getSenderId().equalsIgnoreCase(adam.getSafeId()));
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
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testCollectMessags() {
		try {
			// start node
			adam.unlock("adamspass");
			abel.unlock("abelspass");
			eve.unlock("evespass");
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
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testStartupAgents() {
		try {

			LocalNode testee = LocalNode.newNode();
			adam.unlock("adamspass");
			testee.storeAgent(adam);

			testee.launch();

			try {
				testee.storeAgent(abel);
				fail("L2pSecurityException expected");
			} catch (L2pSecurityException e) {
				// ok
			}

			abel.unlock("abelspass");
			testee.storeAgent(abel);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testSimpleInvocation() {
		try {
			String serviceClass = "i5.las2peer.api.TestService";
			ServiceAgentImpl testService = ServiceAgentImpl.createServiceAgent(
					ServiceNameVersion.fromString(serviceClass + "@1.0"), "a passphrase");
			testService.unlock("a passphrase");

			LocalNode testee = LocalNode.launchNode();

			eve.unlock("evespass");
			testee.storeAgent(eve);

			testee.storeAgent(testService);
			testee.registerReceiver(testService);

			Serializable result = testee.invokeLocally(eve, testService, "inc", new Serializable[] { new Integer(10) });

			assertEquals(12, result);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testUserRegistry() {
		try {
			UserAgentImpl a = UserAgentImpl.createUserAgent("a");
			UserAgentImpl b = UserAgentImpl.createUserAgent("b");

			a.unlock("a");
			b.unlock("b");
			a.setLoginName("alpha");
			b.setLoginName("beta");

			LocalNode testee = LocalNode.launchNode();
			testee.storeAgent(a);
			testee.storeAgent(b);

			assertEquals(a.getSafeId(), testee.getUserManager().getAgentIdByLogin("alpha"));
			assertEquals(b.getSafeId(), testee.getUserManager().getAgentIdByLogin("beta"));

			try {
				testee.getUserManager().getAgentIdByLogin("bla");
				fail("AgentNotKnownException expected");
			} catch (AgentNotKnownException e) {
				// corrects
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testUserRegDistribution() {
		try {
			LocalNode testee1 = LocalNode.launchNode();

			for (int i = 0; i < 11; i++) {
				UserAgentImpl a = UserAgentImpl.createUserAgent("pass" + i);
				a.unlock("pass" + i);
				a.setLoginName("login_" + i);
				testee1.storeAgent(a);
			}

			LocalNode testee2 = LocalNode.launchNode();

			testee2.getUserManager().getAgentIdByLogin("login_2");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
