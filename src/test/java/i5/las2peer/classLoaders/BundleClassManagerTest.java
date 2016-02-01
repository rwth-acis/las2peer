package i5.las2peer.classLoaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import org.junit.Test;

public class BundleClassManagerTest {

	@Test
	public void testSimpleResourceRetrieval() throws IOException {
		BundleClassManager testee = new BundleClassManager(ClassLoader.getSystemClassLoader());
		LibraryClassLoader lib1 = new LibraryClassLoader(LoadedLibraryCache.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar"), testee);
		LibraryClassLoader lib2 = new LibraryClassLoader(LoadedLibraryCache.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage2-1.0.jar"), testee);
		testee.initLibraryLoaders(new LibraryClassLoader[] {lib1,lib2});

		// should exists
		URL test = testee.getResource("i5/las2peer/classLoaders/testPackage1/test.properties");
		assertNotNull(test);
		assertEquals(
				"jar:file:export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar!/i5/las2peer/classLoaders/testPackage1/test.properties",
				test.toString());
		System.out.println(test);

		test = testee.getResource("i5/las2peer/classLoaders/testPackage1/CounterClass.class");
		assertNotNull(test);
		test = testee.getResource("i5/las2peer/classLoaders/testPackage2/UsingCounter.class");
		assertNotNull(test);

		// should not exists
		test = testee.getResource("i5/las2peer/classLoaders/testPackage1/test");
		assertNull(test);
	}

	@Test
	public void testSimpleClassLoading1()
			throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException,
			SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		BundleClassManager testee = new BundleClassManager(ClassLoader.getSystemClassLoader());
		LibraryClassLoader lib1 = new LibraryClassLoader(LoadedLibraryCache.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage1-1.0.jar"), testee);
		LibraryClassLoader lib2 = new LibraryClassLoader(LoadedLibraryCache.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage2-1.0.jar"), testee);
		testee.initLibraryLoaders(new LibraryClassLoader[] {lib1,lib2});

		Class<?> counterClass = testee.loadClass("i5.las2peer.classLoaders.testPackage1.CounterClass");
		Object o = counterClass.newInstance();
		assertEquals("i5.las2peer.classLoaders.testPackage1.CounterClass", o.getClass().getName());

		Method mInc = counterClass.getDeclaredMethod("inc");
		Method mGet = counterClass.getDeclaredMethod("getCounter");

		mInc.invoke(null);
		mInc.invoke(null);
		Integer result = (Integer) mGet.invoke(null);

		assertEquals(2, result.intValue());
		
		assertEquals(lib1, counterClass.getClassLoader());
		Class<?> using = testee.loadClass("i5.las2peer.classLoaders.testPackage2.UsingCounter");
		assertEquals(lib2, using.getClassLoader());
	}

	@Test
	public void testSimpleClassLoading2()
			throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException,
			SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		BundleClassManager testee = new BundleClassManager(ClassLoader.getSystemClassLoader());
		LibraryClassLoader lib1 = new LibraryClassLoader(LoadedLibraryCache.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar"), testee);
		LibraryClassLoader lib2 = new LibraryClassLoader(LoadedLibraryCache.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage2-1.0.jar"), testee);
		testee.initLibraryLoaders(new LibraryClassLoader[] {lib1,lib2});

		Class<?> counterClass = testee.loadClass("i5.las2peer.classLoaders.testPackage1.CounterClass");
		Object o = counterClass.newInstance();
		assertEquals("i5.las2peer.classLoaders.testPackage1.CounterClass", o.getClass().getName());

		Method mInc = counterClass.getDeclaredMethod("inc");
		Method mGet = counterClass.getDeclaredMethod("getCounter");

		mInc.invoke(null);
		mInc.invoke(null);
		Integer result = (Integer) mGet.invoke(null);

		assertEquals(-2, result.intValue());
	}

	@Test
	public void testCompareClasses()
			throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException,
			SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		LoadedLibraryCache cache10 = LoadedLibraryCache.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage1-1.0.jar");
		LoadedLibraryCache cache11 = LoadedLibraryCache.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar");
		LoadedLibraryCache cache20 = LoadedLibraryCache.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage2-1.0.jar");
		
		BundleClassManager testee1 = new BundleClassManager(ClassLoader.getSystemClassLoader());
		LibraryClassLoader lib1 = new LibraryClassLoader(cache11, testee1);
		LibraryClassLoader lib2 = new LibraryClassLoader(cache20, testee1);
		testee1.initLibraryLoaders(new LibraryClassLoader[] {lib1,lib2});
		
		BundleClassManager testee2 = new BundleClassManager(ClassLoader.getSystemClassLoader());
		LibraryClassLoader lib12 = new LibraryClassLoader(cache10, testee2);
		LibraryClassLoader lib22 = new LibraryClassLoader(cache20, testee2);
		testee2.initLibraryLoaders(new LibraryClassLoader[] {lib12,lib22});

		Class<?> counter1 = testee1.loadClass("i5.las2peer.classLoaders.testPackage1.CounterClass");
		Class<?> counter2 = testee2.loadClass("i5.las2peer.classLoaders.testPackage1.CounterClass");

		assertFalse(counter1.equals(counter2));

		Object instance1 = counter1.newInstance();
		Object instance2 = counter2.newInstance();

		assertFalse(instance1.getClass().equals(instance2.getClass()));
		
		Class<?> using1 = testee1.loadClass("i5.las2peer.classLoaders.testPackage2.UsingCounter");
		Class<?> using2 = testee2.loadClass("i5.las2peer.classLoaders.testPackage2.UsingCounter");
		
		assertFalse(using1.equals(using2));
	}
	
	@Test
	public void testSeperateCounters() throws IOException, ClassNotFoundException, SecurityException,
			NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		
		LoadedLibraryCache cache10 = LoadedLibraryCache.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage1-1.0.jar");
		LoadedLibraryCache cache11 = LoadedLibraryCache.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar");
		LoadedLibraryCache cache20 = LoadedLibraryCache.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage2-1.0.jar");
	
		for (int i=0; i<2; i++) {
			BundleClassManager testee1 = new BundleClassManager(ClassLoader.getSystemClassLoader());
			LibraryClassLoader lib1 = new LibraryClassLoader(cache10, testee1);
			LibraryClassLoader lib2 = new LibraryClassLoader(cache20, testee1);
			testee1.initLibraryLoaders(new LibraryClassLoader[] {lib1,lib2});
			
			BundleClassManager testee1_clone = new BundleClassManager(ClassLoader.getSystemClassLoader());
			LibraryClassLoader lib1_clone = new LibraryClassLoader(cache10, testee1_clone);
			LibraryClassLoader lib2_clone = new LibraryClassLoader(cache20, testee1_clone);
			testee1_clone.initLibraryLoaders(new LibraryClassLoader[] {lib1_clone,lib2_clone});
			
			BundleClassManager testee2 = new BundleClassManager(ClassLoader.getSystemClassLoader());
			LibraryClassLoader lib12 = new LibraryClassLoader(cache11, testee2);
			LibraryClassLoader lib22 = new LibraryClassLoader(cache20, testee2);
			testee2.initLibraryLoaders(new LibraryClassLoader[] {lib12,lib22});
			
			testee1.loadClass("i5.las2peer.classLoaders.testPackage1.CounterClass");
			Class<?> user1 = testee1.loadClass("i5.las2peer.classLoaders.testPackage2.UsingCounter");
			Method m1 = user1.getDeclaredMethod("countCalls");
			Object res1 = m1.invoke(null);
			assertEquals(1, ((Integer) res1).intValue());
			res1 = m1.invoke(null);
			assertEquals(2, ((Integer) res1).intValue());
			
			Class<?> user1_clone = testee1_clone.loadClass("i5.las2peer.classLoaders.testPackage2.UsingCounter");
			Method m1_clone = user1_clone.getDeclaredMethod("countCalls");
			Object res1_clone = m1_clone.invoke(null);
			assertEquals(1, ((Integer) res1_clone).intValue());
			res1_clone = m1_clone.invoke(null);
			assertEquals(2, ((Integer) res1_clone).intValue());
			

			Class<?> user2 = testee2.loadClass("i5.las2peer.classLoaders.testPackage2.UsingCounter");
			Method m2 = user2.getDeclaredMethod("countCalls");
			Object res2 = m2.invoke(null);
			assertEquals(-1, ((Integer) res2).intValue());
			res2 = m2.invoke(null);
			assertEquals(-2, ((Integer) res2).intValue());

			m1 = user1.getDeclaredMethod("getPropertyValue");
			m2 = user2.getDeclaredMethod("getPropertyValue");

			res1 = m1.invoke(null);
			res2 = m2.invoke(null);

			assertEquals ( 123, ((Integer) res1).intValue() );
			assertEquals ( 987, ((Integer) res2).intValue() );
		}

	}

	@Test
	public void testGetLoaders() throws IllegalArgumentException, IOException {
		BundleClassManager testee = new BundleClassManager(ClassLoader.getSystemClassLoader());
		LibraryClassLoader lib1 = new LibraryClassLoader(LoadedLibraryCache.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage1-1.0.jar"), testee);
		LibraryClassLoader lib2 = new LibraryClassLoader(LoadedLibraryCache.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage2-1.0.jar"), testee);
		testee.initLibraryLoaders(new LibraryClassLoader[] {lib1,lib2});

		LibraryClassLoader[] check = testee.getLibraryLoaders();
		assertEquals(2, check.length);
		assertTrue(check[0] == lib1 || check[1] == lib1);
		assertTrue(check[0] == lib2 || check[1] == lib2);

	}

}
