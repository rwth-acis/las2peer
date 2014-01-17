package i5.las2peer.classLoaders.libraries;

import static org.junit.Assert.*;
import i5.las2peer.classLoaders.libraries.NotFoundException;
import i5.las2peer.classLoaders.libraries.LoadedJarLibrary;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

public class LoadedJarLibraryTest {

	@Test
	public void testCreation() throws IOException {
		LoadedJarLibrary testee = LoadedJarLibrary.createFromJar( "export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar" );
		assertEquals ( "i5.las2peer.classLoaders.testPackage1;version=\"1.1\"", testee.getIdentifier().toString() );
		assertEquals ( "export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar", testee.getJarFileName());
		assertEquals ( 0, testee.getDependencies().length );

		testee = LoadedJarLibrary.createFromJar( "export/jars/i5.las2peer.classLoaders.testPackage2-1.0.jar" );
		assertEquals ( "i5.las2peer.classLoaders.testPackage2;version=\"1.0\"", testee.getIdentifier().toString() );
		assertEquals ( "export/jars/i5.las2peer.classLoaders.testPackage2-1.0.jar", testee.getJarFileName());
		assertEquals ( 1, testee.getDependencies().length );
		assertEquals ( "i5.las2peer.classLoaders.testPackage1;version=\"[1.1,2.0)\"", testee.getDependencies()[0].toString());

		try {
			testee = LoadedJarLibrary.createFromJar( "export/jars/test2-1.4.jar");
			fail ( "IOException should have been thrown!");
		} catch ( IOException e ) {
		}
	}

	@Test
	public void testStringGetter () throws IOException, NotFoundException, ResourceNotFoundException {
		LoadedJarLibrary testee = LoadedJarLibrary.createFromJar( "export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar" );
		String test = testee.getResourceAsString ("i5/las2peer/classLoaders/testPackage1/test.properties");
		assertEquals ( "attribute=otherValue"+System.lineSeparator()+"integer=987", test);
	}

	@Test
	public void testFiles () throws IOException, NotFoundException, ResourceNotFoundException {
		LoadedJarLibrary testee = LoadedJarLibrary.createFromJar( "export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar" );

		URL test = testee.getResourceAsUrl("i5/las2peer/classLoaders/testPackage1/CounterClass.class");
		assertNotNull ( test );

		test = testee.getResourceAsUrl( "i5/las2peer/classLoaders/testPackage1/test.properties" );
		assertNotNull ( test );
	}

	@Test
	public void testBinaryContent () throws IOException, NotFoundException, ResourceNotFoundException {
		LoadedJarLibrary testee = LoadedJarLibrary.createFromJar( "export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar" );
		byte[] result = testee.getResourceAsBinary("i5/las2peer/classLoaders/testPackage1/test.properties");
		assertNotNull ( result );
		assertTrue ( result.length > 0 );

		result = testee.getResourceAsBinary("i5/las2peer/classLoaders/testPackage1/CounterClass.class");
		assertNotNull ( result );
		assertTrue ( result.length > 0 );
	}

}
