package i5.las2peer.classLoaders.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class LibraryDependencyTest {

	@Test
	public void testStringConstructor() {
		LibraryDependency testee = new LibraryDependency("test;version=\"1.0.1\"");
		assertEquals("test", testee.getName());
		assertEquals("1.0.1", testee.getMin().toString());
		assertEquals("1.0.1", testee.getMax().toString());
		assertTrue(testee.isMinIncluded());
		assertTrue(testee.isMaxIncluded());
		assertFalse(testee.isOptional());

		testee = new LibraryDependency("test;version=\"1.0.1-123\"");
		assertEquals("test", testee.getName());
		assertEquals("1.0.1-123", testee.getMin().toString());
		assertEquals("1.0.1-123", testee.getMax().toString());
		assertTrue(testee.isMinIncluded());
		assertTrue(testee.isMaxIncluded());
		assertFalse(testee.isOptional());

		testee = new LibraryDependency("test-name; version=\"[1.0,2.2.2]\"");
		assertEquals("test-name", testee.getName());
		assertEquals("1.0", testee.getMin().toString());
		assertEquals("2.2.2", testee.getMax().toString());
		assertTrue(testee.isMinIncluded());
		assertTrue(testee.isMaxIncluded());
		assertFalse(testee.isOptional());

		testee = new LibraryDependency("test-name;version=\"(1.0-123,2.2.2-333)\"");
		assertEquals("test-name", testee.getName());
		assertEquals("1.0-123", testee.getMin().toString());
		assertEquals("2.2.2-333", testee.getMax().toString());
		assertFalse(testee.isMinIncluded());
		assertFalse(testee.isMaxIncluded());
		assertFalse(testee.isOptional());

		testee = new LibraryDependency("test.package.name;version=\"(1.0,2.2.2)\";resolution:=\"optional\"");
		assertEquals("test.package.name", testee.getName());
		assertEquals("1.0", testee.getMin().toString());
		assertEquals("2.2.2", testee.getMax().toString());
		assertFalse(testee.isMinIncluded());
		assertFalse(testee.isMaxIncluded());
		assertTrue(testee.isOptional());
	}

	@Test
	public void testStandardConstructor() {
		LibraryDependency testee = new LibraryDependency("test", "1.0.1", "1.2.1");
		assertEquals("test", testee.getName());
		assertEquals(testee.getMin(), "1.0.1");
		assertEquals(testee.getMax(), "1.2.1");

		assertTrue(testee.fits(new LibraryIdentifier("test;version=\"1.1\"")));
		assertFalse(testee.fits(new LibraryIdentifier("tes;version=\"1.1\"")));
		assertFalse(testee.fits(new LibraryIdentifier("test;version=\"1.3\"")));
	}

	@Test
	public void testToString() {
		assertEquals("test;version=\"1.0.1\"", new LibraryDependency("test; version=\"1.0.1\"").toString());
		assertEquals("test;version=\"1.0.1\"", new LibraryDependency("test", "1.0.1").toString());
		assertEquals("test;version=\"1.0.1\"", new LibraryDependency("test", "1.0.1", "1.0.1").toString());
		assertEquals("test;version=\"1.0.1-123\"", new LibraryDependency("test", "1.0.1-123").toString());

		assertEquals("test;version=\"[1.0.1,1.1.1]\"", new LibraryDependency("test", "1.0.1", "1.1.1").toString());
		assertEquals("test;version=\"(1.0.1,2.0)\"", new LibraryDependency("test;version=\"(1.0.1,2.0)\"").toString());
		assertEquals("test;version=\"(1.0.1-123,2.0-333)\"",
				new LibraryDependency("test;version=\"(1.0.1-123,2.0-333)\"").toString());

		assertEquals("test;version=\"(1.0.1,2.0)\";resolution:=\"optional\"",
				new LibraryDependency("test;version=\"(1.0.1,2.0)\";resolution:=\"optional\"").toString());
	}

	@Test
	public void testStringFactory() {
		LibraryDependency[] testee = LibraryDependency.fromString(
				"testlib;version=\"1.0\" ,  bla;resolution:=\"optional\";version=\"(1.0,2.1)\", xyz;version=\"[1.0,2.2]\"");
		assertEquals(3, testee.length);
		assertEquals("testlib;version=\"1.0\"", testee[0].toString());
		assertEquals("bla;version=\"(1.0,2.1)\";resolution:=\"optional\"", testee[1].toString());
		assertEquals("xyz;version=\"[1.0,2.2]\"", testee[2].toString());

		testee = LibraryDependency.fromString(null);
		assertEquals(0, testee.length);

		testee = LibraryDependency.fromString("   \n ");
		assertEquals(0, testee.length);

		try {
			testee = LibraryDependency.fromString("test;version=\"1.0,1.1 , test2;version=\"1.1,2.0\"");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("enclosed in quotes"));
		}
	}

	@Test
	public void testFits() {
		LibraryDependency testee = new LibraryDependency("testlib;version=\"[1.1,2.0)\"");

		assertTrue(testee.fits(new LibraryVersion("1.1")));
		assertTrue(testee.fits(new LibraryVersion("1.2")));
		assertTrue(testee.fits(new LibraryVersion("1.1.1-299")));
		assertTrue(testee.fits(new LibraryVersion("1.1.1")));

		assertFalse(testee.fits(new LibraryVersion("1.0")));
		assertFalse(testee.fits(new LibraryVersion("2.0")));
		assertFalse(testee.fits(new LibraryVersion("1.0.1")));
		assertFalse(testee.fits(new LibraryVersion("1")));
	}

	@Test
	public void testExceptions() {
		try {
			new LibraryDependency("test; version=1.0.1");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
		}
		try {
			new LibraryDependency("test; version=1.0.1");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
		}
		try {
			new LibraryDependency("test; version=\"(1.0.1)\"");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
		}
		try {
			new LibraryDependency("test");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
		}
		try {
			new LibraryDependency("test; blabla");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
		}
		try {
			new LibraryDependency("test; resolution:=\"optional\"");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
		}
		try {
			new LibraryDependency("test; version=\"(1.0.1,2.0.2,3.0)\"");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
		}
		try {
			new LibraryDependency("test; version=\"(1.0.1-1.0)\"");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
		}
		try {
			new LibraryDependency("test; version=\"(1.0.1-)\"");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
		}
		try {
			new LibraryDependency("test; version=\"(-123)\"");
			fail("IllegalArgumentException should have been thrown");
		} catch (IllegalArgumentException e) {
		}
	}

}
