package i5.las2peer.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

import i5.las2peer.testing.MockAgentFactory;

public class BasicAgentStorageTest {

	@Test
	public void testStorage() {
		try {
			BasicAgentStorage testee = new BasicAgentStorage();
			UserAgent eve = MockAgentFactory.getEve();

			eve.unlockPrivateKey("evespass");

			assertFalse(testee.hasAgent(eve.getSafeId()));
			testee.registerAgent(eve);

			assertTrue(testee.hasAgent(eve.getSafeId()));

			assertNotSame(eve, testee.getAgent(eve.getSafeId()));

			assertFalse(eve.isLocked());

			Agent eve2 = testee.getAgent(eve.getSafeId());
			assertTrue(eve2.isLocked());

			((UserAgent) eve2).unlockPrivateKey("evespass");
			assertFalse(eve2.isLocked());

			assertTrue(testee.getAgent(eve.getSafeId()).isLocked());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
