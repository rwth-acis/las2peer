package i5.las2peer.classLoaders;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import i5.las2peer.classLoaders.libraries.FileSystemRepository;

import org.junit.Test;

public class L2pClassLoaderTest {

	@Test
	public void testPackageName() {
		assertEquals ( "my.package", L2pClassLoader.getPackageName("my.package.Class"));

		try {
			L2pClassLoader.getPackageName("teststring");
			fail ( "IllegalArgumentException should have been thrown");
		} catch ( IllegalArgumentException e ) {}
	}


	@Test
	public void testServiceClassLoading () throws ClassLoaderException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		L2pClassLoader testee = new L2pClassLoader ( new FileSystemRepository ("export/jars/"), null);

		Class<?> cl = testee.getServiceClass("i5.las2peer.classLoaders.testPackage2.UsingCounter", "1.0" );

		Method m = cl.getDeclaredMethod("countCalls");
		Object result = m.invoke(null);
		result = m.invoke(null );

		assertEquals ( -2, ((Integer) result).intValue());
	}


	@Test
	public void testJarBehaviour () throws IllegalArgumentException, ClassLoaderException {
		L2pClassLoader testee = new L2pClassLoader ( new FileSystemRepository ("export/jars/"), null);
		testee.getServiceClass("i5.las2peer.classLoaders.testPackage2.UsingCounter", "1.0" );

		assertEquals ( 1, testee.numberOfRegisteredBundles() );
		assertEquals ( 2, testee.numberOfRegisteredLibraries() );

		testee.unregisterService("i5.las2peer.classLoaders.testPackage2.UsingCounter", "1.0" );

		assertEquals ( 0, testee.numberOfRegisteredBundles() );
		assertEquals ( 0, testee.numberOfRegisteredLibraries() );
	}

}
