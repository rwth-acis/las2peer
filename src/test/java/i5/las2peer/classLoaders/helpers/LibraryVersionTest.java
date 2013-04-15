package i5.las2peer.classLoaders.helpers;

import static org.junit.Assert.*;
import i5.las2peer.classLoaders.helpers.LibraryVersion;

import org.junit.Test;

public class LibraryVersionTest {

	@Test
	public void testStringConstructor() {
		LibraryVersion testee = new LibraryVersion ( "10.20.30-4010");
		assertEquals(10,  testee.getMajor());
		assertEquals ( 20, testee.getMinor () );
		assertEquals ( 30, testee.getSub());
		assertEquals( 4010, testee.getBuild () );
		
		testee = new LibraryVersion ( "10.20-4010");
		assertEquals(10,  testee.getMajor());
		assertEquals ( 20, testee.getMinor () );
		assertEquals ( 0, testee.getSub());
		assertEquals( 4010, testee.getBuild () );
		
		testee = new LibraryVersion ( "10.20");
		assertEquals(10,  testee.getMajor());
		assertEquals ( 20, testee.getMinor () );
		assertEquals ( 0, testee.getSub());
		assertEquals( 0, testee.getBuild () );
		
		testee = new LibraryVersion ( "10-4010");
		assertEquals(10,  testee.getMajor());
		assertEquals ( 0, testee.getMinor () );
		assertEquals ( 0, testee.getSub());
		assertEquals( 4010, testee.getBuild () );
		
		testee = new LibraryVersion ( "10");
		assertEquals(10,  testee.getMajor());
		assertEquals ( 0, testee.getMinor () );
		assertEquals ( 0, testee.getSub());
		assertEquals( 0, testee.getBuild () );
	}
	
	@Test
	public void testStringConstructorExceptions () {
		@SuppressWarnings("unused")
		LibraryVersion testee;
		try {
			testee = new LibraryVersion ( "0.-1.10-20");
			fail ( "IllegalArgumentException should have been thrown");
		} catch ( IllegalArgumentException e ) {
			// thats correct
		}
		try {
			testee = new LibraryVersion ( "-");
			fail ( "IllegalArgumentException should have been thrown");
		} catch ( IllegalArgumentException e ) {
			// thats correct
		}
		try {
			testee = new LibraryVersion ( "1-2-3");
			fail ( "IllegalArgumentException should have been thrown");
		} catch ( IllegalArgumentException e ) {
			// thats correct
		}
		try {
			testee = new LibraryVersion ( "a1");
			fail ( "IllegalArgumentException should have been thrown");
		} catch ( IllegalArgumentException e ) {
			// thats correct
		}
		try {
			testee = new LibraryVersion ( "1a");
			fail ( "IllegalArgumentException should have been thrown");
		} catch ( IllegalArgumentException e ) {
			// thats correct
		}
		try {
			testee = new LibraryVersion ( "1.0-10.10");
			fail ( "IllegalArgumentException should have been thrown");
		} catch ( IllegalArgumentException e ) {
			// thats correct
		}
	}
	
	
	@Test
	public void testEquality () {
		//assertEquals ( new LibraryVersion ( "10.0.1-1234"), new LibraryVersion ( 10, 0, 1, 1234) );
		
		LibraryVersion testee1 = new LibraryVersion ( "10.0.1-1234");
		LibraryVersion testee2 = new LibraryVersion(10, 0, 1, 1234);
		assertTrue ( testee1.equals( testee2 ));
		assertEquals ( testee1, testee2 );
		
		assertFalse ( new LibraryVersion ( "10.1.1-1234").equals( new LibraryVersion ( 10,1, 1, 123)));
		assertFalse ( new LibraryVersion ( "10.1.1-1234").equals (new LibraryVersion ( 10,1, 1)));
		
		// check for comparing against string versions
		assertEquals ( new LibraryVersion ( "10.0.1-1234"), "10.0.1-1234");
		
		// this won't work, since String.equals would have to be overwritten
		// for this one
		// assertEquals ( "10.0.1-1234", new LibraryVersion ( "10.0.1-1234"));
	}
	
	@Test
	public void testStringRepresentation () {
		assertEquals ( "10.0.1-1234", new LibraryVersion (10,0,1,1234 ).toString());
		assertEquals ( "10.0-1234", new LibraryVersion ( "10.0-1234").toString() );
		assertEquals ( "10-1234", new LibraryVersion ( "10-1234").toString());
		assertEquals ( "10.0.1", new LibraryVersion ( 10,0,1).toString());
		assertEquals ( "10.1", new LibraryVersion ( 10,1).toString());
		assertEquals ( "10", new LibraryVersion ( 10).toString());
	}
	
