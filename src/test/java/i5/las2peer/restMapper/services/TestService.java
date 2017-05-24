package i5.las2peer.restMapper.services;

import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;

@ServicePath("service1")
public class TestService extends RESTService {
	@Override
	protected void initResources() {
		getResourceConfig().register(RootResource.class);
	}
}
