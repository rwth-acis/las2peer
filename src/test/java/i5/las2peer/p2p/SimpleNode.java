package i5.las2peer.p2p;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Vector;

import rice.environment.Environment;
import rice.p2p.commonapi.NodeHandle;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.leafset.LeafSet;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;

public class SimpleNode {

	/*******************************************************************************
	 * "FreePastry" Peer-to-Peer Application Development Substrate
	 * 
	 * Copyright 2002-2007, Rice University. Copyright 2006-2007, Max Planck Institute for Software Systems. All rights
	 * reserved.
	 * 
	 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
	 * following conditions are met:
	 * 
	 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the
	 * following disclaimer.
	 * 
	 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
	 * following disclaimer in the documentation and/or other materials provided with the distribution.
	 * 
	 * - Neither the name of Rice University (RICE), Max Planck Institute for Software Systems (MPI-SWS) nor the names
	 * of its contributors may be used to endorse or promote products derived from this software without specific prior
	 * written permission.
	 * 
	 * This software is provided by RICE, MPI-SWS and the contributors on an "as is" basis, without any representations
	 * or warranties of any kind, express or implied including, but not limited to, representations or warranties of
	 * non-infringement, merchantability or fitness for a particular purpose. In no event shall RICE, MPI-SWS or
	 * contributors be liable for any direct, indirect, incidental, special, exemplary, or consequential damages
	 * (including, but not limited to, procurement of substitute goods or services; loss of use, data, or profits; or
	 * business interruption) however caused and on any theory of liability, whether in contract, strict liability, or
	 * tort (including negligence or otherwise) arising in any way out of the use of this software, even if advised of
	 * the possibility of such damage.
	 *******************************************************************************/

	private PastryNode node;

	private TestApp app;

	private Environment env;

	/**
	 * This constructor sets up a PastryNode. It will bootstrap to an existing ring if it can find one at the specified
	 * location, otherwise it will start a new ring.
	 * 
	 * @param bindport the local port to bind to
	 * @param bootaddresses collection of socket addresses for bootstrap
	 * @param env the environment for these nodes
	 */
	public SimpleNode(int bindport, Collection<InetSocketAddress> bootaddresses,
			Environment env) throws Exception {

		this.env = env;

		// Generate the NodeIds Randomly
		NodeIdFactory nidFactory = new RandomNodeIdFactory(env);

		// construct the PastryNodeFactory, this is how we use
		// rice.pastry.socket
		PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory,
				bindport, env);

		// construct a node, but this does not cause it to boot
		node = factory.newNode();

		app = new TestApp(node);

		node.boot(bootaddresses);

		// the node may require sending several messages to fully boot into the
		// ring
		synchronized (node) {
			while (!node.isReady() && !node.joinFailed()) {
				// delay so we don't busy-wait
				node.wait(500);

				// abort if can't join
				if (node.joinFailed()) {
					throw new IOException(
							"Could not join the FreePastry ring.  Reason:"
									+ node.joinFailedReason());
				}
			}
		}

		System.out.println("Finished creating new node " + node);
	}

	public PastryNode getNode() {
		return node;
	}

	private static void failWithMessage(String message) {
		System.out.println("Usage-Message...");

		System.out.println("\nMessage: " + message);

		System.exit(1);
	}

	private void sendTestMessages() throws InterruptedException {
		while (true) {
			env.getTimeSource().sleep(10000);
			System.out.println("---------------------------------------");
			System.out.println("Sending new Messages");

			LeafSet leafSet = node.getLeafSet();
			System.out.println("LeafSet-Size: " + leafSet.cwSize());

			// this is a typical loop to cover your leafset. Note that if the leafset
			// overlaps, then duplicate nodes will be sent to twice
			for (int i = -leafSet.ccwSize(); i <= leafSet.cwSize(); i++) {
				if (i != 0) { // don't send to self
					// select the item
					NodeHandle nh = leafSet.get(i);

					// send the message directly to the node
					System.out.println("sending to " + i + " / " + nh);
					app.routeMyMsgDirect(nh);

					// wait a sec
					env.getTimeSource().sleep(1000);
				}
			}

			System.out.println("---------------------------------------------");
		}

	}

	/**
	 * Usage: java [-cp FreePastry-&lt;version&gt;.jar] rice.tutorial.lesson1.DistTutorial localbindport bootIP bootPort
	 * example java rice.tutorial.DistTutorial 9001 pokey.cs.almamater.edu 9001
	 */
	public static void main(String[] args) throws Exception {
		// Loads pastry settings
		Environment env = new Environment();

		// disable the UPnP setting (in case you are testing this on a NATted
		// LAN)
		env.getParameters().setString("nat_search_policy", "never");
		env.getParameters().setString("firewall_test_policy", "never");
		env.getParameters().setString("nat_network_prefixes", "");

		try {
			// the port to use locally
			int bindport = -1;
			Collection<InetSocketAddress> bootaddresses = new Vector<InetSocketAddress>();

			boolean sendTestMessages = false;

			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-p")) {
					if (bindport != -1)
						failWithMessage("parameter -p given more than once!");
					i++;
					bindport = Integer.parseInt(args[i]);
				} else if (args[i].equals("-b")) {
					i++;

					String[] split = args[i].split(":");
					if (split.length != 2)
						failWithMessage("Unkown bootstrap format for: "
								+ args[i]);

					InetAddress bootaddr = InetAddress.getByName(split[0]);
					int bootport = Integer.parseInt(split[1]);
					bootaddresses
							.add(new InetSocketAddress(bootaddr, bootport));
				} else if (args[i].equals("-t")) {
					sendTestMessages = true;
				} else
					failWithMessage("Unkown parameter: " + args[i]);
			}

			if (bootaddresses.size() == 0)
				System.out
						.println("No Bootstrap node given - Starting new Ring");

			// launch our node!
			SimpleNode nodeTest = new SimpleNode(bindport, bootaddresses, env);

			if (sendTestMessages) {
				nodeTest.sendTestMessages();
			} else
				System.out.println("nothing to do...");

		} catch (Exception e) {
			failWithMessage("Exception: " + e);
		}
	}

}