	@Test
	public void testSmaller () {
		assertTrue ( new LibraryVersion("10.0.1-1234").isSmallerThan( "10.0.2"));
		assertTrue ( new LibraryVersion("10.0.1-1234").isSmallerThan( "10.0.1-2212"));
		assertTrue ( new LibraryVersion("10.0.1-1234").isSmallerThan( "10.1.0"));
		assertTrue ( new LibraryVersion("10.0.1-1234").isSmallerThan( "10.2"));
		assertTrue ( new LibraryVersion("10.0.1-1234").isSmallerThan( "10.2.0-1222"));
		assertTrue ( new LibraryVersion("10.0.1-1234").isSmallerThan( "11"));
		assertTrue ( new LibraryVersion("10.0.1").isSmallerThan( "10.0.1-1234"));
		assertTrue ( new LibraryVersion("10.1").isSmallerThan( "10.1.1"));
		assertTrue ( new LibraryVersion("10.1").isSmallerThan( "10.2"));
		assertTrue ( new LibraryVersion("10").isSmallerThan( "10.2"));
		assertTrue ( new LibraryVersion("10").isSmallerThan( "11"));

		assertFalse ( new LibraryVersion("10.1.1-1234").isSmallerThan( "10.0.1-2222"));
		assertFalse ( new LibraryVersion("10.1.1-1234").isSmallerThan( "10.1.1"));
		assertFalse ( new LibraryVersion("10.1.1-1234").isSmallerThan( "10.1"));
		assertFalse ( new LibraryVersion("10.1.1-1234").isSmallerThan( "10.1.0-2222"));
		assertFalse ( new LibraryVersion("10.1.1-1234").isSmallerThan( "10.0.2"));
		assertFalse ( new LibraryVersion("10.1.1-1234").isSmallerThan( "9"));
	}

	
	@Test
	public void testLarger () {
		assertFalse ( new LibraryVersion("10.0.1-1234").isLargerThan( "10.0.2"));
		assertFalse ( new LibraryVersion("10.0.1-1234").isLargerThan( "10.0.1-2212"));
		assertFalse ( new LibraryVersion("10.0.1-1234").isLargerThan( "10.1.0"));
		assertFalse ( new LibraryVersion("10.0.1-1234").isLargerThan( "10.2"));
		assertFalse ( new LibraryVersion("10.0.1-1234").isLargerThan( "10.2.0-1222"));
		assertFalse ( new LibraryVersion("10.0.1-1234").isLargerThan( "11"));
		assertFalse ( new LibraryVersion("10.0.1").isLargerThan( "10.0.1-1234"));
		assertFalse ( new LibraryVersion("10.1").isLargerThan( "10.1.1"));
		assertFalse ( new LibraryVersion("10.1").isLargerThan( "10.2"));
		assertFalse ( new LibraryVersion("10").isLargerThan( "10.2"));
		assertFalse ( new LibraryVersion("10").isLargerThan( "11"));

		assertTrue ( new LibraryVersion("10.1.1-1234").isLargerThan( "10.0.1-2222"));
		assertTrue ( new LibraryVersion("10.1.1-1234").isLargerThan( "10.1.1"));
		assertTrue ( new LibraryVersion("10.1.1-1234").isLargerThan( "10.1"));
		assertTrue ( new LibraryVersion("10.1.1-1234").isLargerThan( "10.1.0-2222"));
		assertTrue ( new LibraryVersion("10.1.1-1234").isLargerThan( "10.0.2"));
		assertTrue ( new LibraryVersion("10.1.1-1234").isLargerThan( "9"));
	}
	
	@Test
	public void testIsBetween () {
		assertTrue ( new LibraryVersion ( "10.2.3-1234").isBetween("10", "11"));
		assertTrue ( new LibraryVersion ( "10.2.3-1234").isBetween("10.2.3-1234", "10.2.3-1234"));
		assertTrue ( new LibraryVersion ( "10.2.3-1234").isBetween("10", "10.2.3-1234"));

		assertFalse ( new LibraryVersion ( "10.2.3-1234").isBetween("10", "10.2.3-1222"));
		assertFalse ( new LibraryVersion ( "10.2.3-1234").isBetween("10", "10.2.3"));
		assertFalse ( new LibraryVersion ( "10.2.3-1234").isBetween("10", "10.2"));
	}
	
	@Test 
	public void testSimpleVersion () {
		assertTrue ( new LibraryDependency("test;version=\"9\"").fits( new LibraryVersion ( "9")));
	}
	
	
}
