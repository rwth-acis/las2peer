package i5.las2peer.classLoaders.libraries;

import static org.junit.Assert.*;
import i5.las2peer.classLoaders.libraries.LoadedLibrary;

import org.junit.Test;

public class LoadedLibraryTest {

	@Test
	public void testResourceToClassName() {
		assertEquals ( "test.bla.Class", LoadedLibrary.resourceToClassName("test/bla/Class.class"));
		
		try {
			LoadedLibrary.resourceToClassName("test.clas");
			fail ( "IllegalArgumentException should have been thrown");
		} catch ( IllegalArgumentException e ) {
			// that's intended
		}
	}
	
	@Test
	public void testClassToResourceName () {
		assertEquals ( "test/bla/Class.class", LoadedLibrary.classToResourceName("test.bla.Class"));
		assertEquals ( "Class.class", LoadedLibrary.classToResourceName("Class"));
	}
	
	@Test
	public void testTransformation () {
		String[] toTest = new String[]{ "test.class", "Test/bla/Klasse.class" };
		for ( int i=0; i<toTest.length; i++ )
			assertEquals ( toTest[i], LoadedLibrary.classToResourceName(LoadedLibrary.resourceToClassName(toTest[i])));
		
		toTest = new String [] { "test.pack.Klasse", "abc.xYz.Super" };
			for ( int i=0; i<toTest.length; i++ )
				assertEquals ( toTest[i], LoadedLibrary.resourceToClassName(LoadedLibrary.classToResourceName(toTest[i])));
	}

}
