package i5.las2peer.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.Vector;

/**
 * A simple command line generator for setup directories for the {@link L2pNodeLauncher}.
 * 
 * The last node is always interactive.
 * 
 * 
 *
 */
public class LaunchDirGeneratorSize {

	public static void writeNodeFile(String name, String bootstrap, int port, String[] methods) throws IOException {
		FileOutputStream fos = new FileOutputStream(name);
		PrintStream print = new PrintStream(fos);

		print.println(port);
		print.println(bootstrap);
		print.println(SimpleTools.join(methods, "\n"));

		fos.close();

		System.out.println("Written node " + name);
	}

	private static Random r = new Random();

	public static String[] createCommands(int size, int cnt, int randRange) {
		Vector<String> result = new Vector<String>();

		int rand = r.nextInt(randRange);

		for (int i = 0; i < rand; i++)
			result.add("waitALittle");
		result.add("storeRandoms (\"" + cnt + "\", \"" + size + "\")");
		result.add("waitALittle");

		return result.toArray(new String[0]);
	}

	public static void writeDir(int nodes, int contentSize, int contentPerNode) throws IOException {
		String dirName = "" + nodes + "/" + contentSize + "/" + contentPerNode;

		if (!new File("" + nodes).isDirectory())
			new File("" + nodes).mkdir();
		if (!new File("" + nodes + "/" + contentSize).isDirectory())
			new File("" + nodes + "/" + contentSize).mkdir();
		if (!new File("" + nodes + "/" + contentSize + "/" + contentPerNode).isDirectory())
			new File("" + nodes + "/" + contentSize + "/" + contentPerNode).mkdir();

		int randRange = 5 + (nodes * contentPerNode / 200 / 50) * 20;

		String bootstrapPrefix = "elika.local";
		int startPort = 9000;

		File dir = new File(dirName);
		if (!dir.exists())
			dir.mkdir();

		DecimalFormat df = new DecimalFormat("0000");

		String[] methods = createCommands(contentSize, contentPerNode, randRange);
		writeNodeFile("" + dirName + "/node-0000.node", "NEW", startPort, methods);

		for (int i = 1; i < nodes - 1; i++) {
			methods = createCommands(contentSize, contentPerNode, randRange);
			writeNodeFile("" + dirName + "/node-" + df.format(i) + ".node", bootstrapPrefix + ":" + startPort,
					startPort + i, methods);
		}

		methods = createCommands(contentSize, contentPerNode, randRange);
		String[] finalMethods = new String[methods.length + 3];
		System.arraycopy(methods, 0, finalMethods, 0, methods.length);
		finalMethods[methods.length] = "waitFinished ( \"" + (nodes - 1) + "\")";
		finalMethods[methods.length + 1] = "waitEnter (\"size: " + contentSize + "  per node: " + contentPerNode
				+ "  nodes: " + nodes + "\")";
		finalMethods[methods.length + 2] = "killAll";

		writeNodeFile("" + dir + "/node-" + df.format(nodes - 1) + ".node", bootstrapPrefix + ":" + startPort,
				startPort + nodes, finalMethods);
	}

	public static void main(String[] argv) throws IOException {
		int[][] combinations = new int[][] {
				new int[] { 20, 10, 0500 },
				new int[] { 20, 10, 1000 },
				new int[] { 20, 10, 1500 },
				new int[] { 20, 10, 2000 },
				new int[] { 20, 10, 2500 },
				new int[] { 20, 10, 3000 },
				new int[] { 20, 10, 3500 },
				new int[] { 20, 10, 4000 },
				new int[] { 20, 10, 4500 },
				new int[] { 20, 10, 5000 },

				new int[] { 40, 10, 2000 },
				new int[] { 40, 20, 2000 },
				new int[] { 40, 30, 2000 },
				new int[] { 40, 40, 2000 },
				new int[] { 40, 50, 2000 },
				new int[] { 40, 60, 2000 },
				new int[] { 40, 70, 2000 },
				new int[] { 40, 80, 2000 },
				new int[] { 40, 90, 2000 },
				new int[] { 40, 100, 2000 },

				new int[] { 10, 20, 1000 },
				new int[] { 20, 20, 1000 },
				new int[] { 30, 20, 1000 },
				new int[] { 40, 20, 1000 },
				new int[] { 50, 20, 1000 },
				new int[] { 60, 20, 1000 },
				new int[] { 70, 20, 1000 },
				new int[] { 80, 20, 1000 },
				new int[] { 90, 20, 1000 },
				new int[] { 100, 20, 1000 },
				new int[] { 110, 20, 1000 },
				new int[] { 120, 20, 1000 },
				new int[] { 130, 20, 1000 },
				new int[] { 140, 20, 1000 },
				new int[] { 150, 20, 1000 },

		};

		for (int[] combin : combinations)
			writeDir(combin[0], combin[2], combin[1]);
	}

}
