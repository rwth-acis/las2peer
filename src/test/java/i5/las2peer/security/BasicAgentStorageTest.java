package i5.las2peer.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import i5.las2peer.testing.MockAgentFactory;

import org.junit.Assert;
import org.junit.Test;

public class BasicAgentStorageTest {

	@Test
	public void testStorage() {
		try {
			BasicAgentStorage testee = new BasicAgentStorage();
			UserAgentImpl eve = MockAgentFactory.getEve();

			eve.unlock("evespass");

			assertFalse(testee.hasAgent(eve.getSafeId()));
			testee.registerAgent(eve);

			assertTrue(testee.hasAgent(eve.getSafeId()));

			assertNotSame(eve, testee.getAgent(eve.getSafeId()));

			assertFalse(eve.isLocked());

			UserAgentImpl eve2 = (UserAgentImpl) testee.getAgent(eve.getSafeId());
			assertTrue(eve2.isLocked());

			eve2.unlock("evespass");
			assertFalse(eve2.isLocked());

			assertTrue(testee.getAgent(eve.getSafeId()).isLocked());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
