package i5.las2peer.classLoaders;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Test;

import i5.las2peer.classLoaders.libraries.FileSystemRepository;

public class L2pClassManagerTest {

	@Test
	public void testPackageName() {
		assertEquals("my.package", L2pClassManager.getPackageName("my.package.Class"));

		try {
			L2pClassManager.getPackageName("teststring");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testServiceClassLoading() throws ClassLoaderException, SecurityException, NoSuchMethodException,
			IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		L2pClassManager testee = new L2pClassManager(new FileSystemRepository("export/jars/"), ClassLoader.getSystemClassLoader());

		Class<?> cl = testee.getServiceClass("i5.las2peer.classLoaders.testPackage2.UsingCounter", "1.0");
		
		assertFalse(cl.getClassLoader().equals(ClassLoader.getSystemClassLoader()));
		
		Method m = cl.getDeclaredMethod("countCalls");
		Object result = m.invoke(null);
		result = m.invoke(null);

		assertEquals(-2, ((Integer) result).intValue());
	}

	@Test
	public void testJarBehaviour() throws IllegalArgumentException, ClassLoaderException {
		L2pClassManager testee = new L2pClassManager(new FileSystemRepository("export/jars/"), ClassLoader.getSystemClassLoader());
		testee.getServiceClass("i5.las2peer.classLoaders.testPackage2.UsingCounter", "1.0");

		assertEquals(1, testee.numberOfRegisteredBundles());
		assertEquals(2, testee.numberOfRegisteredLibraries());

		testee.unregisterService("i5.las2peer.classLoaders.testPackage2.UsingCounter", "1.0");

		assertEquals(0, testee.numberOfRegisteredBundles());
		assertEquals(0, testee.numberOfRegisteredLibraries());
	}
	
	// TODO ADD test to test reusing of library caches

}
