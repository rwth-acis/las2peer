package i5.las2peer.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import i5.las2peer.api.Service;
import i5.las2peer.execution.NoSuchServiceException;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.Agent;

public class LocalServiceTestCaseTest extends LocalServiceTestCase {

	@Test
	public void test() throws NoSuchServiceException {
		assertEquals(TestService.class, getServiceClass());
		assertEquals(TestService.class.getName(), getMyAgent().getServiceNameVersion().getName());

		assertTrue(getNode().hasAgent(getMyAgent().getSafeId()));

		assertEquals(TestService.class, getServiceInstance().getClass());
	}

	@Test
	public void testMockAgents() throws MalformedXMLException, IOException {
		Agent eve = MockAgentFactory.getEve();
		getNode().hasAgent(eve.getSafeId());

		assertTrue(getNode().hasAgent(MockAgentFactory.getAdam().getSafeId()));
		assertTrue(getNode().hasAgent(MockAgentFactory.getAbel().getSafeId()));
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
