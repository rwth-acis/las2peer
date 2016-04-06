package i5.las2peer.security;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import i5.las2peer.api.Service;

public class MaliciousService extends Service {

	public MaliciousService() {
		// default constructor used by las2peer
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
	 * @see SandboxTest#testSubthreading()
	 */
	public void subthreads() {
		Thread thread = new Thread() {
			@Override
			public void run() {
				// just try some other test methods inside a new thread
				// this thread is not an instance of L2pThread
				try {
					BufferedReader bReader = new BufferedReader(new FileReader("/etc/hostname"));
					bReader.close();
					// XXX check a file on Windows systems, too?
					fail("SecurityException expected");
				} catch (SecurityException e) {
					// expected
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
	 * @see SandboxTest#testReflection()
	 */
	public void reflection() {
		try {
			Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			Field f = L2pSecurityManager.class.getDeclaredField("REFLECTION_TEST_VAR");
			modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);
			f.setAccessible(true);
			f.set(null, "changed");
			System.out.println(L2pSecurityManager.REFLECTION_TEST_VAR);
			if (L2pSecurityManager.REFLECTION_TEST_VAR.equals("changed")) {
				fail("This should not happen!");
			}
		} catch (SecurityException e) {
			// expected, just forward it to JUnit
			throw e;
		} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

}