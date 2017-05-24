package i5.las2peer.webConnector.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Utility class to lock by name.
 * 
 * <code>
 * NameLock lock = new NameLock();
 * 
 * lock.lock("someString")
 * try {
 * 	// do sth
 * } finally {
 * 	lock.unlock("someString")
 * }
 * </code>
 *
 */
public class NameLock {
	ConcurrentHashMap<String, ReentrantLock> activeLocks;

	public NameLock() {
		activeLocks = new ConcurrentHashMap<>();
	}

	public void lock(String key) {
		ReentrantLock lock = new ReentrantLock();
		ReentrantLock previous = activeLocks.putIfAbsent(key, lock);
		lock = (previous != null) ? previous : lock;
		lock.lock();
	}

	public void unlock(String key) {
		activeLocks.computeIfPresent(key, (k, v) -> {
			ReentrantLock result = v.hasQueuedThreads() ? v : null;
			v.unlock();
			return result;
		});
	}
}
