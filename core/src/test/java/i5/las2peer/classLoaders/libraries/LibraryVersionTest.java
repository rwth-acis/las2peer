package i5.las2peer.classLoaders.libraries;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import i5.las2peer.classLoaders.libraries.LibraryVersion;

import org.junit.Test;

public class LibraryVersionTest {

	@Test
	public void testStringConstructor() {
		LibraryVersion testee = new LibraryVersion("10.20.30-4010");
		assertEquals(10, testee.getMajor());
		assertEquals(20, testee.getMinor());
		assertEquals(30, testee.getSub());
		assertEquals(4010, testee.getBuild());

		testee = new LibraryVersion("10.20-4010");
		assertEquals(10, testee.getMajor());
		assertEquals(20, testee.getMinor());
		assertEquals(0, testee.getSub());
		assertEquals(4010, testee.getBuild());

		testee = new LibraryVersion("10.20");
		assertEquals(10, testee.getMajor());
		assertEquals(20, testee.getMinor());
		assertEquals(0, testee.getSub());
		assertEquals(0, testee.getBuild());

		testee = new LibraryVersion("10-4010");
		assertEquals(10, testee.getMajor());
		assertEquals(0, testee.getMinor());
		assertEquals(0, testee.getSub());
		assertEquals(4010, testee.getBuild());

		testee = new LibraryVersion("10");
		assertEquals(10, testee.getMajor());
		assertEquals(0, testee.getMinor());
		assertEquals(0, testee.getSub());
		assertEquals(0, testee.getBuild());
	}

	@Test
	public void testStringConstructorExceptions() {
		@SuppressWarnings("unused")
		LibraryVersion testee;
		try {
			testee = new LibraryVersion("0.-1.10-20");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
			// thats correct
		}
		try {
			testee = new LibraryVersion("-");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
			// thats correct
		}
		try {
			testee = new LibraryVersion("1-2-3");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
			// thats correct
		}
		try {
			testee = new LibraryVersion("a1");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
			// thats correct
		}
		try {
			testee = new LibraryVersion("1a");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
			// thats correct
		}
		try {
			testee = new LibraryVersion("1.0-10.10");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
			// thats correct
		}
	}

	@Test
	public void testEquality() {
		// assertEquals ( new LibraryVersion ( "10.0.1-1234"), new LibraryVersion ( 10, 0, 1, 1234) );

		LibraryVersion testee1 = new LibraryVersion("10.0.1-1234");
		LibraryVersion testee2 = new LibraryVersion(10, 0, 1, 1234);
		assertTrue(testee1.equals(testee2));
		assertEquals(testee1, testee2);

		assertFalse(new LibraryVersion("10.1.1-1234").equals(new LibraryVersion(10, 1, 1, 123)));
		assertFalse(new LibraryVersion("10.1.1-1234").equals(new LibraryVersion(10, 1, 1)));

		// check for comparing against string versions
		assertEquals(new LibraryVersion("10.0.1-1234"), "10.0.1-1234");

		// this won't work, since String.equals would have to be overwritten
		// for this one
		// assertEquals ( "10.0.1-1234", new LibraryVersion ( "10.0.1-1234"));
	}

	@Test
	public void testStringRepresentation() {
		assertEquals("10.0.1-1234", new LibraryVersion(10, 0, 1, 1234).toString());
		assertEquals("10.0-1234", new LibraryVersion("10.0-1234").toString());
		assertEquals("10-1234", new LibraryVersion("10-1234").toString());
		assertEquals("10.0.1", new LibraryVersion(10, 0, 1).toString());
		assertEquals("10.1", new LibraryVersion(10, 1).toString());
		assertEquals("10", new LibraryVersion(10).toString());
	}

}
