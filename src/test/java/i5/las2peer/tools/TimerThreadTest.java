package i5.las2peer.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TimerThreadTest {

	public class Counter {
		private int i = 0;

		public void inc() {
			synchronized (this) {
				i++;
			}
		}

		public int getCounter() {
			synchronized (this) {
				return i;
			}
		}

	}

	@Test
	public void test() throws InterruptedException {

		final Counter counter = new Counter();

		TimerThread testee = new TimerThread(1000) {

			@Override
			public void tick() {
				counter.inc();
			}

		};

		assertEquals(0, counter.getCounter());
		assertFalse(testee.isRunning());
		assertFalse(testee.isStopped());

		testee.start();

		Thread.sleep(2500);
		assertTrue(testee.isRunning());
		assertFalse(testee.isStopped());

		Thread.sleep(2700);
		testee.stopTimer();
		Thread.sleep(500);

		int check = counter.getCounter();

		assertTrue(5 <= check);
		assertTrue(7 >= check);

		assertFalse(testee.isRunning());
		assertTrue(testee.isStopped());

		Thread.sleep(3000);
		assertEquals(check, counter.getCounter());
	}

}
