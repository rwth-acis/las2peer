package i5.las2peer.security;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;

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
			// XXX check some file on Windows systems, too
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
					// XXX check some file on Windows systems, too
					BufferedReader bReader = new BufferedReader(new FileReader("/etc/hostname"));
					bReader.close();
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
	 * @see SandboxTest#testClassPath()
	 */
	public void changeClassPath() {
		System.setProperty("java.class.path", "/etc");
	}

	/**
	 * @see SandboxTest#testPolicyProperty()
	 */
	public void changePolicyProperty() {
		System.setProperty("java.security.policy", "");
	}

	/**
	 * @see SandboxTest#testPolicyFile()
	 */
	public void overwritePolicyFile() {
		try {
			BufferedWriter bWriter = new BufferedWriter(new FileWriter("las2peer.policy"));
			bWriter.write("permission java.security.AllPermission;");
			bWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see SandboxTest#testPolicyProperty()
	 */
	public void blockPorts() {
		ServerSocket sock1 = null;
		ServerSocket sock2 = null;
		try {
			sock1 = new ServerSocket(80);
			sock2 = new ServerSocket(443);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (sock1 != null) {
					sock1.close();
				}
				if (sock2 != null) {
					sock2.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}