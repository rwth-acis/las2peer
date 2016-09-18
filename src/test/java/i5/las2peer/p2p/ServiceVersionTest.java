package i5.las2peer.p2p;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class ServiceVersionTest {
	ServiceVersion vNull, vStar, v0, v1, v1b, v10, v11, v100, v101, v1000, v1001;

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
		assertFalse(v100.fits(v1000));
		assertTrue(v1.fits(v1b));
		assertTrue(v11.fits(v1));
		assertTrue(vStar.fits(vStar));
	}

	@Test
	public void testCompare() {
		assertEquals(vStar.compareTo(vNull), 0);
		assertEquals(v0.compareTo(v1), -1);
		assertEquals(v1.compareTo(v0), 1);
		assertEquals(v1.compareTo(v10), -1);
		assertEquals(vNull.compareTo(v1), -1);
	}

	@Test
	public void testString() {
		assertEquals(vStar.toString(), "*");
		assertEquals(v1001.toString(), "1.0.0-1");
	}

}
