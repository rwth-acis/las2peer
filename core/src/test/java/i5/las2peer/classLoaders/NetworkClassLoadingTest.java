package i5.las2peer.classLoaders;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import i5.las2peer.api.TestService;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.persistency.SharedStorage;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.serialization.SerializeTools;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.testing.TestSuite;
import i5.las2peer.tools.CryptoTools;
import i5.las2peer.tools.PackageUploader;

public class NetworkClassLoadingTest {

	@Test
	public void testServiceStart() {
		ArrayList<PastryNodeImpl> nodes = null;
		try {
			nodes = TestSuite.launchNetwork(SharedStorage.DEFAULT_NUM_OF_REPLICAS + 1);
			// upload TestService on first node
			final Class<?> testServiceClass = TestService.class;
			final String serviceName = testServiceClass.getCanonicalName();
			final String serviceVersion = "1.0";
			final String clsFilename = serviceName + ".class";
			final byte[] clsContent = SerializeTools.serialize(testServiceClass);
			final byte[] clsHash = CryptoTools.getSecureHash(clsContent);
			HashMap<String, byte[]> filenameToHash = new HashMap<>();
			filenameToHash.put(clsFilename, clsHash);
			HashMap<String, byte[]> filenameToContent = new HashMap<>();
			filenameToContent.put(clsFilename, clsHash);
			UserAgentImpl developerAgent = MockAgentFactory.getAdam();
			developerAgent.unlock("adamspass");
			PackageUploader.uploadServicePackage(nodes.get(0), serviceName, serviceVersion, filenameToHash,
					filenameToContent, developerAgent);
			// start TestService on second node
			ServiceAgentImpl serviceAgent = ServiceAgentImpl
					.createServiceAgent(new ServiceNameVersion(serviceName, serviceVersion), "servicepass");
			serviceAgent.unlock("servicepass");
			nodes.get(1).registerReceiver(serviceAgent);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		} finally {
			for (Node node : nodes) {
				node.shutDown();
			}
		}
	}

}
