package i5.las2peer.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Hashtable;

import org.junit.Test;

public class ServiceTest {

	@Test
	public void propertyFileTest() {
		TestService testee = new TestService();
		Hashtable<String, String> htProps = testee.getProps();

		assertEquals(6, htProps.size());

		assertEquals(-1, testee.getTestInt1());
		assertEquals(200, testee.getTestInt2());

		assertEquals("test1", testee.getTestString1());
		assertEquals("def", testee.getTestString2());
	}

	@Test
	public void testManualDeployment() {
		TestService test1 = new TestService();
		TestService2 test2 = new TestService2();

		assertFalse(test1.isSelfDeployable());
		assertTrue(test2.isSelfDeployable());

		try {
			test2.setFieldValues();
			fail("IllegalStateException expected");
		} catch (IllegalStateException e) {

		}
	}

}
