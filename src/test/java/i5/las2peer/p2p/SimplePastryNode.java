package i5.las2peer.p2p;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import rice.environment.Environment;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.leafset.LeafSet;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;

public class SimplePastryNode {
	/**
	 * This constructor sets up a PastryNode. It will bootstrap to an existing ring if it can find one at the specified
	 * location, otherwise it will start a new ring.
	 * 
	 * @param bindport the local port to bind to
	 * @param bootaddress the IP:port of the node to boot from
	 * @param env the environment for these nodes
	 */
	public SimplePastryNode(int bindport, InetSocketAddress bootaddress, Environment env) throws Exception {

		// Generate the NodeIds Randomly
		NodeIdFactory nidFactory = new RandomNodeIdFactory(env);

		// construct the PastryNodeFactory, this is how we use rice.pastry.socket
		PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory, bindport, env);

		// construct a node
		PastryNode node = factory.newNode();

		TestApp app = new TestApp(node);

		node.boot(bootaddress);

		// the node may require sending several messages to fully boot into the ring
		synchronized (node) {
			while (!node.isReady() && !node.joinFailed()) {
				// delay so we don't busy-wait
				node.wait(500);

				// abort if can't join
				if (node.joinFailed()) {
					throw new IOException("Could not join the FreePastry ring.  Reason:" + node.joinFailedReason());
				}
			}
		}

		System.out.println("Finished creating new node " + node);

		// wait 10 seconds
		env.getTimeSource().sleep(10000);

		// route 10 messages
		for (int i = 0; i < 10; i++) {
			// pick a key at random
			Id randId = nidFactory.generateNodeId();

			// send to that key
			app.routeMyMsg(randId);

			// wait a sec
			env.getTimeSource().sleep(1000);
		}

		// wait 10 seconds
		env.getTimeSource().sleep(10000);

		// send directly to my leafset
		LeafSet leafSet = node.getLeafSet();

		// this is a typical loop to cover your leafset. Note that if the leafset
		// overlaps, then duplicate nodes will be sent to twice
		for (int i = -leafSet.ccwSize(); i <= leafSet.cwSize(); i++) {
			if (i != 0) { // don't send to self
				// select the item
				NodeHandle nh = leafSet.get(i);

				// send the message directly to the node
				app.routeMyMsgDirect(nh);

				// wait a sec
				env.getTimeSource().sleep(1000);
			}
		}
	}

	/**
	 * Usage: java [-cp FreePastry-&lt;version&gt;.jar] rice.tutorial.lesson3.DistTutorial localbindport bootIP bootPort
	 * example java rice.tutorial.DistTutorial 9001 pokey.cs.almamater.edu 9001
	 */
	public static void main(String[] args) throws Exception {
		// Loads pastry settings
		Environment env = new Environment();

		// disable the UPnP setting (in case you are testing this on a NATted LAN)
		env.getParameters().setString("nat_search_policy", "never");

		try {
			// the port to use locally
			int bindport = Integer.parseInt(args[0]);

			// build the bootaddress from the command line args
			InetAddress bootaddr = InetAddress.getByName(args[1]);
			int bootport = Integer.parseInt(args[2]);
			InetSocketAddress bootaddress = new InetSocketAddress(bootaddr, bootport);

			// launch our node!
			// SimplePastryNode dt =
			new SimplePastryNode(bindport, bootaddress, env);
		} catch (Exception e) {
			// remind user how to use
			System.out.println("Usage:");
			System.out.println(
					"java [-cp FreePastry-<version>.jar] rice.tutorial.lesson3.DistTutorial localbindport bootIP bootPort");
			System.out.println("example java rice.tutorial.DistTutorial 9001 pokey.cs.almamater.edu 9001");
			throw e;
		}
	}

}
