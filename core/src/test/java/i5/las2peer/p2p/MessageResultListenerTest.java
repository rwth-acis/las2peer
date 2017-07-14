package i5.las2peer.p2p;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.communication.Message;
import i5.las2peer.security.BasicAgentStorage;
import i5.las2peer.security.InternalSecurityException;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.testing.MockAgentFactory;

public class MessageResultListenerTest {

	public interface TestInt {
		public void testMethod(int i);
	}

	@Test
	public void test() throws InterruptedException, MalformedXMLException, IOException, InternalSecurityException,
			AgentAccessDeniedException, AgentOperationFailedException {
		final UserAgentImpl eve = MockAgentFactory.getEve();
		eve.unlock("evespass");

		final MessageResultListener l = new MessageResultListener(10000) {
			@Override
			public void notifySuccess() {
				System.out.println("success");
			}
		};

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(4000);
					l.collectAnswer(new Message(eve, eve, "fertig"));
				} catch (Exception e) {
				}
			}
		}).start();

		l.waitForOneAnswer();

		assertTrue(l.isSuccess());
	}

	private static boolean test;
	private static boolean test2;

	@Test
	public void testTimeout() {
		try {
			test = false;

			final MessageResultListener testee = new MessageResultListener(1500) {
				@Override
				public void notifySuccess() {
				}

				@Override
				public void notifyTimeout() {
					MessageResultListenerTest.test = true;
				}
			};

			Thread.sleep(3000);

			assertTrue(testee.checkTimeOut());

			assertTrue(testee.isTimedOut());
			assertTrue(testee.isFinished());
			assertFalse(testee.isSuccess());
			assertFalse(testee.hasException());
			assertTrue(test);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testException() {
		test = false;

		final MessageResultListener testee = new MessageResultListener(2000) {
			@Override
			public void notifySuccess() {
			}

			@Override
			public void notifyException(Exception e) {
				MessageResultListenerTest.test = true;
			}
		};

		Exception ex = new Exception();

		testee.collectException(ex);
		assertFalse(testee.isTimedOut());
		assertTrue(testee.isFinished());
		assertFalse(testee.isSuccess());
		assertTrue(testee.hasException());
		assertTrue(test);
		assertEquals(ex, testee.getExceptions()[0]);
	}

	@Test
	public void testMultiple() {
		try {
			final UserAgentImpl eve = MockAgentFactory.getEve();
			eve.unlock("evespass");
			BasicAgentStorage storage = new BasicAgentStorage();
			storage.registerAgent(eve);

			test = test2 = false;

			final MessageResultListener testee = new MessageResultListener(10000) {
				@Override
				public void notifySuccess() {
					MessageResultListenerTest.test2 = true;
				}

				@Override
				public void notifyException(Exception e) {
					MessageResultListenerTest.test = true;
				}
			};

			testee.addRecipient();

			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(2000);
						testee.collectAnswer(new Message(eve, eve, "fertig"));
						Thread.sleep(2000);
						testee.collectException(new Exception());
					} catch (Exception e) {
					}
				}
			}).start();

			testee.waitForAllAnswers();

			Message answer = testee.getResults()[0];
			answer.open(eve, storage);

			assertEquals("fertig", testee.getResults()[0].getContent());
			assertTrue(test);
			assertTrue(test2);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testNumberOfRecipients() {
		MessageResultListener l = new MessageResultListener(2000);
		assertEquals(1, l.getNumberOfExpectedResults());

		l.addRecipient();
		assertEquals(2, l.getNumberOfExpectedResults());

		l.addRecipients(2);
		assertEquals(4, l.getNumberOfExpectedResults());
	}

}
