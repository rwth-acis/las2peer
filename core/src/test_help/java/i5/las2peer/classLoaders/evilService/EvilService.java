package i5.las2peer.classLoaders.evilService;

import i5.las2peer.api.Context;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.p2p.LocalNodeManager;

public class EvilService {

	public static void notEvil() {
		try {
			Context.get().getMainAgent();
		} catch (IllegalStateException e) {

		}
	}

	public static void accessNode() throws AgentNotFoundException, AgentException {
		new LocalNodeManager().launchNode();
	}

	public static void createThread() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				// do some really bad stuff here
			}
		}).start();
	}

	public static void accessSystemClassLoader() throws ClassNotFoundException {
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		cl.loadClass("i5.las2peer.p2p.Node");
	}

}
