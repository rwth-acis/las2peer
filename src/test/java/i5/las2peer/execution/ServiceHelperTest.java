package i5.las2peer.execution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

import i5.las2peer.api.TestService;
import i5.las2peer.api.execution.ServiceMethodNotFoundException;
import i5.las2peer.security.L2pSecurityException;

public class ServiceHelperTest {

	@Test
	public void testInvocation() throws SecurityException, IllegalArgumentException, ServiceMethodNotFoundException,
			IllegalAccessException, InvocationTargetException, L2pSecurityException {

		TestService testee = new TestService();
		assertEquals(10, ServiceHelper.execute(testee, "getInt"));

		assertEquals(4, ServiceHelper.execute(testee, "inc", 2));

		assertEquals(4, ServiceHelper.execute(testee, "inc", new Integer(2)));
	}

	@Test
	public void testSubclassParam() throws SecurityException, IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, L2pSecurityException, ServiceMethodNotFoundException {
		TestService testee = new TestService();

		assertEquals("testnachricht",
				ServiceHelper.execute(testee, "subclass", new SecurityException("testnachricht")));
	}

	@Test
	public void testExceptions() throws SecurityException, IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, L2pSecurityException {
		TestService testee = new TestService();

		try {
			ServiceHelper.execute(testee, "privateMethod");
			fail("ServiceMethodNotFoundException expected");
		} catch (ServiceMethodNotFoundException e) {
		}

		try {
			ServiceHelper.execute(testee, "protectedMethod");
			fail("ServiceMethodNotFoundException expected");
		} catch (ServiceMethodNotFoundException e) {
		}
		try {
			ServiceHelper.execute(testee, "staticMethod");
			fail("ServiceMethodNotFoundException expected");
		} catch (ServiceMethodNotFoundException e) {
		}
	}

}
