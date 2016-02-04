package i5.las2peer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.UnknownHostException;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;

import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.p2p.ServiceNameVersion;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.ServiceInfoAgent;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

public class ServiceInfoAgentTest {

	private static final int NODES_AMOUNT = 1;
	public static final int START_PORT = 8000;
	private static Node[] nodes = new Node[NODES_AMOUNT];
//	private static ServiceInfoAgent[] agents = new ServiceInfoAgent[NODES_AMOUNT];
//	private static UserAgent testAgent;
//	private static final String testPass = "adamspass";

	@BeforeClass
	public static void start() throws Exception {

	}

	@Test
	public void test() throws UnknownHostException, SerializationException, CryptoException {
		// String host = getHostString();
		nodes[0] = new PastryNodeImpl(START_PORT + 0, "");

		/*
		 * for(int i = 1; i < nodes.length; i++) {
		 * nodes[i]=new PastryNodeImpl(START_PORT+i,host+":"+Integer.toString(START_PORT+i-1));
		 * }
		 */

		try {
			ServiceInfoAgent agent = ServiceInfoAgent.getServiceInfoAgent();

			for (int i = 0; i < nodes.length; i++) {
				nodes[i].launch();

				nodes[i].registerReceiver(agent);

			}

//			PastryNodeImpl node = (PastryNodeImpl) nodes[0];

			String testClass1 = "i5.las2peer.api.TestService@1.0";
			ServiceAgent testService = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString(testClass1), "a pass");
			testService.unlockPrivateKey("a pass");
			nodes[0].registerReceiver(testService);

			String testClass2 = "i5.las2peer.api.TestService2@1.0";
			ServiceAgent testService2 = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString(testClass2), "a pass");
			testService2.unlockPrivateKey("a pass");
			nodes[0].registerReceiver(testService2);

			Thread.sleep(5000);
			ServiceNameVersion[] services = ServiceInfoAgent.getServices();

//			String servicesString = "";
			String[] serviceNames = new String[services.length];
			for (int i = 0; i < services.length; i++) {
				serviceNames[i] = services[i].getName();
			}

			Arrays.sort(serviceNames);

			assertEquals("i5.las2peer.api.TestServicei5.las2peer.api.TestService2", serviceNames[0] + serviceNames[1]);

		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}

	}

	/*
	 * private String getHostString() throws UnknownHostException { String[]
	 * hostAddress=String.valueOf(InetAddress.getLocalHost()).split("/");
	 * System.out.println(hostAddress[hostAddress.length - 1]); return
	 * hostAddress[hostAddress.length - 1]; }
	 */

}
