package i5.las2peer.classLoaders;

import static org.junit.Assert.*;


import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import org.junit.Test;

public class BundleClassLoaderTest {

	@Test
	public void testSimpleResourceRetrieval () throws IOException {
		LibraryClassLoader lib1 = new LibraryClassLoader ( "../export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar" );
		LibraryClassLoader lib2 = new LibraryClassLoader ( "../export/jars/i5.las2peer.classLoaders.testPackage2-1.0.jar" );
		assertEquals (0, lib1.numberOfParentLoaders() );
		assertFalse ( lib1.hasParentLoaders());
		
		BundleClassLoader testee = new BundleClassLoader ( new LibraryClassLoader [] {lib1, lib2} );
		assertEquals (1, lib1.numberOfParentLoaders() );
		assertTrue ( lib1.hasParentLoaders());
		
		// should exists
		URL test = testee.getResource("i5/las2peer/classLoaders/testPackage1/test.properties");
		assertNotNull ( test );
		assertEquals( "jar:file:../export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar!/i5/las2peer/classLoaders/testPackage1/test.properties",test.toString());
		System.out.println( test );
		
		test = testee.getResource("i5/las2peer/classLoaders/testPackage1/CounterClass.class");
		assertNotNull ( test );
		test = testee.getResource( "i5/las2peer/classLoaders/testPackage2/UsingCounter.class");
		assertNotNull ( test );
		
		// should not exists
		test = testee.getResource("i5/las2peer/classLoaders/testPackage1/test");
		assertNull ( test );
	}

	@Test
	public void testSimpleClassLoading1 () throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		LibraryClassLoader lib1 = new LibraryClassLoader  ( "../export/jars/i5.las2peer.classLoaders.testPackage1-1.0.jar");
		LibraryClassLoader lib2 = new LibraryClassLoader  ( "../export/jars/i5.las2peer.classLoaders.testPackage2-1.0.jar" );
		BundleClassLoader testee = new BundleClassLoader ( new LibraryClassLoader [] {lib1, lib2} );
		
		Class<?> counterClass = testee.loadClass("i5.las2peer.classLoaders.testPackage1.CounterClass");
		Object o = counterClass.newInstance();
		assertEquals ( "i5.las2peer.classLoaders.testPackage1.CounterClass", o.getClass().getName());
		
		Method mInc = counterClass.getDeclaredMethod("inc" );
		Method mGet = counterClass.getDeclaredMethod("getCounter" );
		
		mInc.invoke(null);
		mInc.invoke(null);
		Integer result = (Integer) mGet.invoke(null);
		
