package i5.las2peer.classLoaders;

import static org.junit.Assert.*;
import i5.las2peer.classLoaders.libraries.LoadedLibrary;
import i5.las2peer.classLoaders.libraries.ResourceNotFoundException;

import java.io.IOException;

import org.junit.Test;

public class LoadedLibraryCacheTest {

	@Test
	public void testRegistering() {
		BundleClassManager bundle1 = new BundleClassManager(null);
		BundleClassManager bundle2 = new BundleClassManager(null);

		LoadedLibraryCache cache = new LoadedLibraryCache(null);
		assertFalse(cache.isUsed());

		cache.registerBundle(bundle1);
		assertTrue(cache.isUsed());
		cache.registerBundle(bundle2);
		assertTrue(cache.isUsed());

		cache.unregisterBundle(bundle1);
		assertTrue(cache.isUsed());
		cache.unregisterBundle(bundle1);
		assertTrue(cache.isUsed());
		cache.unregisterBundle(bundle2);
		assertFalse(cache.isUsed());
	}

	@Test
	public void testCaching() throws ResourceNotFoundException, IOException {
		LoadedLibraryCache cache = LoadedLibraryCache
				.createFromJar("export/jars/i5.las2peer.classLoaders.testPackage1-1.0.jar");

		byte[] res1 = cache.getCachedResourceAsBinary(
				LoadedLibrary.classToResourceName("i5.las2peer.classLoaders.testPackage1.CounterClass"));
		byte[] res2 = cache.getCachedResourceAsBinary(
				LoadedLibrary.classToResourceName("i5.las2peer.classLoaders.testPackage1.CounterClass"));

		assertEquals(res1, res2);
	}

}
