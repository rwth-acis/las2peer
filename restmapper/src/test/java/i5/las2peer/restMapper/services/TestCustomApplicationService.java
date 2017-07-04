package i5.las2peer.restMapper.services;

import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

@ServicePath("service2")
public class TestCustomApplicationService extends RESTService {
	@Override
	protected void initResources() {
		setApplication(new Application() {
			@Override
			public Set<Class<?>> getClasses() {
				Set<Class<?>> classes = new HashSet<>();
				classes.add(RootResource.class);
				return classes;
			}
		});
	}
}
