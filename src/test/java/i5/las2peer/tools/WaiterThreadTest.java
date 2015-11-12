package i5.las2peer.tools;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;

public class WaiterThreadTest {

	@Test
	public void testTimeout() throws InterruptedException {

		WaiterThread<?> testee = new WaiterThread<Object>(5000);
		testee.start();

		Thread.sleep(6000);

		assertTrue(testee.isFinished());
		assertFalse(testee.isSuccess());
		assertTrue(testee.isTimedOut());
	}

	@Test
	public void testCollecting() throws InterruptedException {

		WaiterThread<String> testee = new WaiterThread<String>();
		testee.start();

		Thread.sleep(2000);

		long timestamp1 = new Date().getTime();
		testee.collectResult("a test");

		long timestamp2 = new Date().getTime();

		testee.join();
		long timestamp3 = new Date().getTime();

		System.out.println(timestamp1);
		System.out.println(timestamp2);
		System.out.println(timestamp3);

		assertTrue(2000 > timestamp2 - timestamp1);
		assertTrue(2000 > timestamp3 - timestamp2);

		assertTrue(testee.isSuccess());
		assertFalse(testee.isTimedOut());
		assertTrue(testee.isFinished());
		assertFalse(testee.hasException());
		assertTrue(testee.hasResult());

		assertEquals("a test", testee.getResult());

	}

	@Test
	public void testException() throws InterruptedException {

		WaiterThread<String> testee = new WaiterThread<String>();
		testee.start();

		Thread.sleep(2000);

		testee.collectException(new Exception("a test"));

		long timestamp = new Date().getTime();

		testee.join();

		assertTrue(2000 > (new Date().getTime() - timestamp));

		assertFalse(testee.isSuccess());
		assertFalse(testee.isTimedOut());
		assertTrue(testee.isFinished());
		assertTrue(testee.hasException());
		assertFalse(testee.hasResult());

		assertEquals("a test", testee.getException().getMessage());

	}

}
