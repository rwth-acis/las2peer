package i5.las2peer.p2p;

import i5.las2peer.security.UserAgent;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import org.junit.Test;

public class PastryNodeImplTest {
	public final int NODE_COUNT = 5;
	public final int AGENTS_PER_NODE_COUNT = 5;
	public final int PORT = 30200;
	PastryNodeImpl[] node = new PastryNodeImpl[NODE_COUNT];
	UserAgent[] agents = new UserAgent[NODE_COUNT * AGENTS_PER_NODE_COUNT];

	/**
	 * gets a non-loopback address to connect to this pc
	 * 
	 * more a hack than clean code, i couldn't find another way to do this...
	 * 
	 * @return
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	private static String getIp() throws SocketException, UnknownHostException {
		Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
		for (; n.hasMoreElements();) {
			NetworkInterface e = n.nextElement();

			Enumeration<InetAddress> a = e.getInetAddresses();
			while (a.hasMoreElements()) {
				InetAddress addr = a.nextElement();
				String ip = addr.getHostAddress();

				if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress() && !addr.isAnyLocalAddress())
					return ip;
			}
		}

		return Inet4Address.getLocalHost().getHostAddress();
	}

	@Test
	public void doNothing() {

	}

	// disabled, because it does not work in Jenkins...

	/*
		@Before
		public void setUp() throws NodeException, CryptoException, L2pSecurityException, AgentException,
				UnknownHostException, SocketException, InterruptedException {
			int port = PORT;

			String ip = getIp();

			// launch first node
			node[0] = new PastryNodeImpl(port, null, STORAGE_MODE.memory, false, null, null);
			node[0].launch();
			port++;

			// launch other nodes
			for (int i = 1; i < NODE_COUNT; i++) {
				node[i] = new PastryNodeImpl(port, ip + ":" + PORT, STORAGE_MODE.memory, false, null, null);
				node[i].launch();
				port++;
			}

			Thread.sleep(1000);

			// register agents
			for (int nodeId = 0; nodeId < NODE_COUNT; nodeId++) {
				for (int agentId = 0; agentId < AGENTS_PER_NODE_COUNT; agentId++) {
					UserAgent a = UserAgent.createUserAgent("passphrase");
					a.unlockPrivateKey("passphrase");
					node[nodeId].storeAgent(a);
					node[nodeId].getOrRegisterLocalMediator(a);
					agents[nodeId * AGENTS_PER_NODE_COUNT + agentId] = a;
				}
			}

			Thread.sleep(1000);
		}

		@After
		public void shutDown() {
			for (int i = 0; i < NODE_COUNT; i++) {
				node[i].shutDown();
			}
		}

		@Test
		public void testTopics() throws AgentNotKnownException, AgentAlreadyRegisteredException, L2pSecurityException,
				AgentException, EncodingFailedException, SerializationException, InterruptedException, NodeException {

			// register
			node[0].registerReceiverToTopic(node[0].getOrRegisterLocalMediator(agents[1]), 123);
			node[0].registerReceiverToTopic(node[0].getOrRegisterLocalMediator(agents[2]), 123);
			node[1].registerReceiverToTopic(node[1].getOrRegisterLocalMediator(agents[5]), 123);
			node[1].registerReceiverToTopic(node[1].getOrRegisterLocalMediator(agents[6]), 123);
			node[2].registerReceiverToTopic(node[2].getOrRegisterLocalMediator(agents[10]), 123);
			node[2].registerReceiverToTopic(node[2].getOrRegisterLocalMediator(agents[11]), 123);

			// send
			Message msg = new Message(agents[0], 123, "topic");
			node[0].sendMessage(msg, null);
			Thread.sleep(5000);
			assertTrue(node[0].getOrRegisterLocalMediator(agents[1]).getNextMessage().getContent().equals("topic"));
			assertTrue(node[0].getOrRegisterLocalMediator(agents[2]).getNextMessage().getContent().equals("topic"));
			assertTrue(node[1].getOrRegisterLocalMediator(agents[5]).getNextMessage().getContent().equals("topic"));
			assertTrue(node[1].getOrRegisterLocalMediator(agents[6]).getNextMessage().getContent().equals("topic"));
			assertTrue(node[2].getOrRegisterLocalMediator(agents[10]).getNextMessage().getContent().equals("topic"));
			assertTrue(node[2].getOrRegisterLocalMediator(agents[11]).getNextMessage().getContent().equals("topic"));

			// unregister
			node[1].unregisterReceiverFromTopic(node[1].getOrRegisterLocalMediator(agents[6]), 123);
			node[2].unregisterReceiverFromTopic(node[2].getOrRegisterLocalMediator(agents[10]), 123);
			node[2].unregisterReceiverFromTopic(node[2].getOrRegisterLocalMediator(agents[11]), 123);

			// send
			Message msg2 = new Message(agents[0], 123, "topic");
			node[0].sendMessage(msg2, null);
			Thread.sleep(5000);
			assertTrue(node[0].getOrRegisterLocalMediator(agents[1]).getNextMessage().getContent().equals("topic"));
			assertTrue(node[0].getOrRegisterLocalMediator(agents[2]).getNextMessage().getContent().equals("topic"));
			assertTrue(node[1].getOrRegisterLocalMediator(agents[5]).getNextMessage().getContent().equals("topic"));
			assertFalse(node[1].getOrRegisterLocalMediator(agents[6]).hasMessages());
			assertFalse(node[2].getOrRegisterLocalMediator(agents[10]).hasMessages());
			assertFalse(node[2].getOrRegisterLocalMediator(agents[11]).hasMessages());

		}*/

	// TODO add more pastry node tests

}
