package i5.las2peer.classLoaders;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Properties;

import org.junit.Test;

public class LibraryClassLoaderTest {

	@Test
	public void test() throws IllegalArgumentException, IOException {
		LibraryClassLoader testee = new LibraryClassLoader ( "../export/jars/i5.las2peer.classLoaders.testPackage2-1.0.jar" );
		
		assertEquals ( "i5.las2peer.classLoaders.testPackage2", testee.getLibrary().getIdentifier().getName());
		assertEquals ( "1.0", testee.getLibrary().getIdentifier().getVersion().toString());
	}
	

	@Test
	public void testClassLoading () throws IllegalArgumentException, IOException, ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		LibraryClassLoader testee = new LibraryClassLoader ( "../export/jars/i5.las2peer.classLoaders.testPackage1-1.0.jar");
		
		Class<?> cl = testee.loadClass( "i5.las2peer.classLoaders.testPackage1.CounterClass");
		
		Method inc = cl.getDeclaredMethod("inc");
		Method counter = cl.getDeclaredMethod("getCounter");
		
		inc.invoke(null);
		Object res = counter.invoke ( null );
		
		assertEquals ( 1, ((Integer) res).intValue());
		
		try {
			testee.loadClass( "some.not.existing.class");
			fail ( "ClassNotFoundException should have been thrown");
		} catch ( ClassNotFoundException e ) {}
	}
	
	@Test
	public void testResources () throws IllegalArgumentException, IOException {
		LibraryClassLoader testee = new LibraryClassLoader ( "../export/jars/i5.las2peer.classLoaders.testPackage1-1.0.jar");
				
		Properties properties = new Properties();
		properties.load( testee.getResourceAsStream("i5/las2peer/classLoaders/testPackage1/test.properties"));
	
		assertEquals ( "123", properties.getProperty("integer"));
		assertEquals ( "value", properties.getProperty("attribute"));
		

		URL test = testee.getResource( "does/not/exist");
		assertNull ( test );
		
	}
	
	@Test
	public void testLoaderBehaviour () throws ClassNotFoundException, IllegalArgumentException, IOException {
		LibraryClassLoader testee1 = new LibraryClassLoader ( "../export/jars/i5.las2peer.classLoaders.testPackage1-1.0.jar");
		LibraryClassLoader testee2 = new LibraryClassLoader ( "../export/jars/i5.las2peer.classLoaders.testPackage1-1.0.jar");
		
		Class<?> test1 = testee1.loadClass("i5.las2peer.classLoaders.testPackage1.CounterClass");
		Class<?> test2 = testee2.loadClass("i5.las2peer.classLoaders.testPackage1.CounterClass");
		
		assertNotSame ( test1, test2 );
		
		test2 = testee1.loadClass("i5.las2peer.classLoaders.testPackage1.CounterClass");
		assertEquals ( test1, test2 );
	}

}
