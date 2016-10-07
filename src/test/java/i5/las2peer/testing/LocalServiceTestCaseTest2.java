package i5.las2peer.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import i5.las2peer.api.Service;
import i5.las2peer.api.execution.ServiceNotFoundException;

import org.junit.Test;

public class LocalServiceTestCaseTest2 extends LocalServiceTestCase {

	public static final String AGENT_XML_FILE = "i5/las2peer/testing/TestService.agent.xml";
	public static final String AGENT_PASSPHRASE = "agentpass";

	@Test
	public void test() throws ServiceNotFoundException {
		assertEquals(TestService.class, getServiceClass());
		assertEquals(TestService.class.getName(), getMyAgent().getServiceNameVersion().getName());

		assertTrue(getNode().hasAgent(getMyAgent().getSafeId()));

		assertEquals(TestService.class, getServiceInstance().getClass());

		assertEquals(AGENT_PASSPHRASE, getAgentPassphrase());
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
