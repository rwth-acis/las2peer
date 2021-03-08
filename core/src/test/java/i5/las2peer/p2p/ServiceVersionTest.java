package i5.las2peer.p2p;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import i5.las2peer.api.p2p.ServiceVersion;

public class ServiceVersionTest {
	ServiceVersion vNull, vStar, v0, v1, v1b, v10, v11, v100, v101, v1000, v1001, v100_alpha, v100_rc1, v100_pr_build, v100_build;

	@Before
	public void construct() {
		vNull = new ServiceVersion(null);
		vStar = new ServiceVersion("*");
		v0 = new ServiceVersion("0");
		v1 = new ServiceVersion("1");
		v1b = new ServiceVersion("1");
		v10 = new ServiceVersion("1.0");
		v11 = new ServiceVersion("1.1");
		v100 = new ServiceVersion("1.0.0");
		v101 = new ServiceVersion("1.0.1");
		v1000 = new ServiceVersion("1.0.0-0");
		v1001 = new ServiceVersion("1.0.0-1");

		v100_alpha = new ServiceVersion("1.0.0-alpha");
		v100_rc1 = new ServiceVersion("1.0.0-rc.1");
		v100_pr_build = new ServiceVersion("1.0.0-rc.1+sha.0abcd");
		v100_build = new ServiceVersion("1.0.0+sha.0abcd");
	}

	@Test
	public void testEqual() {
		assertEquals(vNull, vStar);
		assertEquals(v1, v1b);
		assertNotEquals(vStar, v0);
		assertNotEquals(v0, v1);
		assertNotEquals(v1, v10);
	}

	@Test
	public void testFits() {
		assertTrue(v0.fits(vStar));
		assertFalse(vStar.fits(v0));
		assertTrue(v1000.fits(v100));
		assertTrue(v100.fits(v1000));
		assertTrue(v1.fits(v1b));
		assertTrue(v11.fits(v1));
		assertTrue(vStar.fits(vStar));
		assertTrue(v100.fits(v100_alpha));
		assertTrue(v100.fits(v100_alpha));
		assertTrue(v100.fits(v100_build));
	}

	@Test
	public void testCompare() {
		assertEquals(0, vStar.compareTo(vNull));
		assertEquals(-1, v0.compareTo(v1));
		assertEquals(1, v1.compareTo(v0));
		assertEquals(-1, v1.compareTo(v10));
		assertEquals(-1, vNull.compareTo(v1));

		// Release takes precedence over pre-release
		assertEquals(1, v100.compareTo(v100_rc1));

		// Higher pre-release takes precedence
		assertEquals(-1, v1000.compareTo(v1001));
		assertEquals(-1, v100_alpha.compareTo(v100_rc1));

		// Build must be ignored in precedence
		assertEquals(0, v100_rc1.compareTo(v100_pr_build));

	}

	@Test
	public void testString() {
		assertEquals("*", vStar.toString());
		assertEquals("1.0.0-1", v1001.toString());
		assertEquals("1.0.0-rc.1+sha.0abcd", v100_pr_build.toString());
	}

}
