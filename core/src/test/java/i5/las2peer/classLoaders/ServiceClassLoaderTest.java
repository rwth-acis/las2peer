package i5.las2peer.classLoaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import i5.las2peer.classLoaders.libraries.LoadedJarLibrary;
import i5.las2peer.classLoaders.libraries.LoadedLibrary;
import i5.las2peer.classLoaders.policies.DefaultPolicy;
import i5.las2peer.classLoaders.policies.RestrictivePolicy;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Properties;

import org.junit.Test;

public class ServiceClassLoaderTest {

	@Test
	public void test() throws IllegalArgumentException, IOException {
		LoadedLibrary lib = LoadedJarLibrary
				.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage2-1.0.jar");
		ServiceClassLoader testee = new ServiceClassLoader(lib, null, new DefaultPolicy());

		assertEquals("i5.las2peer.classLoaders.testPackage2", testee.getLibrary().getIdentifier().getName());
		assertEquals("1.0", testee.getLibrary().getIdentifier().getVersion().toString());
	}

	@Test
	public void testClassLoading() throws IllegalArgumentException, IOException, ClassNotFoundException,
			SecurityException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		LoadedLibrary lib = LoadedJarLibrary
				.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage1-1.0.jar");
		ServiceClassLoader testee = new ServiceClassLoader(lib, null, new DefaultPolicy());

		Class<?> cl = testee.loadClass("i5.las2peer.classLoaders.testPackage1.CounterClass", false);

		Method inc = cl.getDeclaredMethod("inc");
		Method counter = cl.getDeclaredMethod("getCounter");

		inc.invoke(null);
		Object res = counter.invoke(null);

		assertEquals(1, ((Integer) res).intValue());

		assertSame(testee, cl.getClassLoader());

		try {
			testee.loadClass("some.not.existing.class");
			fail("ClassNotFoundException should have been thrown");
		} catch (ClassNotFoundException e) {
		}
	}

	@Test
	public void testResources() throws IllegalArgumentException, IOException {
		LoadedLibrary lib = LoadedJarLibrary
				.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage1-1.0.jar");
		ServiceClassLoader testee = new ServiceClassLoader(lib, null, new DefaultPolicy());

		Properties properties = new Properties();
		properties.load(testee.getResourceAsStream("i5/las2peer/classLoaders/testPackage1/test.properties"));

		assertEquals("123", properties.getProperty("integer"));
		assertEquals("value", properties.getProperty("attribute"));

		URL test = testee.getResource("does/not/exist");
		assertNull(test);

		// how to make sure that the resource was loaded using the LibraryClassLoader and not the platform loader?!?

	}

	@Test
	public void testLoaderBehaviour() throws ClassNotFoundException, IllegalArgumentException, IOException {
		LoadedLibrary lib = LoadedJarLibrary
				.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage1-1.0.jar");
		ServiceClassLoader testee1 = new ServiceClassLoader(lib, null, new DefaultPolicy());
		ServiceClassLoader testee2 = new ServiceClassLoader(lib, null, new DefaultPolicy());

		Class<?> test1 = testee1.loadClass("i5.las2peer.classLoaders.testPackage1.CounterClass", false);
		Class<?> test2 = testee2.loadClass("i5.las2peer.classLoaders.testPackage1.CounterClass", false);

		assertNotSame(test1, test2);
		assertSame(testee1, test1.getClassLoader());
		assertSame(testee2, test2.getClassLoader());

		test2 = testee1.loadClass("i5.las2peer.classLoaders.testPackage1.CounterClass");
		assertEquals(test1, test2);
		assertSame(testee1, test2.getClassLoader());
	}

	@Test
	public void testPackages() throws IllegalArgumentException, IOException, ClassNotFoundException {
		LoadedLibrary lib = LoadedJarLibrary
				.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage1-1.0.jar");
		ServiceClassLoader testee = new ServiceClassLoader(lib, null, new DefaultPolicy());

		Class<?> cl = testee.loadClass("i5.las2peer.classLoaders.testPackage1.CounterClass", false);

		assertSame(testee, cl.getClassLoader());

		assertNotNull(cl.getPackage());

		assertEquals(cl.getPackage().getName(), "i5.las2peer.classLoaders.testPackage1");
	}
	
	@Test
	public void testPolicy() throws IllegalArgumentException, IOException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InvocationTargetException {
		LoadedLibrary lib = LoadedJarLibrary
				.createFromJar("export/jars/i5.las2peer.classLoaders.evilService-1.0.jar");
		ServiceClassLoader testee = new ServiceClassLoader(lib, null, new RestrictivePolicy());

		Class<?> cl = testee.loadClass("i5.las2peer.classLoaders.evilService.EvilService", false);
		
		Method notEvil = cl.getDeclaredMethod("notEvil");
		Method accessNode = cl.getDeclaredMethod("accessNode");
		Method createThread = cl.getDeclaredMethod("createThread");
		
		notEvil.invoke(null);

		try {
			accessNode.invoke(null);
			fail("NoClassDefFoundError expected");
		} catch (InvocationTargetException e) {
			System.out.print(e.getTargetException());
			if (!(e.getTargetException() instanceof NoClassDefFoundError)) {
				fail("NoClassDefFoundError expected");
			}
		}
		
		try {
			createThread.invoke(null);
			fail("NoClassDefFoundError expected");
		} catch (InvocationTargetException e) {
			if (!(e.getTargetException() instanceof NoClassDefFoundError)) {
				fail("NoClassDefFoundError expected");
			}
		}
	}

}
