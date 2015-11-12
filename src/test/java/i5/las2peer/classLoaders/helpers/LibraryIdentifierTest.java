package i5.las2peer.classLoaders.helpers;

import static org.junit.Assert.*;
import i5.las2peer.classLoaders.helpers.LibraryIdentifier;

import org.junit.Test;

public class LibraryIdentifierTest {

	@Test
	public void testConstructor() {
		LibraryIdentifier testee = new LibraryIdentifier("testname;version=\"1.0.1-22\"");
		assertEquals("testname", testee.getName());
		assertEquals("1.0.1-22", testee.getVersion().toString());
		assertEquals(22, testee.getVersion().getBuild());

		testee = new LibraryIdentifier("testname-mit-strichen;version=\"1.0\"");
		assertEquals("testname-mit-strichen", testee.getName());
		assertEquals(testee.getVersion(), "1.0");

		testee = new LibraryIdentifier("test.mit.punkten.2;version=\"1.0-10\"");
		assertEquals("test.mit.punkten.2", testee.getName());
		assertEquals("1.0-10", testee.getVersion().toString());
	}

	@Test
	public void testEquality() {
		assertEquals(new LibraryIdentifier("testname;version=\"1.0.1-22\""), "testname;version=\"1.0.1-22\"");
		assertFalse(new LibraryIdentifier("testname;version=\"1.0.1-22\"").equals("tstname;version=\"1.0.1-22\""));
	}

	@Test
	public void testMatchesRange() {
		assertTrue(new LibraryIdentifier("test;version=\"10.2.3-1234\"").matchesRange("10", "11"));
		assertTrue(new LibraryIdentifier("test;version=\"10.2.3-1234\"").matchesRange("10.2.3-1234", "10.2.3-1234"));
		assertTrue(new LibraryIdentifier("test;version=\"10.2.3-1234\"").matchesRange("10", "10.2.3-1234"));

		assertFalse(new LibraryIdentifier("test;version=\"10.2.3-1234\"").matchesRange("10", "10.2.3-1222"));
		assertFalse(new LibraryIdentifier("test;version=\"10.2.3-1234\"").matchesRange("10", "10.2.3"));
		assertFalse(new LibraryIdentifier("test;version=\"10.2.3-1234\"").matchesRange("10", "10.2"));
	}

	@Test
	public void testExceptions() {
		try {
			new LibraryIdentifier("test;vers=1.0");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
		}

		try {
			new LibraryIdentifier("test;vers=\"1.0\"");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
		}

		try {
			new LibraryIdentifier("test;version=1.0");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
		}

		try {
			new LibraryIdentifier("test;version=\"1.0");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
		}

		try {
			new LibraryIdentifier("test;version=1.0\"");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
		}

		try {
			new LibraryIdentifier("test;version=-222\"");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
		}

		try {
			new LibraryIdentifier("test;version=1.0-\"");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
		}

	}
}