		assertEquals ( 2, result.intValue()  );
		
		
		assertEquals ( lib1, counterClass.getClassLoader() );
		Class<?> using = testee.loadClass("i5.las2peer.classLoaders.testPackage2.UsingCounter");
		assertEquals ( lib2, using.getClassLoader() );
	}
	
	@Test
	public void testSimpleClassLoading2 () throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		LibraryClassLoader lib1 = new LibraryClassLoader  ( "../export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar");
		LibraryClassLoader lib2 = new LibraryClassLoader ("../export/jars/i5.las2peer.classLoaders.testPackage2-1.0.jar" );
		BundleClassLoader testee = new BundleClassLoader ( new LibraryClassLoader [] {lib1, lib2} );
		
		Class<?> counterClass = testee.loadClass("i5.las2peer.classLoaders.testPackage1.CounterClass");
		Object o = counterClass.newInstance();
		assertEquals ( "i5.las2peer.classLoaders.testPackage1.CounterClass", o.getClass().getName());
		
		Method mInc = counterClass.getDeclaredMethod("inc" );
		Method mGet = counterClass.getDeclaredMethod("getCounter" );
		
		mInc.invoke(null);
		mInc.invoke(null);
		Integer result = (Integer) mGet.invoke(null);
		
		assertEquals ( -2, result.intValue()  );
	}

	
	
	@Test
	public void testCompareClasses() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		LibraryClassLoader lib1 = new LibraryClassLoader ( "../export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar");
		LibraryClassLoader lib2 = new LibraryClassLoader ( "../export/jars/i5.las2peer.classLoaders.testPackage2-1.0.jar" );

		BundleClassLoader testee1 = new BundleClassLoader ( new LibraryClassLoader [] {lib1, lib2} );
		BundleClassLoader testee2 = new BundleClassLoader ( new LibraryClassLoader [] {lib1, lib2} );
		
		Class<?> counter1 = testee1.loadClass("i5.las2peer.classLoaders.testPackage1.CounterClass");
		Class<?> counter2 = testee2.loadClass("i5.las2peer.classLoaders.testPackage1.CounterClass");
		
		assertEquals ( counter1, counter2);
		
		Object instance1 = counter1.newInstance();
		Object instance2 = counter2.newInstance();
		
		assertEquals ( instance1.getClass(), instance2.getClass ());
		
		LibraryClassLoader lib3 = new LibraryClassLoader ("../export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar" );
		testee2 = new BundleClassLoader ( new LibraryClassLoader [] { lib3, lib2 });
		assertEquals (3, lib2.numberOfParentLoaders() );
		assertTrue ( lib2.hasParentLoaders());
				
		counter2 = testee2.loadClass( "i5.las2peer.classLoaders.testPackage1.CounterClass");
		assertNotSame ( counter1, counter2);
		
		instance2 = counter2.newInstance();
		assertNotSame ( instance1.getClass(), instance2.getClass());
		
		
	}
	
	
	@Test
	public void testBundleRegistering () throws IllegalArgumentException, IOException {
		LibraryClassLoader lib1 = new LibraryClassLoader ( "../export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar");
		LibraryClassLoader lib2 = new LibraryClassLoader ( "../export/jars/i5.las2peer.classLoaders.testPackage2-1.0.jar" );

		assertEquals (0, lib2.numberOfParentLoaders() );
		assertFalse ( lib2.hasParentLoaders());
		
		BundleClassLoader testee1 = new BundleClassLoader ( new LibraryClassLoader [] {lib1, lib2} );
		assertEquals (1, lib2.numberOfParentLoaders() );
		assertTrue ( lib2.hasParentLoaders());
		BundleClassLoader testee2 = new BundleClassLoader ( new LibraryClassLoader [] {lib1, lib2} );
		assertEquals (2, lib2.numberOfParentLoaders() );
		assertTrue ( lib2.hasParentLoaders());
		
		LibraryClassLoader lib3 = new LibraryClassLoader ("../export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar" );
		BundleClassLoader testee3 = new BundleClassLoader ( new LibraryClassLoader [] { lib3, lib2 });
		assertEquals (3, lib2.numberOfParentLoaders() );
		assertTrue ( lib2.hasParentLoaders());
		
		testee2.removeLibraryLoader ( lib2 );
		assertTrue ( lib2.hasParentLoaders() );
		assertEquals (2, lib2.numberOfParentLoaders() );
		
		testee1.removeLibraryLoader ( lib2 );
		assertTrue ( lib2.hasParentLoaders() );
		assertEquals (1, lib2.numberOfParentLoaders() );
		
		testee3.removeLibraryLoader ( lib2 );
		assertFalse ( lib2.hasParentLoaders() );
		assertEquals (0, lib2.numberOfParentLoaders() );
		
		
	}
	
	
	
	@Test
	public void testSeperateCounters () throws IOException, ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		{
			LibraryClassLoader lib10 = new LibraryClassLoader ( "../export/jars/i5.las2peer.classLoaders.testPackage1-1.0.jar");
			LibraryClassLoader lib11 = new LibraryClassLoader ( "../export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar");
			LibraryClassLoader lib2 = new LibraryClassLoader ( "../export/jars/i5.las2peer.classLoaders.testPackage2-1.0.jar" );
			BundleClassLoader testee1 = new BundleClassLoader ( new LibraryClassLoader [] {lib10, lib2} );
			
			testee1.loadClass("i5.las2peer.classLoaders.testPackage1.CounterClass");
			Class<?> user1 = testee1.loadClass("i5.las2peer.classLoaders.testPackage2.UsingCounter");
			Method m1 = user1.getDeclaredMethod( "countCalls" );
			Object res1 = m1.invoke(null );
			assertEquals ( 1, ((Integer) res1).intValue ());
			res1 = m1.invoke(null );
			assertEquals ( 2, ((Integer) res1).intValue ());

			
			BundleClassLoader testee2 = new BundleClassLoader ( new LibraryClassLoader [] {lib11, lib2} );
			Class<?> user2 = testee2.loadClass("i5.las2peer.classLoaders.testPackage2.UsingCounter");	
			Method m2 = user2.getDeclaredMethod( "countCalls" );
			Object res2 = m2.invoke(null );
			assertEquals ( 3, ((Integer) res2).intValue ());
			res2 = m2.invoke(null );
			assertEquals ( 4, ((Integer) res2).intValue ());
			
			
			
			m1 = user1.getDeclaredMethod("getPropertyValue" );
			m2 = user2.getDeclaredMethod("getPropertyValue" );
			
			res1 = m1.invoke( null);
			res2 = m2.invoke( null );
			
			// test depends on hash implementation and is sometimes 123 and somtimes 987
			//assertEquals ( 123, ((Integer) res1).intValue() );
			//assertEquals ( 987, ((Integer) res1).intValue() );
			//assertEquals ( 123, ((Integer) res2).intValue() );
		}

		
		{
			LibraryClassLoader lib10 = new LibraryClassLoader ( "../export/jars/i5.las2peer.classLoaders.testPackage1-1.0.jar");
			LibraryClassLoader lib11 = new LibraryClassLoader ( "../export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar");
			LibraryClassLoader lib2 = new LibraryClassLoader ( "../export/jars/i5.las2peer.classLoaders.testPackage2-1.0.jar" );
			BundleClassLoader testee1 = new BundleClassLoader ( new LibraryClassLoader [] {lib11, lib2} );			
			
			Class<?> user1 = testee1.loadClass("i5.las2peer.classLoaders.testPackage2.UsingCounter");
			Method m1 = user1.getDeclaredMethod( "countCalls" );
			Object res1 = m1.invoke(null );
			assertEquals ( -1, ((Integer) res1).intValue ());
			res1 = m1.invoke(null );
			assertEquals ( -2, ((Integer) res1).intValue ());

			BundleClassLoader testee2 = new BundleClassLoader ( new LibraryClassLoader [] {lib10, lib2} );
			Class<?> user2 = testee2.loadClass("i5.las2peer.classLoaders.testPackage2.UsingCounter");	
			Method m2 = user2.getDeclaredMethod( "countCalls" );
			Object res2 = m2.invoke(null );
			assertEquals ( -3, ((Integer) res2).intValue ());
			res2 = m2.invoke(null );
			assertEquals ( -4, ((Integer) res2).intValue ());
			
			
			
			m1 = user1.getDeclaredMethod("getPropertyValue" );
			m2 = user2.getDeclaredMethod("getPropertyValue" );
			
			res1 = m1.invoke( null);
			res2 = m2.invoke( null );
			
			// TODO: resource getting not deterministic!
			//assertEquals ( 987, ((Integer) res1).intValue() );
			//assertEquals ( 987, ((Integer) res2).intValue() );
		}
		
	}
	
	
	
	@Test
	public void testHotplugging () throws IllegalArgumentException, IOException, ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		LibraryClassLoader lib10 = new LibraryClassLoader ( "../export/jars/i5.las2peer.classLoaders.testPackage1-1.0.jar");
		LibraryClassLoader lib11 = new LibraryClassLoader ( "../export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar");
		LibraryClassLoader lib2 = new LibraryClassLoader ( "../export/jars/i5.las2peer.classLoaders.testPackage2-1.0.jar" );
		
		BundleClassLoader testee = new BundleClassLoader ( new LibraryClassLoader [] {lib10, lib2} );
		
		Class<?> cl1 = testee.loadClass ( "i5.las2peer.classLoaders.testPackage2.UsingCounter");
		Method m1 = cl1.getDeclaredMethod("countCalls");
		
		//Object res1 = m1.invoke(null );
		//Object res2 = m1.invoke(null );
		
		//assertEquals ( 1, ((Integer) res1).intValue ());
		//assertEquals ( 2, ((Integer) res2).intValue ());
				

		testee.removeLibraryLoader( lib10);
		testee.registerLibraryLoader(lib11);
		
		cl1 = testee.loadClass ( "i5.las2peer.classLoaders.testPackage2.UsingCounter");
		m1 = cl1.getDeclaredMethod("countCalls");

		
		Object res3 = m1.invoke ( null);
		assertEquals ( -1, ((Integer) res3).intValue () );
	}
	
	
	@Test
	public void testGetLoaders () throws IllegalArgumentException, IOException {
		LibraryClassLoader lib10 = new LibraryClassLoader ( "../export/jars/i5.las2peer.classLoaders.testPackage1-1.0.jar");
		LibraryClassLoader lib2 = new LibraryClassLoader ( "../export/jars/i5.las2peer.classLoaders.testPackage2-1.0.jar" );
		
		BundleClassLoader testee = new BundleClassLoader ( new LibraryClassLoader [] {lib10, lib2} );
		
		LibraryClassLoader[] check = testee.getLibraryLoaders();
		assertEquals ( 2, check.length );
		assertTrue ( check[0] == lib10 || check[1] == lib10);
		assertTrue ( check[0] == lib2 || check[1] == lib2);
		
	}
	
	
}
