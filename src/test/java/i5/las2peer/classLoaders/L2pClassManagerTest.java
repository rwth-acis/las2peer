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
		
		testee.getServiceClass("i5.las2peer.classLoaders.testPackage2.UsingCounter", "1.0");
		testee.getServiceClass("i5.las2peer.classLoaders.testPackage1.CounterClass", "1.0");
		testee.getServiceClass("i5.las2peer.classLoaders.testPackage1.CounterClass", "1.1");
		
		assertEquals(3, testee.numberOfRegisteredBundles());
		assertEquals(3, testee.numberOfRegisteredLibraries());
	}
	
	@Test
	public void testMultipleServiceClassLoading() throws ClassLoaderException, SecurityException, NoSuchMethodException,
			IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		L2pClassManager testee = new L2pClassManager(new FileSystemRepository("export/jars/"), ClassLoader.getSystemClassLoader());

		Class<?> cl1 = testee.getServiceClass("i5.las2peer.classLoaders.testPackage2.UsingCounter", "1.0");
		Class<?> cl2 = testee.getServiceClass("i5.las2peer.classLoaders.testPackage2.UsingCounter", "1.0");
		
		assertFalse(cl1.getClassLoader().equals(ClassLoader.getSystemClassLoader()));
		assertFalse(cl2.getClassLoader().equals(ClassLoader.getSystemClassLoader()));
		
		// check that CounterClass is the same
		assertSame(cl1,cl2);
	}
	
}
