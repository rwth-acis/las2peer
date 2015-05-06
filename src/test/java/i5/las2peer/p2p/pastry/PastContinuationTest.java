package i5.las2peer.p2p.pastry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.Agent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;

import java.io.IOException;

import org.junit.Test;

public class PastContinuationTest {

	@Test
	public void testSimpleRetrieval() throws L2pSecurityException,
			MalformedXMLException, IOException {
		PastGetContinuation<Agent> testee = new PastGetContinuation<Agent>(
				Agent.class, 20000);

		assertFalse(testee.isFinished());
		assertFalse(testee.hasException());
		assertNull(testee.getException());
		assertNull(testee.getResult());

		UserAgent eve = MockAgentFactory.getEve();
		eve.unlockPrivateKey("evespass");
		testee.receiveResult(new ContentEnvelope(eve));

		assertTrue(testee.isFinished());
		assertFalse(testee.hasException());
		assertNull(testee.getException());
		assertEquals(eve.getId(), testee.getResult().getId());
	}

	@Test
	public void testCastException() throws MalformedXMLException, IOException,
			L2pSecurityException {
		PastGetContinuation<Envelope> testee = new PastGetContinuation<Envelope>(
				Envelope.class, 20000);

		assertFalse(testee.isFinished());
		assertFalse(testee.hasException());
		assertNull(testee.getException());
		assertNull(testee.getResult());

		UserAgent eve = MockAgentFactory.getEve();
		eve.unlockPrivateKey("evespass");
		testee.receiveResult(new ContentEnvelope(eve));

		assertTrue(testee.isFinished());
		assertTrue(testee.hasException());
		assertNull(testee.getResult());

		assertTrue(testee.getException() instanceof PastryStorageException);
		assertTrue(testee.getException().getCause() instanceof ClassCastException);
	}

	@Test
	public void testWaiting() throws Exception {

		final PastGetContinuation<Agent> testee = new PastGetContinuation<Agent>(
				Agent.class, 20000);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(3000);

					testee.receiveResult(new ContentEnvelope(MockAgentFactory
							.getEve()));
				} catch (InterruptedException e) {
				} catch (MalformedXMLException e) {
				} catch (IOException e) {
				}
			}

		}).start();

		Agent result = testee.getResultWaiting();

		assertTrue(testee.isFinished());
		assertEquals(MockAgentFactory.getEve().getId(), result.getId());
		assertFalse(testee.hasException());
		assertNull(testee.getException());
		assertSame(result, testee.getResult());
	}

	@Test
	public void testExceptionWaiting() throws Exception {

		final PastGetContinuation<Agent> testee = new PastGetContinuation<Agent>(
				Agent.class, 20000);
		final Exception testException = new Exception("a test");

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(3000);

					testee.receiveException(testException);
				} catch (InterruptedException e) {
				}
			}

		}).start();

		try {
			testee.getResultWaiting();
			fail("exception expected");
		} catch (Exception e) {
			assertSame(testException, e);
		}

		assertSame(testException, testee.getException());
		assertTrue(testee.isFinished());
		assertTrue(testee.hasException());
		assertNull(testee.getResult());
	}

}
