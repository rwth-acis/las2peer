package i5.las2peer.tools;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentLockedException;
import i5.las2peer.p2p.AgentAlreadyRegisteredException;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.LocalNodeManager;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.NodeException;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.serialization.MalformedXMLException;

/**
 * Command line tool for starting a {@link i5.las2peer.p2p.LocalNode} and set up some artifacts from a directory
 * containing XML files.
 * 
 * After starting a given static method may be invoked (i.e. for testing purposes)
 * 
 * 
 *
 */
public class LocalNodeStarter {

	/**
	 * start a local node and load all artifact files of the given directory into the node
	 * 
	 * @param node
	 * @param artifactDirectory
	 */
	public static void configureNode(LocalNode node, String artifactDirectory) {
		File dir = new File(artifactDirectory);
		for (File xml : dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".xml");
			}
		})) {
			try {
				String content = FileContentReader.read(xml);
				if (xml.getName().toLowerCase().startsWith("agent")) {
					AgentImpl a = AgentImpl.createFromXml(content);
					node.storeAgent(a);
					System.err.println("loaded agent from " + xml);
				}
			} catch (MalformedXMLException e) {
				System.err.println("unable to deserialize contents of " + xml.toString() + " into an XML envelope!");
			} catch (IOException e) {
				System.err.println("problems reading the contents of " + xml.toString() + ": " + e);
			} catch (AgentLockedException e) {
				System.err.println("error storing agent from " + xml.toString() + ": " + e);
			} catch (AgentAlreadyRegisteredException e) {
				System.err.println("agent from " + xml.toString() + " already known at this node!");
			} catch (AgentException e) {
				System.err.println("other agent problems: " + e);
			}
		}
	}

	/**
	 * invoke a static method which has the local node as possible parameter
	 * 
	 * @param method
	 * @param node
	 */
	public static void invokeStatic(String method, LocalNode node) {
		String sClass = method.substring(0, method.lastIndexOf('.'));
		String sMethod = method.substring(method.lastIndexOf('.') + 1);

		Class<?> cls;
		try {
			cls = Class.forName(sClass);
		} catch (Exception e) {
			System.err.println("Unable to find class " + sClass + ": " + e);
			return;
		}

		Method m = null;
		try {
			m = cls.getMethod(sMethod, LocalNode.class);
			if (!Modifier.isStatic(m.getModifiers())) {
				m = null;
			}
		} catch (Exception e) {
		}

		if (m == null) {
			try {
				m = cls.getMethod(sMethod, Node.class);
				if (!Modifier.isStatic(m.getModifiers())) {
					m = null;
				}
			} catch (Exception e) {
			}
		}

		if (m == null) {
			try {
				m = cls.getMethod(sMethod);
				if (!Modifier.isStatic(m.getModifiers())) {
					m = null;
				}
			} catch (Exception e) {
			}
		}

		if (m == null) {
			System.err.println("Unable to find static method " + sMethod + " of class " + sClass);
			return;
		}

		try {
			if (m.getParameterTypes().length == 1) {
				m.invoke(null, node);
			} else {
				m.invoke(null);
			}
		} catch (Exception e) {
			System.out.println("Error invokin requested method: " + e);
		}
	}

	/**
	 * start a {@link i5.las2peer.p2p.LocalNode} and set up stored artifacts
	 * 
	 * @param argv
	 * @throws InterruptedException
	 * @throws NodeException
	 */
	public static void main(String argv[]) throws InterruptedException, NodeException {
		if (argv.length < 1) {
			System.err.println(
					"Usage: java {-cp ...} i5.las2peer.tools.LocalNodeStarter [xml directory] {static method to invoke}");
			return;
		}

		File directory = new File(argv[0]);
		if (!directory.isDirectory()) {
			System.err.println("The given xml directory does not exist or is not a directory!");
			return;
		}

		LocalNode node = new LocalNodeManager().newNode();

		configureNode(node, argv[0]);

		node.launch();

		Thread.sleep(2500);

		System.err.println("Starting node...");
		if (argv.length == 2) {
			invokeStatic(argv[1], node);
		}
	}

}
