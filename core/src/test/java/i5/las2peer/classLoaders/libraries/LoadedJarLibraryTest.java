package i5.las2peer.classLoaders.libraries;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import i5.las2peer.classLoaders.LibraryNotFoundException;

public class LoadedJarLibraryTest {

	@Test
	public void testCreation() throws IOException {
		LoadedJarLibrary testee = LoadedJarLibrary
				.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar");
		assertEquals("i5.las2peer.classLoaders.testPackage1;version=\"1.1\"", testee.getIdentifier().toString());
		assertEquals("export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar", testee.getJarFileName());

		testee = LoadedJarLibrary.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage2-1.0.jar");
		assertEquals("i5.las2peer.classLoaders.testPackage2;version=\"1.0\"", testee.getIdentifier().toString());
		assertEquals("export/jars/i5.las2peer.classLoaders.testPackage2-1.0.jar", testee.getJarFileName());

		try {
			testee = LoadedJarLibrary.createFromJar("export/jars/test2-1.4.jar");
			fail("IOException should have been thrown!");
		} catch (IOException e) {
		}
	}

	@Test
	public void testStringGetter() throws IOException, LibraryNotFoundException, ResourceNotFoundException {
		LoadedJarLibrary testee = LoadedJarLibrary
				.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar");
		String test = testee.getResourceAsString("i5/las2peer/classLoaders/testPackage1/test.properties");
		test = test.replace("\n", "").replace("\r", ""); // Workaround to prevent different line ending (Win/Unix OS)
															// comparison probelms
		assertEquals("attribute=otherValueinteger=987", test);
	}

	@Test
	public void testFiles() throws IOException, LibraryNotFoundException, ResourceNotFoundException {
		LoadedJarLibrary testee = LoadedJarLibrary
				.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar");

		URL test = testee.getResourceAsUrl("i5/las2peer/classLoaders/testPackage1/CounterClass.class");
		assertNotNull(test);

		test = testee.getResourceAsUrl("i5/las2peer/classLoaders/testPackage1/test.properties");
		assertNotNull(test);
	}

	@Test
	public void testBinaryContent() throws IOException, LibraryNotFoundException, ResourceNotFoundException {
		LoadedJarLibrary testee = LoadedJarLibrary
				.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage1-1.1.jar");
		byte[] result = testee.getResourceAsBinary("i5/las2peer/classLoaders/testPackage1/test.properties");
		assertNotNull(result);
		assertTrue(result.length > 0);

		result = testee.getResourceAsBinary("i5/las2peer/classLoaders/testPackage1/CounterClass.class");
		assertNotNull(result);
		assertTrue(result.length > 0);
	}

}
