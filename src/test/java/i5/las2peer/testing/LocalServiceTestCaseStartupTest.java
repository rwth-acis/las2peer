package i5.las2peer.testing;

import java.io.IOException;

import org.junit.Test;

import i5.las2peer.api.Service;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.persistency.MalformedXMLException;

public class LocalServiceTestCaseStartupTest extends LocalServiceTestCase {

	@Test
	public void test() throws MalformedXMLException, IOException, AgentNotFoundException {
		getNode().getAgent(MockAgentFactory.getEve().getSafeId());
	}

	@Override
	public Class<? extends Service> getServiceClass() {
		return TestService.class;
	}

	@Override
	public String getStartupDir() {
		return "testing/local_xml_startup";
	}

	@Override
	public String getServiceVersion() {
		return "1.0";
	}

}
