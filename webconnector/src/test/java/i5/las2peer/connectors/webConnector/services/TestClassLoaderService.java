package i5.las2peer.connectors.webConnector.services;

import i5.las2peer.connectors.webConnector.services.classLoaderTest.TestResource;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;

@ServicePath("classloader")
public class TestClassLoaderService extends RESTService {

	@Override
	protected void initResources() {
		getResourceConfig().register(TestResource.class);
		//getResourceConfig().packages(TestResource.class.getPackage().toString());
	}

}
