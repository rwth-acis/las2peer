package i5.las2peer.classLoaders.libraries;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import i5.las2peer.classLoaders.libraries.LibraryIdentifier;

import org.junit.Assert;
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

	@Test
	public void testFromFilename() {
		try {
			LibraryIdentifier testFull = LibraryIdentifier
					.fromFilename("service/i5.las2peer.services.testService-4.2.jar");
			Assert.assertEquals("i5.las2peer.services.testService", testFull.getName());
			Assert.assertEquals("4.2", testFull.getVersion().toString());
			LibraryIdentifier testTripleVersion = LibraryIdentifier
					.fromFilename("i5.las2peer.services.testService-4.2.0.jar");
			Assert.assertEquals("i5.las2peer.services.testService", testTripleVersion.getName());
			Assert.assertEquals("4.2.0", testTripleVersion.getVersion().toString());
			LibraryIdentifier testDoubleVersion = LibraryIdentifier
					.fromFilename("i5.las2peer.services.testService-4.2.jar");
			Assert.assertEquals("i5.las2peer.services.testService", testDoubleVersion.getName());
			Assert.assertEquals("4.2", testDoubleVersion.getVersion().toString());
			LibraryIdentifier testSingleVersion = LibraryIdentifier
					.fromFilename("i5.las2peer.services.testService-4.jar");
			Assert.assertEquals("i5.las2peer.services.testService", testSingleVersion.getName());
			Assert.assertEquals("4", testSingleVersion.getVersion().toString());
			LibraryIdentifier testNoVersion = LibraryIdentifier.fromFilename("i5.las2peer.services.testService.jar");
			Assert.assertEquals("i5.las2peer.services.testService", testNoVersion.getName());
			Assert.assertNull(testNoVersion.getVersion());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
