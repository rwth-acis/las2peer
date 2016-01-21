package i5.las2peer.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import i5.las2peer.api.Service;
import i5.las2peer.execution.NoSuchServiceException;

public class LocalServiceTestCaseTest2 extends LocalServiceTestCase {

	public static final String AGENT_XML_FILE = "i5/las2peer/testing/TestServiceAgent.xml";
	public static final String AGENT_PASSPHRASE = "agentpass";

	@Test
	public void test() throws NoSuchServiceException {
		assertEquals(TestService.class, getServiceClass());
		assertEquals(TestService.class.getName(), getMyAgent().getServiceClassName());

		assertTrue(getNode().hasAgent(getMyAgent().getId()));

		assertEquals(TestService.class, getServiceInstance().getClass());

		assertEquals(AGENT_PASSPHRASE, getAgentPassphrase());
	}

	@Override
	public Class<? extends Service> getServiceClass() {
		return TestService.class;
	}

}
