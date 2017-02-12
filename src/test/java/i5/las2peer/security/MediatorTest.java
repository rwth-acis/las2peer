package i5.las2peer.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.testing.MockAgentFactory;

//NOTE: Answering each message is disabled in testing mode (since there exists no real las2peer context here).
//Also refer to the Mediator implementation for more information.
public class MediatorTest {

	public class MessageAcceptor implements MessageHandler {

		private int accepted = 0;

		@Override
		public boolean handleMessage(Message message, AgentContext context) throws Exception {
			accepted++;
			return true;
		}

		public int getAccepted() {
			return accepted;
		}

	}

	public class MessageDenier implements MessageHandler {

		private int denied = 0;

		@Override
		public boolean handleMessage(Message message, AgentContext context) throws Exception {
			denied++;
			return false;
		}

		public int getDenied() {
			return denied;
		}

	}

	@Test
	public void testConstructor() {
		try {
			UserAgent eve = MockAgentFactory.getEve();

			try {
				new Mediator(null, eve);
				fail("L2pSecurityException expected");
			} catch (L2pSecurityException e) {
				// that's expected!
			}

			eve.unlockPrivateKey("evespass");
			Mediator testee = new Mediator(null, eve);

			assertEquals(eve.getSafeId(), testee.getResponsibleForAgentSafeId());

			assertFalse(testee.hasMessages());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testMessages() {
		try {
			UserAgent eve = MockAgentFactory.getEve();
			eve.unlockPrivateKey("evespass");
			Mediator testee = new Mediator(null, eve);

			assertFalse(testee.hasMessages());
			assertEquals(0, testee.getNumberOfWaiting());

			AgentContext c = new AgentContext(null, eve);

			String content1 = "some content";
			String content2 = "a 2nd message";
			Message m1 = new Message(eve, eve, content1);
			Message m2 = new Message(eve, eve, content2);

			testee.receiveMessage(m1, c);
			assertTrue(testee.hasMessages());
			assertEquals(1, testee.getNumberOfWaiting());

			testee.receiveMessage(m2, c);
			assertTrue(testee.hasMessages());
			assertEquals(2, testee.getNumberOfWaiting());

			Message rec = testee.getNextMessage();
			assertTrue(testee.hasMessages());
			assertEquals(1, testee.getNumberOfWaiting());

			rec.open(eve, null);
			assertEquals(content1, rec.getContent());

			rec = testee.getNextMessage();
			rec.open(eve, null);
			assertEquals(content2, rec.getContent());
			assertFalse(testee.hasMessages());
			assertEquals(0, testee.getNumberOfWaiting());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testWrongRecipient() {
		try {
			UserAgent eve = MockAgentFactory.getEve();
			eve.unlockPrivateKey("evespass");
			UserAgent adam = MockAgentFactory.getAdam();
			adam.unlockPrivateKey("adamspass");
			Mediator testee = new Mediator(null, eve);

			Message m = new Message(eve, adam, "a message");

			try {
				testee.receiveMessage(m, null);
				fail("MessageException expected!");
			} catch (MessageException e) {
				// expected
				assertTrue(e.getMessage().contains("not responsible"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testReceiverHook() {
		try {
			UserAgent eve = MockAgentFactory.getEve();
			eve.unlockPrivateKey("evespass");

			final HashSet<String> got = new HashSet<>();

			Mediator testee = new Mediator(null, eve) {
				@Override
				public boolean workOnMessage(Message message, AgentContext c) {
					try {
						if (message.getContent() instanceof String) {
							got.add((String) message.getContent());
							return true;
						} else {
							return false;
						}
					} catch (L2pSecurityException e) {
						return false;
					}
				}
			};
			AgentContext c = new AgentContext(null, eve);

			testee.receiveMessage(new Message(eve, eve, 100), c);

			assertTrue(testee.hasMessages());
			assertEquals(0, got.size());
			testee.getNextMessage();

			assertFalse(testee.hasMessages());

			testee.receiveMessage(new Message(eve, eve, "a string"), c);
			assertFalse(testee.hasMessages());

			assertEquals(1, got.size());
			assertTrue(got.contains("a string"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testHandlersAcceptBeforeDeny() {
		try {
			UserAgent eve = MockAgentFactory.getEve();
			eve.unlockPrivateKey("evespass");
			AgentContext c = new AgentContext(null, eve);

			Mediator testee = new Mediator(null, eve);

			MessageAcceptor acceptor = new MessageAcceptor();
			testee.registerMessageHandler(acceptor);

			MessageDenier denier = new MessageDenier();
			testee.registerMessageHandler(denier);

			testee.receiveMessage(new Message(eve, eve, "some content"), c);
			assertEquals(1, acceptor.getAccepted());
			assertEquals(0, denier.getDenied());
			assertEquals(0, testee.getNumberOfWaiting());

			testee.unregisterMessageHandler(acceptor);
			testee.receiveMessage(new Message(eve, eve, "some content"), c);
			assertEquals(1, acceptor.getAccepted());
			assertEquals(1, denier.getDenied());

			assertEquals(1, testee.getNumberOfWaiting());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testHandlersDenyBeforeAccept() {
		try {
			UserAgent eve = MockAgentFactory.getEve();
			eve.unlockPrivateKey("evespass");
			AgentContext c = new AgentContext(null, eve);

			Mediator testee = new Mediator(null, eve);

			MessageDenier denier = new MessageDenier();
			assertFalse(testee.hasMessageHandlerClass(MessageDenier.class));
			testee.registerMessageHandler(denier);
			assertTrue(testee.hasMessageHandlerClass(MessageDenier.class));
			assertTrue(testee.hasMessageHandler(denier));

			MessageAcceptor acceptor = new MessageAcceptor();
			assertFalse(testee.hasMessageHandlerClass(MessageAcceptor.class));
			testee.registerMessageHandler(acceptor);
			assertTrue(testee.hasMessageHandler(acceptor));
			assertTrue(testee.hasMessageHandlerClass(MessageAcceptor.class));

			testee.receiveMessage(new Message(eve, eve, "some content"), c);

			assertEquals(1, acceptor.getAccepted());
			assertEquals(1, denier.getDenied());
			assertEquals(0, testee.getNumberOfWaiting());

			testee.unregisterMessageHandlerClass(MessageDenier.class.getName());
			assertFalse(testee.hasMessageHandler(denier));
			assertFalse(testee.hasMessageHandlerClass(MessageDenier.class));

			testee.receiveMessage(new Message(eve, eve, "some content"), c);
			assertEquals(2, acceptor.getAccepted());
			assertEquals(1, denier.getDenied());
			assertEquals(0, testee.getNumberOfWaiting());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
