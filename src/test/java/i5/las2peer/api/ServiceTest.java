package i5.las2peer.api;

import static org.junit.Assert.*;

import i5.las2peer.execution.NoSuchServiceMethodException;
import i5.las2peer.security.L2pSecurityException;

import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;

import org.junit.Test;

public class ServiceTest {

	@Test
	public void testInvocation() throws SecurityException, IllegalArgumentException, NoSuchMethodException,
			IllegalAccessException, InvocationTargetException, L2pSecurityException, NoSuchServiceMethodException {

		TestService testee = new TestService();
		assertEquals(10, testee.execute("getInt"));

		assertEquals(4, testee.execute("inc", 2));

		assertEquals(4, testee.execute("inc", new Integer(2)));
	}

	@Test
	public void testSubclassParam() throws SecurityException, IllegalArgumentException, NoSuchMethodException,
			IllegalAccessException, InvocationTargetException, L2pSecurityException, NoSuchServiceMethodException {
		TestService testee = new TestService();

		assertEquals("testnachricht", testee.execute("subclass", new SecurityException("testnachricht")));
	}

	@Test
	public void testExceptions() throws SecurityException, IllegalArgumentException, NoSuchMethodException,
			IllegalAccessException, InvocationTargetException, L2pSecurityException, NoSuchServiceMethodException {
		TestService testee = new TestService();

		try {
			testee.execute("privateMethod");
			fail("NoSuchServiceMethodException expected");
		} catch (NoSuchServiceMethodException e) {
		}

		try {
			testee.execute("protectedMethod");
			fail("NoSuchMethodException expected");
		} catch (NoSuchServiceMethodException e) {
		}
		try {
			testee.execute("staticMethod");
			fail("NoSuchServiceMethodException expected");
		} catch (NoSuchServiceMethodException e) {
		}
	}

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

}
