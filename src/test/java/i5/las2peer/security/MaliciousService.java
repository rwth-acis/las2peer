package i5.las2peer.security;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.ServerSocket;

import i5.las2peer.api.Service;

public class MaliciousService extends Service {

	public MaliciousService() {
		// default constructor used by LAS2peer
		super();
	}

	/**
	 * @see SandboxTest#testDisableSecurityManager()
	 */
	public void disableSecurityManager() {
		System.setSecurityManager(null);
	}

	/**
	 * @see SandboxTest#testFilesystem()
	 */
	public void readFile() {
		try {
			BufferedReader bReader = new BufferedReader(new FileReader("/etc/shadow"));
			System.out.println(bReader.readLine());
			bReader.close();
			fail("SecurityException expected");
		} catch (FileNotFoundException e) {
			// not a Unix system ...
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see SandboxTest#testNetwork()
	 */
	public void openBackdoor() {
		try {
			ServerSocket socket = new ServerSocket(0);
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see SandboxTest#testSubthreading()
	 */
	public void subthreads() {
		Thread thread = new Thread() {
			@Override
			public void run() {
				// just try some other test methods inside a new thread
				// this thread is not an instance of L2pThread
				try {
					BufferedReader bReader = new BufferedReader(new FileReader("/etc/shadow"));
					System.out.println(bReader.readLine());
					bReader.close();
					fail("SecurityException expected");
				} catch (FileNotFoundException e) {
					// not a Unix system ...
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see SandboxTest#testOverloading()
	 */
	public void overload() {
		try {
			Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			Field f = L2pSecurityManager.class.getDeclaredField("L2PTHREAD_CLASS");
			modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);
			f.setAccessible(true);
			f.set(null, MaliciousThread.class);
		} catch (SecurityException e) {
			// expected, just forward it to JUnit
			throw e;
		} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	private class MaliciousThread extends Thread {
		@Override
		public void run() {
			System.out.println("This is malware!");
		}
	}

}