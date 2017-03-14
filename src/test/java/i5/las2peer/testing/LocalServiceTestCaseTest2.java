package i5.las2peer.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

import i5.las2peer.api.Service;

public class LocalServiceTestCaseTest2 extends LocalServiceTestCase {

	public static final String AGENT_XML_FILE = "i5/las2peer/testing/TestService.agent.xml";
	public static final String AGENT_PASSPHRASE = "agentpass";

	@Test
	public void test() {
		try {
			assertEquals(TestService.class, getServiceClass());
			assertEquals(TestService.class.getName(), getMyAgent().getServiceNameVersion().getName());

			assertTrue(getNode().hasAgent(getMyAgent().getIdentifier()));

			assertEquals(TestService.class, getServiceInstance().getClass());

			assertEquals(AGENT_PASSPHRASE, getAgentPassphrase());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Override
	public Class<? extends Service> getServiceClass() {
		return TestService.class;
	}

	@Override
	public String getServiceVersion() {
		return "1.0";
	}

}
