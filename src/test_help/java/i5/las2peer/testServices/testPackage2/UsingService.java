package i5.las2peer.testServices.testPackage2;

import i5.las2peer.api.Service;
import i5.las2peer.execution.L2pServiceException;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.TimeoutException;
import i5.las2peer.security.L2pSecurityException;

public class UsingService extends Service {
	
	public int useTestService(String version) throws AgentNotKnownException, L2pServiceException, L2pSecurityException, InterruptedException, TimeoutException {
		String testService = "i5.las2peer.testServices.testPackage1.TestService";
		if (!version.equals("null")) {
			testService += "@" + version;
		}
		
		return (int)this.invokeInternally(testService, "getVersion");
	}
	
}
