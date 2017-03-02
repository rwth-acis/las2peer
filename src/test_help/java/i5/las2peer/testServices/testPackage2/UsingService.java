package i5.las2peer.testServices.testPackage2;

import i5.las2peer.api.Context;
import i5.las2peer.api.Service;
import i5.las2peer.api.execution.ServiceInvocationException;

public class UsingService extends Service {

	public int useTestService(String version) throws ServiceInvocationException {
		String testService = "i5.las2peer.testServices.testPackage1.TestService";
		if (!version.equals("null")) {
			testService += "@" + version;
		}
		return (int) Context.get().invokeInternally(testService, "getVersion");
	}

}
