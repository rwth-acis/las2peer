package i5.las2peer.classLoaders.libraries;

import static org.junit.Assert.*;

import i5.las2peer.classLoaders.UnresolvedDependenciesException;
import i5.las2peer.classLoaders.helpers.LibraryDependency;
import i5.las2peer.classLoaders.helpers.LibraryIdentifier;
import i5.las2peer.classLoaders.helpers.LibraryVersion;
import i5.las2peer.classLoaders.libraries.FileSystemRepository;
import i5.las2peer.classLoaders.libraries.LoadedLibrary;
import i5.las2peer.classLoaders.libraries.Repository;
import i5.las2peer.classLoaders.libraries.NotFoundException;

import java.io.File;
import java.util.Collection;

import org.junit.Test;

public class FileSystemRepositoryTest {

	@Test
	public void testNonRecursive() {
		FileSystemRepository testee = new FileSystemRepository(
				"test_stubs" + File.separator + "fstest" + File.separator + "dir1");

		Collection<String> foundJars = testee.getLibraryCollection();
		assertEquals(3, foundJars.size());
		assertTrue(foundJars.contains("test_stubs"
				+ File.separator + "fstest"
				+ File.separator + "dir1"
				+ File.separator + "some.test.Jar-2.1.1.jar"));
		assertTrue(foundJars.contains("test_stubs"
				+ File.separator + "fstest"
				+ File.separator + "dir1"
				+ File.separator + "some.test.Jar-1.1.jar"));
		assertTrue(foundJars.contains("test_stubs"
				+ File.separator + "fstest"
				+ File.separator + "dir1"
				+ File.separator + "anotherJar-1.0.jar"));

		Collection<LibraryVersion> foundVersions = testee.getAvailableVersionSet("anotherJar");
		assertEquals(1, foundVersions.size());
		assertTrue(foundVersions.contains("1.0"));

		foundVersions = testee.getAvailableVersionSet("some.test.Jar");
		assertEquals(2, foundVersions.size());
		assertTrue(foundVersions.contains("2.1.1"));
		assertTrue(foundVersions.contains("1.1"));
		assertTrue(true);
	}

	@Test
	public void testRecursive() {
		FileSystemRepository testee = new FileSystemRepository("test_stubs/fstest/dir1", true);

		Collection<String> foundJars = testee.getLibraryCollection();
		assertEquals(4, foundJars.size());
		assertTrue(foundJars.contains("test_stubs"
				+ File.separator + "fstest"
				+ File.separator + "dir1"
				+ File.separator + "some.test.Jar-2.1.1.jar"));
		assertTrue(foundJars.contains("test_stubs"
				+ File.separator + "fstest"
				+ File.separator + "dir1"
				+ File.separator + "some.test.Jar-1.1.jar"));
		assertTrue(foundJars.contains("test_stubs"
				+ File.separator + "fstest"
				+ File.separator + "dir1"
				+ File.separator + "anotherJar-1.0.jar"));
		assertTrue(foundJars.contains("test_stubs"
				+ File.separator + "fstest"
				+ File.separator + "dir1"
				+ File.separator + "subdir"
				+ File.separator + "jarInSubDir-1.0.jar"));

		Collection<LibraryVersion> foundVersions = testee.getAvailableVersionSet("anotherJar");
		assertEquals(1, foundVersions.size());
		assertTrue(foundVersions.contains("1.0"));

		foundVersions = testee.getAvailableVersionSet("some.test.Jar");
		assertEquals(2, foundVersions.size());
		assertTrue(foundVersions.contains("2.1.1"));
		assertTrue(foundVersions.contains("1.1"));

		foundVersions = testee.getAvailableVersionSet("jarInSubDir");
		assertEquals(1, foundVersions.size());
		assertTrue(foundVersions.contains("1.0"));

		assertTrue(true);
	}

	@Test
	public void testMultiple() {
		FileSystemRepository testee = new FileSystemRepository(
				new String[] { "test_stubs/fstest/dir1", "test_stubs/fstest/dir2" });

		Collection<String> foundJars = testee.getLibraryCollection();
		assertEquals(4, foundJars.size());
		assertTrue(foundJars.contains("test_stubs"
				+ File.separator + "fstest"
				+ File.separator + "dir1"
				+ File.separator + "some.test.Jar-2.1.1.jar"));
		assertTrue(foundJars.contains("test_stubs"
				+ File.separator + "fstest"
				+ File.separator + "dir1"
				+ File.separator + "some.test.Jar-1.1.jar"));
		assertTrue(foundJars.contains("test_stubs"
				+ File.separator + "fstest"
				+ File.separator + "dir1"
				+ File.separator + "anotherJar-1.0.jar"));
		assertTrue(foundJars.contains("test_stubs"
				+ File.separator + "fstest"
				+ File.separator + "dir2"
				+ File.separator + "in.second.dir-1.0.jar"));

		Collection<LibraryVersion> foundVersions = testee.getAvailableVersionSet("anotherJar");
		assertEquals(1, foundVersions.size());
		assertTrue(foundVersions.contains("1.0"));

		foundVersions = testee.getAvailableVersionSet("some.test.Jar");
		assertEquals(2, foundVersions.size());
		assertTrue(foundVersions.contains("2.1.1"));
		assertTrue(foundVersions.contains("1.1"));
		assertTrue(true);

		foundVersions = testee.getAvailableVersionSet("in.second.dir");
		assertEquals(1, foundVersions.size());
		assertTrue(foundVersions.contains("1.0"));
	}

	@Test
	public void testLibrarySearch() throws NotFoundException, IllegalArgumentException {
		Repository testee = new FileSystemRepository("export" + File.separator + "jars" + File.separator);

		testee.findLibrary(new LibraryIdentifier("i5.las2peer.classLoaders.testPackage1", "1.0"));

		LoadedLibrary lib = testee
				.findMatchingLibrary(new LibraryDependency("i5.las2peer.classLoaders.testPackage1", "1.1", "2.0"));
		assertEquals("i5.las2peer.classLoaders.testPackage1", lib.getIdentifier().getName());
		assertEquals("1.1", lib.getIdentifier().getVersion().toString());

		try {
			testee.findMatchingLibrary(new LibraryDependency("i5.las2peer.classLoaders.testPackage1", "1.4", "2.0"));
			fail("NotFoundException should have been thrown!");
		} catch (NotFoundException e) {
		}

		try {
			testee.findMatchingLibrary(new LibraryDependency("i5.las2peer.classLoaders.testPackage1", "0.1", "0.4.4"));
			fail("NotFoundException should have been thrown!");
		} catch (NotFoundException e) {
		}

		try {
			testee.findMatchingLibrary(new LibraryDependency("tesadawodauiwd", "1.0", "2.0"));
			fail("NotFoundException should have been thrown!");
		} catch (NotFoundException e) {
		}
	}

	public void testFindingLibrary() throws NotFoundException, UnresolvedDependenciesException {
		Repository testee = new FileSystemRepository("export" + File.separator + "jars" + File.separator);

		LoadedLibrary lib = testee.findLibrary("i5.las2peer.classLoaders.testPackage1");
		assertEquals("1.1", lib.getIdentifier().getVersion().toString());

	}

}
